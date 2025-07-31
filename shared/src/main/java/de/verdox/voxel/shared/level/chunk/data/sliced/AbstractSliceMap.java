package de.verdox.voxel.shared.level.chunk.data.sliced;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public abstract class AbstractSliceMap implements SliceMap {

    public enum State {EMPTY, UNIFORM, DENSE}

    @Override
    public void write(Kryo kryo, Output output) {
        State st = (strategy instanceof SliceMapStrategy.Empty ? State.EMPTY : strategy instanceof SliceMapStrategy.Uniform ? State.UNIFORM : State.DENSE);
        kryo.writeObject(output, st);

        switch (st) {
            case EMPTY:
                break;
            case UNIFORM:
                byte v = ((SliceMapStrategy.Uniform) strategy).value;
                output.writeByte(v);
                break;
            case DENSE:
                output.writeByte(((SliceMapStrategy.Dense) strategy).initialUniform);
                output.writeInt(((SliceMapStrategy.Dense) strategy).uniformCount);
                byte[] data = ((SliceMapStrategy.Dense) strategy).data;
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
                strategy = SliceMapStrategy.Empty.INSTANCE;
                break;

            case UNIFORM:
                byte u = input.readByte();
                strategy = new SliceMapStrategy.Uniform(u);
                break;

            case DENSE:
                byte initialUniform = input.readByte();
                int uniformCount = input.readInt();
                int len = input.readInt(true);
                SliceMapStrategy.Dense ds = new SliceMapStrategy.Dense(this, initialUniform);
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

    protected SliceMapStrategy strategy;

    public AbstractSliceMap() {
        this.strategy = SliceMapStrategy.Empty.INSTANCE;
    }

    @Override
    public byte get(int x, int z) {
        return strategy.get(this, x, z);
    }

    @Override
    public void set(int x, int z, byte value) {
        SliceMapStrategy next = strategy.set(this, x, z, value);
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
}

