package de.verdox.voxel.shared.level.chunk;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Arrays;

public abstract class AbstractSliceMap implements SliceMap {

    public enum State {EMPTY, UNIFORM, DENSE}

    @Override
    public void write(Kryo kryo, Output output) {
        State st = (strategy instanceof EmptyStrategy ? State.EMPTY : strategy instanceof UniformStrategy ? State.UNIFORM : State.DENSE);
        kryo.writeObject(output, st);

        switch (st) {
            case EMPTY:
                break;
            case UNIFORM:
                byte v = ((UniformStrategy) strategy).value;
                output.writeByte(v);
                break;
            case DENSE:
                output.writeByte(((DenseStrategy) strategy).initialUniform);
                output.writeInt(((DenseStrategy) strategy).uniformCount);
                byte[] data = ((DenseStrategy) strategy).data;
                output.writeInt(data.length, true);
                output.writeBytes(data);
                break;
        }
    }

    @Override
    public void readAndUpdate(Kryo kryo, Input input) {
        // 1) Lese State
        State st = kryo.readObject(input, State.class);

        switch (st) {
            case EMPTY:
                strategy = new EmptyStrategy();
                break;

            case UNIFORM:
                byte u = input.readByte();
                strategy = new UniformStrategy(u);
                break;

            case DENSE:
                byte initialUniform = input.readByte();
                int uniformCount = input.readInt();
                int len = input.readInt(true);
                DenseStrategy ds = new DenseStrategy(initialUniform);
                ds.uniformCount = uniformCount;
                if (len != ds.data.length) {
                    throw new IllegalStateException(
                            "DenseStrategy length mismatch: expected=" + ds.data.length +
                                    " got=" + len);
                }
                input.readBytes(ds.data);
                strategy = ds;
                break;
        }
    }

    protected final int sx, sz;
    protected Strategy strategy;

    public AbstractSliceMap(int sx, int sz) {
        this.sx = sx;
        this.sz = sz;
        this.strategy = new EmptyStrategy();
    }

    @Override
    public byte get(int x, int z) {
        return strategy.get(x, z);
    }

    @Override
    public void set(int x, int z, byte value) {
        Strategy next = strategy.set(x, z, value);
        if (next != null) {
            strategy = next;
        }
    }

    @Override
    public boolean isEmpty() {
        return strategy.isEmpty();
    }

    @Override
    public boolean isUniform() {
        return strategy.isUniform();
    }

    // --- Strategy-Interface und drei Implementierungen ---

    protected interface Strategy {
        byte get(int x, int z);

        /**
         * Setzt den Wert.
         *
         * @return null, wenn intern geblieben,
         * neue Strategy, wenn gewechselt werden muss.
         */
        Strategy set(int x, int z, byte value);

        boolean isEmpty();

        boolean isUniform();
    }

    /**
     * 1) noch leer: default == 0 überall
     */
    private class EmptyStrategy implements Strategy {
        @Override
        public byte get(int x, int z) {
            return 0;
        }

        @Override
        public Strategy set(int x, int z, byte v) {
            if (v == 0) return null;
            // erster non-zero → Uniform
            return new UniformStrategy(v);
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
    private class UniformStrategy implements Strategy {
        private final byte value;

        UniformStrategy(byte v) {
            this.value = v;
        }

        @Override
        public byte get(int x, int z) {
            return value;
        }

        @Override
        public Strategy set(int x, int z, byte v) {
            if (v == value) return null;
            // erster Unterschied → Dense
            DenseStrategy dense = new DenseStrategy(value);
            dense.fill(value);
            dense.set(x, z, v);
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
    private class DenseStrategy implements Strategy {
        private final byte initialUniform; // Wert, mit dem wir gestartet sind
        private int uniformCount;          // wie viele Einträge == initialUniform
        private final byte[] data = new byte[sx * sz];

        // Konstruktor: called von UniformStrategy.promote()
        DenseStrategy(byte uniformValue) {
            this.initialUniform = uniformValue;
            this.uniformCount = data.length;
            Arrays.fill(data, uniformValue);
        }

        @Override
        public byte get(int x, int z) {
            return data[x * sz + z];
        }

        @Override
        public Strategy set(int x, int z, byte v) {
            int idx = x * sz + z;
            byte old = data[idx];
            data[idx] = v;

            // Update uniformCount
            if (old == initialUniform) uniformCount--;
            if (v == initialUniform) uniformCount++;

            // 1) leer (empty)? initialUniform==0 und alle == 0
            if (initialUniform == 0 && uniformCount == data.length) {
                return new EmptyStrategy();
            }
            // 2) uniform (aber nicht 0)? alle == initialUniform
            if (uniformCount == data.length) {
                return new UniformStrategy(initialUniform);
            }
            // 3) sonst bleibt Dense
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

