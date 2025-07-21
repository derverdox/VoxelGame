package de.verdox.voxel.client.test.chunk;

import static org.junit.jupiter.api.Assertions.*;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.occupancy.NaiveChunkOccupancyMask;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.data.types.Blocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Integration tests for ClientChunk and ChunkOccupancyMask.
 * Verifies that updating blocks in a chunk correctly updates the occupancy mask.
 */
public class ClientChunkIntegrationTest {

    private ClientChunk chunk;
    private static final int SX = 16, SY = 16, SZ = 16;

    @BeforeEach
    void setUp() {
        ClientWorld clientWorld = new ClientWorld(UUID.randomUUID(),0 ,32,  (byte) SX, (byte) SY, (byte) SZ);

        chunk = new ClientChunk(
            clientWorld,  // world is not needed for occupancy test
            0, 0, 0
        );
    }

    /**
     * Test that initOccupancyMask marks no blocks opaque in a new chunk.
     */
    @Test
    void testInitialOccupancyMaskEmpty() {
        NaiveChunkOccupancyMask com = chunk.getOccupancyMask();
        // All positions in a new chunk should be transparent
        for (int x = 0; x < SX; x++) {
            for (int y = 0; y < SY; y++) {
                for (int z = 0; z < SZ; z++) {
                    assertFalse(
                        com.isOpaque(x, y, z),
                        String.format("Expected transparent at (%d,%d,%d)", x, y, z)
                    );
                }
            }
        }
    }

    /**
     * Test that setting a block to STONE updates the occupancy mask to opaque
     * and that resetting it to AIR clears the mask again.
     */
    @Test
    void testOccupancyMaskUpdateOnSetBlock() {
        NaiveChunkOccupancyMask com = chunk.getOccupancyMask();
        int x = 5, y = 6, z = 7;

        // Place a STONE block and verify occupancy
        chunk.setBlockAt(Blocks.STONE, x, y, z);
        assertTrue(
            com.isOpaque(x, y, z),
            "Block at (5,6,7) should be marked opaque after setBlockAt(STONE)"
        );

        // Remove the block (set to AIR) and verify transparency
        chunk.setBlockAt(Blocks.AIR, x, y, z);
        assertFalse(
            com.isOpaque(x, y, z),
            "Block at (5,6,7) should be transparent after setBlockAt(AIR)"
        );
    }

    /**
     * Test that computeFaceMasks reflects a newly placed block by
     * producing correct visibility bits on its faces.
     */
    @Test
    void testFaceMasksIntegration() {
        NaiveChunkOccupancyMask com = chunk.getOccupancyMask();
        // Put a block at the boundary to test neighbor retrieval as AIR
        chunk.setBlockAt(Blocks.STONE, 0, 0, 0);

        FaceMasks masks = com.getFaceMasks();
        masks.computeMasks(chunk.getWorld(), com.getGameChunk().getChunkX(), com.getGameChunk().getChunkY(), com.getGameChunk().getChunkZ(), com.getOccupancyMask());

        long expectedBit = 1L << 0; // z=0 position
        // At (0,0) +X face should be visible
        assertEquals(
            expectedBit,
            masks.xPlus[0][0],
            "+X face at (0,0) should be visible"
        );
        // -X face at (0,0) queries neighbor as AIR, so also visible
        assertEquals(
            expectedBit,
            masks.xMinus[0][0],
            "-X face at (0,0) should be visible"
        );
        // Similarly Y faces at (0,0)
        assertEquals(expectedBit, masks.yPlus[0][0], "+Y face visible");
        assertEquals(expectedBit, masks.yMinus[0][0], "-Y face visible");
        // Z+ and Z- faces
        assertEquals(expectedBit, masks.zPlus[0][0], "+Z face visible");
        assertEquals(expectedBit, masks.zMinus[0][0], "-Z face visible");
    }
}

