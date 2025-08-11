package de.verdox.voxel.shared.util.palette.strategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public interface PaletteStrategy<T> {
    T get(short x, short y, short z, ThreeDimensionalPalette<T> context);

    void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> context);

    void write(Kryo kryo, Output output);

    void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> context);

    class Empty<T> implements PaletteStrategy<T> {

        public Empty() {
        }

        @Override
        public T get(short x, short y, short z, ThreeDimensionalPalette<T> context) {
            return context.getDefaultValue();
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            if (!block.equals(ctx.getDefaultValue())) {
                ctx.setStrategy(new Paletted<>(ctx), ThreeDimensionalPalette.State.PALETTED);
                ctx.getStrategy().set(x, y, z, block, ctx);
            }
        }

        @Override
        public void write(Kryo kryo, Output output) {
        }

        @Override
        public void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> ctx) {
            ctx.setStrategy(new Empty<>(), ThreeDimensionalPalette.State.EMPTY);
        }
    }

    class Uniform<T> implements PaletteStrategy<T> {
        @Getter
        private final T uniformValue;

        public Uniform(T uniformValue) {
            this.uniformValue = uniformValue;
        }

        @Override
        public T get(short x, short y, short z, ThreeDimensionalPalette<T> ctx) {
            return uniformValue;
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            if (!block.equals(uniformValue)) {
                Paletted<T> paletted = new Paletted<>(ctx);
                ctx.setStrategy(paletted, ThreeDimensionalPalette.State.PALETTED);
                paletted.fill(uniformValue);
                paletted.set(x, y, z, block, ctx);
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
        @Getter
        private final ThreeDimensionalPalette<T> ctx;
        @Getter
        private int totalSize;
        @Getter
        private PaletteStorage storage;

        private Map<T, Integer> blockToId;
        @Getter
        private List<T> idToBlock;
        private int nonDefaultCount;

        public Paletted(ThreeDimensionalPalette<T> ctx) {
            this.ctx = ctx;
            initFromContext();
        }

        public int getPaletteSize() {
            return idToBlock.size();
        }

        /**
         * Initialisiert die Palette mit ctx.defaultValue (ID 0).
         */
        private void initFromContext() {
            blockToId = new Object2IntOpenHashMap<>();
            idToBlock = new ObjectArrayList<>();
            // Default-Wert immer ID 0
            blockToId.put(ctx.getDefaultValue(), 0);
            idToBlock.add(ctx.getDefaultValue());
            totalSize = ctx.getSizeX() * ctx.getSizeY() * ctx.getSizeZ();
            storage = PaletteStorage.create(this, 4);
            nonDefaultCount = 0;
        }

        @Override
        public T get(short x, short y, short z, ThreeDimensionalPalette<T> ctx) {
            int idx = ctx.computeIndex(x, y, z);
            int id = storage.read(idx);
            return idToBlock.get(id);
        }

        public void fill(T block) {
            blockToId.clear();
            idToBlock.clear();
            nonDefaultCount = totalSize;

            blockToId.put(ctx.getDefaultValue(), 0);
            idToBlock.add(ctx.getDefaultValue());

            int id = idToBlock.size();
            idToBlock.add(block);
            blockToId.put(block, id);
            storage.fill(id);
        }

        public void remap() {
            int oldSize = idToBlock.size();
            List<T> newIdToBlock = new ArrayList<>();
            int[] reMap = new int[oldSize];
            for (int oldId = 0; oldId < oldSize; oldId++) {
                T oldEntry = idToBlock.get(oldId);
                if (oldEntry == null) {
                    continue;
                }
                int newId = newIdToBlock.size();
                newIdToBlock.add(oldEntry);
                reMap[oldId] = newId;
            }

            for (int i = 0; i < totalSize; i++) {
                int oldId = storage.read(i);
                storage.write(i, reMap[oldId]);
            }

            blockToId.clear();
            this.idToBlock = newIdToBlock;
            for (int i = 0; i < this.idToBlock.size(); i++) {
                T value = this.idToBlock.get(i);
                blockToId.put(value, i);
            }
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            int idx = ctx.computeIndex(x, y, z);
            int oldId = storage.read(idx);
            final int defaultId = 0;

            Integer id = blockToId.get(block);
            if (id == null) {
                id = idToBlock.size();
                idToBlock.add(block);
                blockToId.put(block, id);
                storage = PaletteStorage.resizeIfNeeded(this, storage);
            }

            boolean wasDefault = (oldId == defaultId);
            boolean nowDefault = (id == defaultId);
            if (wasDefault && !nowDefault) nonDefaultCount++;
            else if (!wasDefault && nowDefault) nonDefaultCount--;

            storage.write(idx, id);

            if (nonDefaultCount == totalSize) {
                ctx.setStrategy(new Uniform<>(block), ThreeDimensionalPalette.State.UNIFORM);
            }
        }

        @Override
        public void write(Kryo kryo, Output output) {
            remap();
            output.writeInt(idToBlock.size(), true);
            for (T entry : idToBlock) {
                kryo.writeClassAndObject(output, entry);
            }
            storage.write(kryo, output);
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
            storage = PaletteStorage.create(this, 1);
            storage.read(kryo, input);

            nonDefaultCount = 0;
            for (int i = 0; i < totalSize; i++) {
                if (storage.read(i) != 0) {
                    nonDefaultCount++;
                }
            }
        }
    }
}
