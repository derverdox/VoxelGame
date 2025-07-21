package de.verdox.voxel.shared.util.palette;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.network.packet.serializer.NetworkSerializable;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStrategy;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class ThreeDimensionalPalette<T> implements NetworkSerializable {
    @Override
    public void write(Kryo kryo, Output output) {
        kryo.writeObject(output, state);
        strategy.write(kryo, output);
    }

    @Override
    public void readAndUpdate(Kryo kryo, Input input) {
        this.state = kryo.readObject(input, State.class);
        this.strategy = switch (this.state) {
            case EMPTY -> new PaletteStrategy.Empty<>(this);
            case UNIFORM -> new PaletteStrategy.Uniform<>(null);
            case PALETTED -> new PaletteStrategy.Paletted<>(this);
        };
        strategy.read(kryo, input, this);
    }

    public enum State {EMPTY, UNIFORM, PALETTED}

    private State state = State.EMPTY;

    @Getter
    private PaletteStrategy<T> strategy;
    @Getter
    private final short dimensionX, dimensionY, dimensionZ;
    @Getter
    private final int totalSize;
    private final T defaultValue;

    private final Map<T, Integer> blockToId = new HashMap<>();
    private final List<T> idToBlock = new ArrayList<>();

    /**
     * Create a palette region of given dimensions, all initialized to defaultValue.
     */
    public ThreeDimensionalPalette(T defaultValue, short dx, short dy, short dz) {
        this.defaultValue = defaultValue;
        this.strategy = new PaletteStrategy.Empty<>(this);
        this.dimensionX = dx;
        this.dimensionY = dy;
        this.dimensionZ = dz;
        this.totalSize = dx * dy * dz;
    }

    public T get(short x, short y, short z) {
        return strategy.get(x, y, z);
    }

    public void set(short x, short y, short z, T block) {
        strategy.set(x, y, z, block, this);
    }

    public void setStrategy(PaletteStrategy<T> strategy, State state) {
        this.strategy = strategy;
        this.state = state;
    }

    /**
     * Returns an unmodifiable view of the palette (ID → block).
     */
    public List<T> getPalette() {
        return Collections.unmodifiableList(idToBlock);
    }

    // --- internal methods ---

    public void checkBounds(short x, short y, short z) {
        if (x < 0 || x >= dimensionX || y < 0 || y >= dimensionY || z < 0 || z >= dimensionZ) {
            throw new IndexOutOfBoundsException(
                    "Coordinates out of bounds: (" + x + "," + y + "," + z + ")");
        }
    }

    public int computeIndex(short x, short y, short z) {
        return x + dimensionX * (y + dimensionY * z);
    }

    @Override
    public String toString() {
        return "ThreeDimensionalPalette{" +
                "state=" + state +
                ", strategy=" + strategy +
                ", dimensionX=" + dimensionX +
                ", dimensionY=" + dimensionY +
                ", dimensionZ=" + dimensionZ +
                ", totalSize=" + totalSize +
                ", defaultValue=" + defaultValue +
                ", blockToId=" + blockToId +
                ", idToBlock=" + idToBlock +
                '}';
    }
}
