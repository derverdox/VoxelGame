package de.verdox.voxel.shared.util.palette.strategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;

import java.util.*;

public interface PaletteStrategy<T> {
    T get(short x, short y, short z);

    void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> context);

    void write(Kryo kryo, Output output);

    void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> context);

    class Empty<T> implements PaletteStrategy<T> {
        private final ThreeDimensionalPalette<T> parent;

        public Empty(ThreeDimensionalPalette<T> parent) {
            this.parent = parent;
        }

        @Override
        public T get(short x, short y, short z) {
            return parent.getDefaultValue();
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            if (!block.equals(parent.getDefaultValue())) {
                // erster Nicht-Default → direkt in die Paletted-Strategie
                ctx.setStrategy(new Paletted<>(ctx), ThreeDimensionalPalette.State.PALETTED);
                ctx.getStrategy().set(x, y, z, block, ctx);
            }
        }

        @Override
        public void write(Kryo kryo, Output output) {
        }

        @Override
        public void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> ctx) {
            ctx.setStrategy(new Empty<>(ctx), ThreeDimensionalPalette.State.EMPTY);
        }
    }

    class Uniform<T> implements PaletteStrategy<T> {
        private final T uniformValue;

        public Uniform(T uniformValue) {
            this.uniformValue = uniformValue;
        }

        @Override
        public T get(short x, short y, short z) {
            return uniformValue;
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            if (!block.equals(uniformValue)) {
                ctx.setStrategy(new Paletted<>(ctx), ThreeDimensionalPalette.State.PALETTED);
                ctx.getStrategy().set(x, y, z, block, ctx);
            }
        }

        @Override
        public void write(Kryo kryo, Output output) {
            kryo.writeClassAndObject(output, uniformValue);
        }

        @Override
        public void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> ctx) {
            T v = (T) kryo.readClassAndObject(input);
            ctx.setStrategy(new Uniform<>(v), ThreeDimensionalPalette.State.UNIFORM);
        }
    }

    class Paletted<T> implements PaletteStrategy<T> {
        private final ThreeDimensionalPalette<T> ctx;
        private final int totalSize;

        private Map<T, Integer> blockToId;
        private List<T> idToBlock;
        private int bitsPerBlock;
        private long[] data;
        private int nonDefaultCount;

        public Paletted(ThreeDimensionalPalette<T> ctx) {
            this.ctx = ctx;
            this.totalSize = ctx.getTotalSize();
            initFromContext();
        }

        /**
         * Initialisiert die Palette mit ctx.defaultValue (ID 0).
         */
        private void initFromContext() {
            blockToId = new HashMap<>();
            idToBlock = new ArrayList<>();
            // Default-Wert immer ID 0
            blockToId.put(ctx.getDefaultValue(), 0);
            idToBlock.add(ctx.getDefaultValue());
            bitsPerBlock = 4;
            data = new long[((totalSize * bitsPerBlock) + 63) >>> 6];
            // alles default
            Arrays.fill(data, 0L);
            nonDefaultCount = 0;
        }

        @Override
        public T get(short x, short y, short z) {
            int idx = ctx.computeIndex(x, y, z);
            int id = readPaletteIndex(idx);
            return idToBlock.get(id);
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            int idx = ctx.computeIndex(x, y, z);
            int oldId = readPaletteIndex(idx);
            final int defaultId = 0;

            // neue ID holen/erzeugen
            Integer id = blockToId.get(block);
            if (id == null) {
                id = idToBlock.size();
                idToBlock.add(block);
                blockToId.put(block, id);
                resizeIfNeeded();
            }

            boolean wasDefault = (oldId == defaultId);
            boolean nowDefault = (id == defaultId);
            if (wasDefault && !nowDefault) nonDefaultCount++;
            else if (!wasDefault && nowDefault) nonDefaultCount--;

            writePaletteIndex(idx, id);

            // Falls jetzt wirklich alle Zellen non-default sind, zurück in UNIFORM
            if (nonDefaultCount == totalSize) {
                ctx.setStrategy(new Uniform<>(block), ThreeDimensionalPalette.State.UNIFORM);
            }
        }

        @Override
        public void write(Kryo kryo, Output output) {
            output.writeInt(idToBlock.size(), true);
            for (T entry : idToBlock) {
                kryo.writeClassAndObject(output, entry);
            }
            output.writeInt(bitsPerBlock, true);
            output.writeInt(data.length, true);
            for (long word : data) {
                output.writeLong(word);
            }
        }

        @Override
        public void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> ctx) {
            blockToId.clear();
            idToBlock.clear();

            int paletteSize = input.readInt(true);
            for (int i = 0; i < paletteSize; i++) {
                @SuppressWarnings("unchecked")
                T block = (T) kryo.readClassAndObject(input);
                if (blockToId.containsKey(block)) {
                    return;
                }
                blockToId.put(block, i);
                idToBlock.add(block);
            }
            bitsPerBlock = input.readInt(true);
            int len = input.readInt(true);
            data = new long[len];
            for (int i = 0; i < len; i++) {
                data[i] = input.readLong();
            }

            // nonDefaultCount neu berechnen
            nonDefaultCount = 0;
            for (int i = 0; i < totalSize; i++) {
                if (readPaletteIndex(i) != 0) {
                    nonDefaultCount++;
                }
            }
        }

        // --- Hilfsmethoden für bit-gepackten Zugriff ---

        private void resizeIfNeeded() {
            int requiredBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(idToBlock.size() - 1));
            if (requiredBits != bitsPerBlock) {
                long[] newData = new long[((totalSize * requiredBits) + 63) >>> 6];
                // alte Werte neu packen
                for (int i = 0; i < totalSize; i++) {
                    int oldId = readPaletteIndex(i);
                    writeBits(newData, i, oldId, requiredBits);
                }
                bitsPerBlock = requiredBits;
                data = newData;
            }
        }

        private int readPaletteIndex(int cellIndex) {
            int bitPos = cellIndex * bitsPerBlock;
            int longPos = bitPos >>> 6;
            int offset = bitPos & 63;
            long segment = data[longPos] >>> offset;
            if (offset + bitsPerBlock > 64) {
                int overflow = offset + bitsPerBlock - 64;
                segment |= data[longPos + 1] << (64 - offset);
            }
            return (int) (segment & ((1L << bitsPerBlock) - 1));
        }

        private void writePaletteIndex(int cellIndex, int id) {
            writeBits(data, cellIndex, id, bitsPerBlock);
        }

        private void writeBits(long[] target, int cellIndex, int value, int width) {
            int bitPos = cellIndex * width;
            int longPos = bitPos >>> 6;
            int offset = bitPos & 63;
            long mask = ((1L << width) - 1L) << offset;
            target[longPos] = (target[longPos] & ~mask) |
                    (((long) value << offset) & mask);
            int overflow = offset + width - 64;
            if (overflow > 0) {
                long mask2 = (1L << overflow) - 1L;
                target[longPos + 1] = (target[longPos + 1] & ~mask2) |
                        ((long) value >>> (width - overflow) & mask2);
            }
        }
    }
}
