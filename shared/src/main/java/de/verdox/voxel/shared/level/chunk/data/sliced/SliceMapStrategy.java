package de.verdox.voxel.shared.level.chunk.data.sliced;

import java.util.Arrays;

public interface SliceMapStrategy {
    SliceMapStrategy set(SliceMap sliceMap, int x, int z, byte value);

    byte get(SliceMap sliceMap, int x, int z);

    boolean isEmpty();

    boolean isUniform();

    /**
     * 1) noch leer: default == 0 überall
     */
    class Empty implements SliceMapStrategy {
        public static final SliceMapStrategy INSTANCE = new Empty();

        private Empty() {
        }

        @Override
        public byte get(SliceMap sliceMap, int x, int z) {
            return 0;
        }

        @Override
        public SliceMapStrategy set(SliceMap sliceMap, int x, int z, byte v) {
            if (v == 0) return null;
            // erster non-zero → Uniform
            return new Uniform(v);
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean isUniform() {
            return true;
        }
    }

    /**
     * 2) alle Einträge gleich non-zero
     */
    class Uniform implements SliceMapStrategy {
        final byte value;

        Uniform(byte v) {
            this.value = v;
        }

        @Override
        public byte get(SliceMap sliceMap, int x, int z) {
            return value;
        }

        @Override
        public SliceMapStrategy set(SliceMap sliceMap, int x, int z, byte v) {
            if (v == value) return null;
            // erster Unterschied → Dense
            Dense dense = new Dense(sliceMap, value);
            dense.fill(value);
            dense.set(sliceMap, x, z, v);
            return dense;
        }

        @Override
        public boolean isEmpty() {
            return value == 0;
        }

        @Override
        public boolean isUniform() {
            return true;
        }
    }

    /**
     * 3) echtes 2D-Array
     */
    class Dense implements SliceMapStrategy {
        final SliceMap owner;
        final byte initialUniform;
        int uniformCount;
        final byte[] data;

        // Konstruktor: called von UniformStrategy.promote()
        Dense(SliceMap owner, byte uniformValue) {
            this.owner = owner;
            this.initialUniform = uniformValue;
            this.data = new byte[owner.getSizeX() * owner.getSizeZ()];
            this.uniformCount = data.length;
            Arrays.fill(data, uniformValue);
        }

        @Override
        public byte get(SliceMap sliceMap, int x, int z) {
            return data[x * owner.getSizeZ() + z];
        }

        @Override
        public SliceMapStrategy set(SliceMap sliceMap, int x, int z, byte v) {
            int idx = x * owner.getSizeZ() + z;
            byte old = data[idx];
            data[idx] = v;

            // Update uniformCount
            if (old == initialUniform) uniformCount--;
            if (v == initialUniform) uniformCount++;


            if (initialUniform == 0 && uniformCount == data.length) {
                return SliceMapStrategy.Empty.INSTANCE;
            }

            if (uniformCount == data.length) {
                return new Uniform(initialUniform);
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            // alle Einträge == 0
            return (initialUniform == 0 && uniformCount == data.length);
        }

        @Override
        public boolean isUniform() {
            // alle Einträge == initialUniform
            return (uniformCount == data.length);
        }

        // fill wird jetzt nur noch intern von der Promotion gebraucht
        void fill(byte v) {
            Arrays.fill(data, v);
            // initialUniform und uniformCount waren schon entsprechend gesetzt
        }
    }
}
