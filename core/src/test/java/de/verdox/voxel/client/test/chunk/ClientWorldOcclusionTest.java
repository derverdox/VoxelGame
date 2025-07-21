package de.verdox.voxel.client.test.chunk;

import static org.junit.jupiter.api.Assertions.*;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.data.types.Blocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Integration test: multiple adjacent chunks in ClientWorld,
 * validating that FaceMasks and sideOcclusionMask observe neighbors.
 */
class ClientWorldOcclusionTest {
    private ClientWorld world;
    private ClientChunk chunkA, chunkB;
    private static final int SX = 16, SY = 16, SZ = 16;

    @BeforeEach
    void setUp() {
        // Create world with chunk sizes SX, SY, SZ
        world = new ClientWorld(UUID.randomUUID(), 0, 32, (byte) SX, (byte) SY, (byte) SZ);
        // Create two adjacent chunks: A at (0,0,0), B at (1,0,0)
        chunkA = new ClientChunk(world, 0, 0, 0);
        chunkB = new ClientChunk(world, 1, 0, 0);
        world.addChunk(chunkA);
        world.addChunk(chunkB);
    }

    /**
     * Test that a block at the +X edge of chunkA and corresponding block in chunkB
     * hide the +X faces in chunkA's FaceMasks.
     */
    @Test
    void testAdjacencyCullsFacesBetweenChunks() {
        // Fill the entire +X face edge of chunkA at x = SX-1
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                chunkA.setBlockAt(Blocks.STONE, SX - 1, y, z);
                // Also fill the -X face of chunkB at x=0
                chunkB.setBlockAt(Blocks.STONE, 0, y, z);
            }
        }

        FaceMasks masksA = chunkA.getOccupancyMask().getFaceMasks();
        masksA.computeMasks(chunkA.getWorld(), chunkA.getChunkX(), chunkA.getChunkY(), chunkA.getChunkZ(), chunkA.getOccupancyMask().getOccupancyMask());

        long[][] xPlusA = masksA.xPlus;
        // All y,z at x = SX-1 should have mask zero (no visible faces)
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                // xPlus is indexed [x][y], bits represent z
                long bits = xPlusA[SX - 1][y];
                assertEquals(0L, bits, String.format("Expected +X-face at edge y=%d to be culled, got bits=0x%X", y, bits));
            }
        }
    }
}

