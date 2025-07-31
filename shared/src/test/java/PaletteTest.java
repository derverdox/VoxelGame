import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStorage;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PaletteTest {
    private ThreeDimensionalPalette<String> palette;
    private PaletteStrategy.Paletted<String> strategy;

    @BeforeEach
    public void setup() {
        palette = new ThreeDimensionalPalette<>("air") {
            @Override
            public int getSizeX() {
                return 16;
            }

            @Override
            public int getSizeZ() {
                return 16;
            }

            @Override
            public int getSizeY() {
                return 16;
            }
        };

        strategy = new PaletteStrategy.Paletted<>(palette);
    }

    @Test
    public void testComputeRequiredBits() {
        Assertions.assertEquals(1, PaletteStorage.computeRequiredBitsPerEntry(0));
        Assertions.assertEquals(1, PaletteStorage.computeRequiredBitsPerEntry(1));
        Assertions.assertEquals(1, PaletteStorage.computeRequiredBitsPerEntry(2));
        Assertions.assertEquals(2, PaletteStorage.computeRequiredBitsPerEntry(4));
        Assertions.assertEquals(3, PaletteStorage.computeRequiredBitsPerEntry(8));
        Assertions.assertEquals(4, PaletteStorage.computeRequiredBitsPerEntry(16));
        Assertions.assertEquals(5, PaletteStorage.computeRequiredBitsPerEntry(32));
        Assertions.assertEquals(6, PaletteStorage.computeRequiredBitsPerEntry(64));
        Assertions.assertEquals(7, PaletteStorage.computeRequiredBitsPerEntry(128));
        Assertions.assertEquals(8, PaletteStorage.computeRequiredBitsPerEntry(256));
        Assertions.assertEquals(9, PaletteStorage.computeRequiredBitsPerEntry(512));
    }

    @Test
    public void testInsertAndGet() {
        strategy.set((short) 1, (short) 1, (short) 1, "stone", palette);
        Assertions.assertEquals("stone", strategy.get((short) 1, (short) 1, (short) 1, palette));
        Assertions.assertEquals("air", strategy.get((short) 2, (short) 1, (short) 1, palette));
    }

    @Test
    public void testInsertAndGetAfterResizeToNewPalette() {
        strategy.set((short) 1, (short) 1, (short) 1, "stone", palette);

        Assertions.assertEquals("stone", strategy.get((short) 1, (short) 1, (short) 1, palette));
        Assertions.assertEquals("air", strategy.get((short) 2, (short) 1, (short) 1, palette));
    }
}
