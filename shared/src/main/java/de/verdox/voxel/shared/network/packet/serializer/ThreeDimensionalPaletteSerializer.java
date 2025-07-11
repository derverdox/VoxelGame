package de.verdox.voxel.shared.network.packet.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;

import java.util.List;

public abstract class ThreeDimensionalPaletteSerializer<T>
    extends Serializer<ThreeDimensionalPalette<T>> {

    /**
     * Schreibe einen einzelnen Block-Wert vom Typ T.
     */
    protected abstract void writeData(Kryo kryo, Output output, T block);

    /**
     * Lese einen einzelnen Block-Wert vom Typ T.
     */
    protected abstract T readData(Kryo kryo, Input input);

    @Override
    public void write(Kryo kryo, Output output, ThreeDimensionalPalette<T> palette) {
        // 1) Dimensionen
        output.writeShort(palette.getDimensionX());
        output.writeShort(palette.getDimensionY());
        output.writeShort(palette.getDimensionZ());
        // 2) Default-Wert
        writeData(kryo, output, palette.getDefaultValue());
        // 3) Palette (ID → Block)
        List<T> entries = palette.getPalette();
        output.writeInt(entries.size(), true);
        for (T block : entries) {
            writeData(kryo, output, block);
        }
        // 4) bitsPerBlock
        output.writeInt(palette.getBitsPerBlock(), true);
        // 5) rohes Daten-Array
        long[] data = palette.getData();
        output.writeInt(data.length, true);
        for (long word : data) {
            output.writeLong(word);
        }
    }

    @Override
    public ThreeDimensionalPalette<T> read(Kryo kryo, Input input,
                                           Class<? extends ThreeDimensionalPalette<T>> type) {
        // 1) Dimensionen
        short dimX = input.readShort();
        short dimY = input.readShort();
        short dimZ = input.readShort();
        // 2) Default-Wert
        T defaultValue = readData(kryo, input);
        // Erstelle Instanz
        ThreeDimensionalPalette<T> palette = new ThreeDimensionalPalette<>(defaultValue, dimX, dimY, dimZ);

        // 3) Palette-Einträge
        int paletteSize = input.readInt(true);
        // Index 0 ist defaultValue, den überspringen wir
        for (int i = 0; i < paletteSize; i++) {
            T block = readData(kryo, input);
            // hookup in die interne Palette (reflectiv oder über setter)
            palette.setForSerialization(block, i);
        }

        // 4) bitsPerBlock
        int bitsPerBlock = input.readInt(true);
        // 5) Daten-Words
        int dataLen = input.readInt(true);
        long[] data = new long[dataLen];
        for (int i = 0; i < dataLen; i++) {
            data[i] = input.readLong();
        }
        // hookup intern
        palette.setForDeserialization(bitsPerBlock, data);

        return palette;
    }
}
