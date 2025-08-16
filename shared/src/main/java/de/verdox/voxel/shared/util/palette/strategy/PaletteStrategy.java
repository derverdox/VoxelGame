package de.verdox.voxel.shared.util.palette.strategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.data.types.Registries;
import de.verdox.voxel.shared.util.lod.GridDim;
import de.verdox.voxel.shared.util.lod.LodPyramid;
import de.verdox.voxel.shared.util.lod.OccupancyFn;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.*;

public interface PaletteStrategy<T extends PaletteIDHolder> {
    T get(short x, short y, short z, ThreeDimensionalPalette<T> context);

    void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> context);

    void write(Kryo kryo, Output output);

    void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> context);

    long contentHash();

    class Empty<T extends PaletteIDHolder> implements PaletteStrategy<T> {

        public Empty() {
        }

        @Override
        public T get(short x, short y, short z, ThreeDimensionalPalette<T> context) {
            return context.getDefaultValue();
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            if (!block.equals(ctx.getDefaultValue())) {
                ctx.setStrategy(Paletted.create(ctx), ThreeDimensionalPalette.State.PALETTED);
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

        @Override
        public long contentHash() {
            return 0;
        }
    }

    class Uniform<T extends PaletteIDHolder> implements PaletteStrategy<T> {
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
                Paletted<T> Paletted = PaletteStrategy.Paletted.create(ctx);
                ctx.setStrategy(Paletted, ThreeDimensionalPalette.State.PALETTED);
                Paletted.fill(uniformValue);
                Paletted.set(x, y, z, block, ctx);
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

        @Override
        public long contentHash() {
            return 0;
        }
    }

    class PalettedLocalID<T extends PaletteIDHolder> extends Paletted<T> {
        private Map<T, Integer> blockToId;
        @Getter
        private List<T> idToBlock;

        public PalettedLocalID(ThreeDimensionalPalette<T> ctx) {
            super(ctx);
        }

        @Override
        protected void initFromContext() {
            super.initFromContext();
            blockToId = new Object2IntOpenHashMap<>();
            idToBlock = new ObjectArrayList<>();
            blockToId.put(ctx.getDefaultValue(), 0);
            idToBlock.add(ctx.getDefaultValue());
        }

        @Override
        public T get(short x, short y, short z, ThreeDimensionalPalette<T> ctx) {
            int idx = ctx.computeIndex(x, y, z);
            int id = storage.read(idx);
            return idToBlock(id);
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

            if (oldId != id) {
                storage.write(idx, id);
            }

            if (nonDefaultCount == getTotalSize()) {
                ctx.setStrategy(new Uniform<>(block), ThreeDimensionalPalette.State.UNIFORM);
            }
        }

        public void fill(T block) {
            blockToId.clear();
            idToBlock.clear();
            nonDefaultCount = getTotalSize();

            blockToId.put(ctx.getDefaultValue(), 0);
            idToBlock.add(ctx.getDefaultValue());

            int id = idToBlock.size();
            idToBlock.add(block);
            blockToId.put(block, id);
            storage.fill(id);
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
            for (int i = 0; i < getTotalSize(); i++) {
                if (storage.read(i) != 0) {
                    nonDefaultCount++;
                }
            }
        }

        @Override
        public int getPaletteSize() {
            return idToBlock.size();
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

            for (int i = 0; i < getTotalSize(); i++) {
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
    }

    class PalettedGlobalID<T extends PaletteIDHolder> extends Paletted<T> {
        public PalettedGlobalID(ThreeDimensionalPalette<T> ctx) {
            super(ctx);
        }

        @Override
        public int getPaletteSize() {
            return Registries.BLOCKS.getAmountValues();
        }
    }

    abstract class Paletted<T extends PaletteIDHolder> implements PaletteStrategy<T> {

        public static <T extends PaletteIDHolder> Paletted<T> create(ThreeDimensionalPalette<T> ctx) {
            return new PalettedGlobalID<>(ctx);
        }

        @Getter
        protected final ThreeDimensionalPalette<T> ctx;
        @Getter
        protected PaletteStorage storage;
        protected int nonDefaultCount;

        public Paletted(ThreeDimensionalPalette<T> ctx) {
            this.ctx = ctx;
            initFromContext();
        }

        public abstract int getPaletteSize();

        /**
         * Initialisiert die Palette mit ctx.defaultValue (ID 0).
         */
        protected void initFromContext() {
            storage = PaletteStorage.create(this, Math.min(4, getPaletteSize()));
            nonDefaultCount = 0;
        }

        public int getTotalSize() {
            return ctx.getSizeX() * ctx.getSizeY() * ctx.getSizeZ();
        }

        protected T idToBlock(int id) {
            return ctx.getPaletteIDMapper().byID(id);
        }

        @Override
        public T get(short x, short y, short z, ThreeDimensionalPalette<T> ctx) {
            int idx = ctx.computeIndex(x, y, z);
            int id = storage.read(idx);
            return idToBlock(id);
        }

        public void fill(T block) {
            storage.fill(block.getPaletteID());
        }

        @Override
        public void set(short x, short y, short z, T block, ThreeDimensionalPalette<T> ctx) {
            int idx = ctx.computeIndex(x, y, z);
            int oldId = storage.read(idx);
            final int defaultId = 0;

            int id = block.getPaletteID();

            boolean wasDefault = (oldId == defaultId);
            boolean nowDefault = (id == defaultId);
            if (wasDefault && !nowDefault) nonDefaultCount++;
            else if (!wasDefault && nowDefault) nonDefaultCount--;

            if (oldId != id) {
                storage.write(idx, id);
            }

            if (nonDefaultCount == getTotalSize()) {
                ctx.setStrategy(new Uniform<>(block), ThreeDimensionalPalette.State.UNIFORM);
            }
        }

        @Override
        public void write(Kryo kryo, Output output) {
            storage.write(kryo, output);
        }

        @Override
        public void read(Kryo kryo, Input input, ThreeDimensionalPalette<T> ctx) {
            storage = PaletteStorage.create(this, 1);
            storage.read(kryo, input);

            nonDefaultCount = 0;
            for (int i = 0; i < getTotalSize(); i++) {
                if (storage.read(i) != 0) {
                    nonDefaultCount++;
                }
            }
        }

        @Override
        public long contentHash() {
            return 0;
        }
    }
}
