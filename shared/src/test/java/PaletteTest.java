import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaletteTest {
    private ThreeDimensionalPalette<String> palette;

    @BeforeEach
    void setUp() {
        // default block type is "air", dimensions 16x16x16 for tests
        palette = new ThreeDimensionalPalette<>("air", (short) 16, (short) 16, (short) 16);
    }

    @Test
    void testDefaultValues() {
        // All positions should return default value
        for (short x = 0; x < 16; x++) {
            for (short y = 0; y < 16; y++) {
                for (short z = 0; z < 16; z++) {
                    assertEquals("air", palette.get(x, y, z),
                        String.format("Expected default at (%d,%d,%d)", x, y, z));
                }
            }
        }
    }

    @Test
    void testSetAndGetSingle() {
        palette.set((short) 5, (short) 10, (short) 15, "stone");
        assertEquals("stone", palette.get((short) 5, (short) 10, (short) 15));
        // Others remain default
        assertEquals("air", palette.get((short) 0, (short) 0, (short) 0));
    }

    @Test
    void testOverwriteBlock() {
        palette.set((short) 1, (short) 1, (short) 1, "dirt");
        assertEquals("dirt", palette.get((short) 1, (short) 1, (short) 1));
        palette.set((short) 1, (short) 1, (short) 1, "grass");
        assertEquals("grass", palette.get((short) 1, (short) 1, (short) 1));
    }

    @Test
    void testPaletteListContents() {
        // Initially only "air"
        assertEquals(1, palette.getPalette().size());
        assertTrue(palette.getPalette().contains("air"));

        // Add some blocks
        palette.set((short) 3, (short) 3, (short) 3, "stone");
        palette.set((short) 4, (short) 5, (short) 6, "dirt");

        // Palette size should now be 3
        assertEquals(3, palette.getPalette().size());
        assertEquals("air", palette.getPalette().getFirst());
        assertTrue(palette.getPalette().contains("stone"));
        assertTrue(palette.getPalette().contains("dirt"));
    }

    @Test
    void testResizeBitsPerBlock() {
        // 4 bits => up to 16 entries. Add 17 unique to force resize
        for (int i = 1; i <= 16; i++) {
            palette.set((short) (i - 1), (short) 0, (short) 0, "block" + i);
        }
        // Now palette size is 18 (including "air")
        assertEquals(17, palette.getPalette().size());
        // Verify values
        for (int i = 1; i <= 16; i++) {
            String expected = "block" + i;
            assertEquals(expected, palette.get((short) (i - 1), (short) 0, (short) 0));
        }
    }

    @Test
    void testOutOfBoundsThrows() {
        assertThrows(IndexOutOfBoundsException.class,
            () -> palette.get((short) -1, (short) 0, (short) 0));
        assertThrows(IndexOutOfBoundsException.class,
            () -> palette.get((short) 0, (short) 16, (short) 0));
        assertThrows(IndexOutOfBoundsException.class,
            () -> palette.set((short) 0, (short) 0, (short) 16, "stone"));
    }

    @Test
    void testDefaultBackAfterOverwrite() {
        palette.set((short) 8, (short) 8, (short) 8, "stone");
        assertEquals("stone", palette.get((short) 8, (short) 8, (short) 8));
        // Set back to default
        palette.set((short) 8, (short) 8, (short) 8, "air");
        assertEquals("air", palette.get((short) 8, (short) 8, (short) 8));
    }

    @Test
    void testCopyIndependence() {
        palette.set((short) 2, (short) 2, (short) 2, "sand");
        ThreeDimensionalPalette<String> clone = palette.copy();
        clone.set((short) 2, (short) 2, (short) 2, "gravel");
        assertEquals("sand", palette.get((short) 2, (short) 2, (short) 2));
        assertEquals("gravel", clone.get((short) 2, (short) 2, (short) 2));
    }
}
