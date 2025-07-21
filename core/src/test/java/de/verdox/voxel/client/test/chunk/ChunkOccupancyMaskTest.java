package de.verdox.voxel.client.test.chunk;

import static org.junit.jupiter.api.Assertions.*;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.occupancy.NaiveChunkOccupancyMask;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.data.types.Blocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ChunkOccupancyMaskTest {

    private NaiveChunkOccupancyMask com;


    private final int SX = 16, SY = 16, SZ = 16;
    private ClientWorld clientWorld;
    private ClientChunk chunk;

    @BeforeEach
    void setUp() {
        clientWorld = new ClientWorld(UUID.randomUUID(), 0, 32, (byte) SX, (byte) SY, (byte) SZ);
        chunk = new ClientChunk(
            clientWorld,  // world is not needed for occupancy test
            0, 0, 0
        );
        com = chunk.getOccupancyMask();
    }

    /**
     * Test that updateOccupancyMask correctly sets and clears bits,
     * and that isOpaque returns the expected values.
     */
    @Test
    void testUpdateAndIsOpaque() {
        // Zuerst: leer → alles transparent
        for (int x = 0; x < SX; x++) {
            for (int y = 0; y < SY; y++) {
                for (int z = 0; z < SZ; z++) {
                    assertFalse(com.isOpaque(x, y, z), "should be transparent initially");
                }
            }
        }

        com.updateOccupancyMask(Blocks.STONE, 1, 1, 2);
        assertTrue(com.isOpaque(1, 1, 2));
        com.updateOccupancyMask(Blocks.AIR, 1, 1, 2);
        assertFalse(com.isOpaque(1, 1, 2));
    }

    /**
     * Test that a single opaque voxel in the center produces visible faces
     * on all six sides at the correct bit position.
     */
    @Test
    void testFaceMasksSingleVoxelCenter() {
        com.updateOccupancyMask(Blocks.STONE, 1, 1, 1);

        FaceMasks m = com.getFaceMasks();
        m.computeMasks(clientWorld, com.getGameChunk().getChunkX(), com.getGameChunk().getChunkY(), com.getGameChunk().getChunkZ(), com.getOccupancyMask());


        long expected = 1L << 1;
        // X+
        assertEquals(expected, m.xPlus[1][1]);
        // X-
        assertEquals(expected, m.xMinus[1][1]);
        // Y+
        assertEquals(expected, m.yPlus[1][1]);
        // Y-
        assertEquals(expected, m.yMinus[1][1]);
        // Z+
        assertEquals(expected, m.zPlus[1][1]);
        // Z-
        assertEquals(expected, m.zMinus[1][1]);

        // Alle anderen (x,y) müssen 0 sein
        for (int x = 0; x < SX; x++) {
            for (int y = 0; y < SY; y++) {
                if (x == 1 && y == 1) continue;
                assertEquals(0L, m.xPlus[x][y]);
                assertEquals(0L, m.xMinus[x][y]);
                assertEquals(0L, m.yPlus[x][y]);
                assertEquals(0L, m.yMinus[x][y]);
                assertEquals(0L, m.zPlus[x][y]);
                assertEquals(0L, m.zMinus[x][y]);
            }
        }
    }

    /**
     * Test that two adjacent opaque voxels remove the shared face,
     * while keeping the outer faces visible.
     */
    @Test
    void testFaceMasksTwoAdjacentVoxelsCullBetween() {
        // Mache zwei nebeneinanderliegende opaque Blöcke (1,1,1) und (2,1,1)
        com.updateOccupancyMask(Blocks.STONE, 1, 1, 1);
        com.updateOccupancyMask(Blocks.STONE, 2, 1, 1);

        FaceMasks m = com.getFaceMasks();
        m.computeMasks(clientWorld, com.getGameChunk().getChunkX(), com.getGameChunk().getChunkY(), com.getGameChunk().getChunkZ(), com.getOccupancyMask());

        long expected = 1L << 1;

        // X+ an (1,1) prüft curr=1,1 und next=2,1 → beide opaque → 1 & ~1 = 0
        assertEquals(0L, m.xPlus[1][1]);
        // X- an (2,1) prüft curr=2,1 und prev=1,1 → beide opaque → 1 & ~1 = 0
        assertEquals(0L, m.xMinus[2][1]);

        // Aber X- an (1,1) prüft prev=(0,1)=0 → 1 & ~0 = 1<<1
        assertEquals(expected, m.xMinus[1][1]);
        // und X+ an (2,1) mit next außerhalb, wird als Air behandelt → 1 & ~0 = bit
        assertEquals(expected, m.xPlus[2][1]);

        // Y-Faces an (1,1) und (2,1) weiterhin sichtbar in Y±
        assertEquals(expected, m.yPlus[1][1]);
        assertEquals(expected, m.yMinus[1][1]);
        assertEquals(expected, m.yPlus[2][1]);
        assertEquals(expected, m.yMinus[2][1]);

        // Z-Faces an beiden Punkten sichtbar
        assertEquals(expected, m.zPlus[1][1]);
        assertEquals(expected, m.zMinus[1][1]);
        assertEquals(expected, m.zPlus[2][1]);
        assertEquals(expected, m.zMinus[2][1]);
    }

    /**
     * Test that an empty chunk (all transparent) produces no visible faces
     * in any of the face masks.
     */
    @Test
    void testEmptyChunkNoFaces() {
        // Nie update → alles transparent
        FaceMasks m = com.getFaceMasks();
        m.computeMasks(clientWorld, com.getGameChunk().getChunkX(), com.getGameChunk().getChunkY(), com.getGameChunk().getChunkZ(), com.getOccupancyMask());

        for (int x = 0; x < SX; x++) {
            for (int y = 0; y < SY; y++) {
                assertEquals(0L, m.xPlus[x][y]);
                assertEquals(0L, m.xMinus[x][y]);
                assertEquals(0L, m.yPlus[x][y]);
                assertEquals(0L, m.yMinus[x][y]);
                assertEquals(0L, m.zPlus[x][y]);
                assertEquals(0L, m.zMinus[x][y]);
            }
        }
    }

    /**
     * Initially, the chunk should be empty (no opaque voxels).
     */
    @Test
    void testEmptyAfterClear() {
        assertTrue(com.isChunkEmpty(), "Chunk must be empty after clear");
        assertFalse(com.isChunkFullOpaque(), "Chunk should not be full opaque when empty");
        assertEquals(0, com.getTotalOpaque(), "Total opaque count should be zero");
        for (int x = 0; x < SX; x++) {
            for (int y = 0; y < SY; y++) {
                assertEquals(0, com.getColumnOpaqueCount(x, y),
                    String.format("Column (%d,%d) count should be zero", x, y));
            }
        }
    }

    /**
     * Setting a single block should update empty/full flags and counts.
     */
    @Test
    void testSingleBlockUpdate() {
        int x = 1, y = 2, z = 3;
        // Set one opaque
        com.updateOccupancyMask(Blocks.STONE, x, y, z);
        assertFalse(com.isChunkEmpty(), "Chunk should not be empty after one block");
        assertFalse(com.isChunkFullOpaque(), "Chunk should not be full opaque after one block");
        assertEquals(1, com.getTotalOpaque(), "Total opaque count must be 1");
        assertEquals(1, com.getColumnOpaqueCount(x, y),
            "Column opaque count at (1,2) must be 1");
        // Other columns remain zero
        for (int i = 0; i < SX; i++)
            for (int j = 0; j < SY; j++)
                if (!(i == x && j == y)) {
                    assertEquals(0, com.getColumnOpaqueCount(i, j));
                }
        // Clear the block back to air
        com.updateOccupancyMask(Blocks.AIR, x, y, z);
        assertTrue(com.isChunkEmpty(), "Chunk should be empty after clearing");
        assertEquals(0, com.getTotalOpaque(), "Total opaque count must be 0");
    }

    /**
     * Filling the entire chunk should mark it as full opaque and non-empty.
     */
    @Test
    void testFullChunkOpaque() {
        // Fill all voxels
        for (int x = 0; x < SX; x++) {
            for (int y = 0; y < SY; y++) {
                for (int z = 0; z < SZ; z++) {
                    com.updateOccupancyMask(Blocks.STONE, x, y, z);
                }
            }
        }
        assertFalse(com.isChunkEmpty(), "Chunk should not be empty when full");
        assertTrue(com.isChunkFullOpaque(), "Chunk should be full opaque when all set");
        assertEquals(SX * SY * SZ, com.getTotalOpaque(),
            "Total opaque count must equal total voxels");
        for (int x = 0; x < SX; x++) {
            for (int y = 0; y < SY; y++) {
                assertEquals(SZ, com.getColumnOpaqueCount(x, y),
                    String.format("Column (%d,%d) must have %d opaque bits", x, y, SZ));
            }
        }
    }
}

