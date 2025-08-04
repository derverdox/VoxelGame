import de.verdox.voxel.shared.level.chunk.Chunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkMathTest {
    // Maske für 21 Bits
    private static final int MASK = 0x1FFFFF;

    @Test
    void testZeroCoordinates() {
        int x = 0, y = 0, z = 0;
        long key = Chunk.computeChunkKey(x, y, z);
        assertEquals(0L, key, "Key for (0,0,0) sollte 0 sein");
        assertEquals(0, Chunk.unpackChunkX(key));
        assertEquals(0, Chunk.unpackChunkY(key));
        assertEquals(0, Chunk.unpackChunkZ(key));
    }

    @Test
    void testPositiveCoordinates() {
        int x = 1, y = 2, z = 3;
        long expected = (1L << 42) | (2L << 21) | 3L;
        long key = Chunk.computeChunkKey(x, y, z);

        assertEquals(expected, key, "Key stimmt nicht für (1,2,3)");
        assertEquals(x, Chunk.unpackChunkX(key));
        assertEquals(y, Chunk.unpackChunkY(key));
        assertEquals(z, Chunk.unpackChunkZ(key));
    }

    @Test
    void testMaxCoordinates() {
        // Maximal darstellbarer Wert innerhalb 21 Bits
        int max = MASK;
        long key = Chunk.computeChunkKey(max, max, max);
        assertEquals(max, Chunk.unpackChunkX(key));
        assertEquals(max, Chunk.unpackChunkY(key));
        assertEquals(max, Chunk.unpackChunkZ(key));
    }

    @Test
    void testNegativeCoordinates() {
        // Negative Werte werden beim Packen auf die unteren 21 Bits gemasked
        int x = -1, y = -2, z = -3;
        long key = Chunk.computeChunkKey(x, y, z);

        assertEquals(x & MASK, Chunk.unpackChunkX(key),
            "UnpackChunkX sollte x & MASK zurückliefern");
        assertEquals(y & MASK, Chunk.unpackChunkY(key),
            "UnpackChunkY sollte y & MASK zurückliefern");
        assertEquals(z & MASK, Chunk.unpackChunkZ(key),
            "UnpackChunkZ sollte z & MASK zurückliefern");
    }

    @Test
    void testRoundTripRandomValues() {
        // Stichprobenhafte Zufallswerte, um die Roundtrip-Eigenschaft zu prüfen
        int[] samples = {-1048576, -12345, 0, 42, 999999, MASK};
        for (int x : samples) {
            for (int y : samples) {
                for (int z : samples) {
                    long key = Chunk.computeChunkKey(x, y, z);
                    assertEquals(x & MASK, Chunk.unpackChunkX(key),
                        () -> "Fehler bei X=" + x);
                    assertEquals(y & MASK, Chunk.unpackChunkY(key),
                        () -> "Fehler bei Y=" + y);
                    assertEquals(z & MASK, Chunk.unpackChunkZ(key),
                        () -> "Fehler bei Z=" + z);
                }
            }
        }
    }
}
