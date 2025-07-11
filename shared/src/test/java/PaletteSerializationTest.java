import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import de.verdox.voxel.shared.network.packet.serializer.ThreeDimensionalPaletteSerializer;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PaletteSerializationTest {
    private Kryo kryo;

    @BeforeEach
    void setUp() {
        kryo = new Kryo();
        // register the palette class with a String serializer
        kryo.register(ThreeDimensionalPalette.class,
            new ThreeDimensionalPaletteSerializer<String>() {
                @Override
                protected void writeData(Kryo k, Output out, String value) {
                    out.writeString(value);
                }

                @Override
                protected String readData(Kryo k, Input in) {
                    return in.readString();
                }
            }
        );
    }

    @Test
    void testSerializeDeserializeEmptyPalette() {
        ThreeDimensionalPalette<String> original =
            new ThreeDimensionalPalette<>("air", (short) 4, (short) 4, (short) 4);

        ThreeDimensionalPalette<String> copy = serializeDeserialize(original);

        // verify dimensions and defaultValue
        assertEquals(original.getDimensionX(), copy.getDimensionX());
        assertEquals(original.getDimensionY(), copy.getDimensionY());
        assertEquals(original.getDimensionZ(), copy.getDimensionZ());
        assertEquals("air", copy.getDefaultValue());

        // palette list should contain only "air"
        List<String> paletteList = copy.getPalette();
        assertEquals(1, paletteList.size());
        assertEquals("air", paletteList.get(0));

        // bitsPerBlock should be 4 and data length matches
        assertEquals(original.getBitsPerBlock(), copy.getBitsPerBlock());
        assertArrayEquals(original.getData(), copy.getData());
    }

    @Test
    void testSerializeDeserializeWithEntries() {
        ThreeDimensionalPalette<String> original =
            new ThreeDimensionalPalette<>("air", (short) 4, (short) 4, (short) 4);
        // set some blocks
        original.set((short) 1, (short) 1, (short) 1, "stone");
        original.set((short) 2, (short) 3, (short) 0, "dirt");

        ThreeDimensionalPalette<String> copy = serializeDeserialize(original);

        // dimensions and default
        assertEquals((short) 4, copy.getDimensionX());
        assertEquals((short) 4, copy.getDimensionY());
        assertEquals((short) 4, copy.getDimensionZ());
        assertEquals("air", copy.getDefaultValue());

        // palette contains air, stone, dirt in some order consistent with IDs
        List<String> pal = copy.getPalette();
        assertTrue(pal.contains("air"));
        assertTrue(pal.contains("stone"));
        assertTrue(pal.contains("dirt"));
        // check that get returns correct values
        assertEquals("stone", copy.get((short) 1, (short) 1, (short) 1));
        assertEquals("dirt", copy.get((short) 2, (short) 3, (short) 0));
        assertEquals("air", copy.get((short) 0, (short) 0, (short) 0));

        // bitsPerBlock should match
        assertEquals(original.getBitsPerBlock(), copy.getBitsPerBlock());
        assertArrayEquals(original.getData(), copy.getData());
    }

    @SuppressWarnings("unchecked")
    private ThreeDimensionalPalette<String> serializeDeserialize(
        ThreeDimensionalPalette<String> palette) {

        // 1) Schreibe in einen reinen Kryo-Byte-Puffer
        Output output = new Output(
            /* initialSize */ 4096,
            /* maxSize     */ -1      // unbegrenzt wachsend
        );
        kryo.writeObject(output, palette);
        // hole Dir hier unbedingt das gesamte Array
        byte[] bytes = output.toBytes();
        output.close();

        // Debugging: stell sicher, dass da wirklich was drin ist
        System.out.println(">>> Serialized " + bytes.length + " bytes");

        // 2) Lies direkt vom Byte-Array
        Input input = new Input(bytes);
        ThreeDimensionalPalette<String> copy =
            kryo.readObject(input, ThreeDimensionalPalette.class);
        input.close();
        return copy;
    }
}
