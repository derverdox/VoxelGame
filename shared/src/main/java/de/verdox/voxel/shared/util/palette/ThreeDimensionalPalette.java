package de.verdox.voxel.shared.util.palette;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.level.chunk.Box;
import de.verdox.voxel.shared.network.packet.serializer.NetworkSerializable;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStrategy;
import lombok.Getter;

import java.util.*;

@Getter
public abstract class ThreeDimensionalPalette<T> implements NetworkSerializable, Box {
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
            case PALETTED -> new PaletteStrategy.Paletted<>(this);
        };
        strategy.read(kryo, input, this);
    }

    public enum State {EMPTY, UNIFORM, PALETTED}

    @Getter
    private State state = State.EMPTY;

    @Getter
    private PaletteStrategy<T> strategy;

    private final T defaultValue;

    /**
     * Create a palette region of given dimensions, all initialized to defaultValue.
     */
    public ThreeDimensionalPalette(T defaultValue) {
        this.defaultValue = defaultValue;
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

    public List<T> getPalette() {
        if (this.strategy instanceof PaletteStrategy.Empty<T>) {
            return List.of(defaultValue);
        } else if (this.strategy instanceof PaletteStrategy.Uniform<T> uniform) {
            return List.of(defaultValue, uniform.getUniformValue());
        } else {
            PaletteStrategy.Paletted<T> paletted = (PaletteStrategy.Paletted<T>) this.strategy;
            return Collections.unmodifiableList(paletted.getIdToBlock());
        }
    }

    public int getPaletteSize() {
        if (this.strategy instanceof PaletteStrategy.Empty<T>) {
            return 1; // ONLY AIR
        } else if (this.strategy instanceof PaletteStrategy.Uniform<T>) {
            return 2; // AIR + UNIFORM
        } else {
            PaletteStrategy.Paletted<T> paletted = (PaletteStrategy.Paletted<T>) this.strategy;
            return paletted.getCtx().getPalette().size();
        }
    }

    public int computeIndex(short x, short y, short z) {
        return x + getSizeX() * (y + getSizeY() * z);
    }

    public long contentHash() {
        return this.strategy.contentHash();
    }
}
