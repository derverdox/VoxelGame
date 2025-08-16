package de.verdox.voxel.shared.util.palette;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.level.chunk.Box;
import de.verdox.voxel.shared.network.packet.serializer.NetworkSerializable;
import de.verdox.voxel.shared.util.palette.strategy.PaletteIDHolder;
import de.verdox.voxel.shared.util.palette.strategy.PaletteIDMapper;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStrategy;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;

@Getter
public abstract class ThreeDimensionalPalette<T extends PaletteIDHolder> implements NetworkSerializable, Box {
    @Override
    public void write(Kryo kryo, Output output) {
        kryo.writeObject(output, state);
        strategy.write(kryo, output);
    }

    @Override
    public void readAndUpdate(Kryo kryo, Input input) {
        this.state = kryo.readObject(input, State.class);
        this.strategy = switch (this.state) {
            case EMPTY -> new PaletteStrategy.Empty<>();
            case UNIFORM -> new PaletteStrategy.Uniform<>(null);
            case PALETTED -> PaletteStrategy.Paletted.create(this);
        };
        strategy.read(kryo, input, this);
    }

    public enum State {EMPTY, UNIFORM, PALETTED}

    @Getter
    private State state = State.EMPTY;

    @Getter
    private PaletteStrategy<T> strategy;

    private final T defaultValue;
    @Getter
    private final PaletteIDMapper<T> paletteIDMapper;

    /**
     * Create a palette region of given dimensions, all initialized to defaultValue.
     */
    public ThreeDimensionalPalette(T defaultValue, PaletteIDMapper<T> paletteIDMapper) {
        this.defaultValue = defaultValue;
        this.paletteIDMapper = paletteIDMapper;
        this.strategy = new PaletteStrategy.Empty<>();
    }

    public T get(short x, short y, short z) {
        return strategy.get(x, y, z, this);
    }

    public void set(short x, short y, short z, T block) {
        strategy.set(x, y, z, block, this);
    }

    public void setStrategy(PaletteStrategy<T> strategy, State state) {
        this.strategy = strategy;
        this.state = state;
    }

    public int getPaletteSize() {
        if (this.strategy instanceof PaletteStrategy.Empty<T>) {
            return 1; // ONLY AIR
        } else if (this.strategy instanceof PaletteStrategy.Uniform<T>) {
            return 2; // AIR + UNIFORM
        } else {
            PaletteStrategy.Paletted<T> Paletted = (PaletteStrategy.Paletted<T>) this.strategy;
            return Paletted.getPaletteSize();
        }
    }

    public int computeIndex(short x, short y, short z) {
        return x + getSizeX() * (y + getSizeY() * z);
    }

    public long contentHash() {
        return this.strategy.contentHash();
    }
}
