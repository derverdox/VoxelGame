package de.verdox.voxel.client.test.chunk;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ChunkVisibilityGraph;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.data.types.Blocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

/**
 * Unit tests for ChunkVisibilityGraph:
 * - neighbor linking
 * - BFS-based visibility
 * - empty-chunk skipping and dynamic updates
 */
class ChunkVisibilityGraphTest {
    private ClientWorld world;
    private ChunkVisibilityGraph graph;
    private ClientChunk chunkA, chunkB;
    private static final int SX = 16, SY = 16, SZ = 16;

    /**
     * stub camera whose frustum always accepts any AABB
     */
    private Frustum makeFrustum() {
        return new Frustum() {
            @Override
            public boolean boundsInFrustum(BoundingBox box) {
                return true;
            }
        };
    }

    @BeforeEach
    void setUp() {
        world = new ClientWorld(UUID.randomUUID(), 0, 0,
            (byte) SX, (byte) SY, (byte) SZ);
        graph = world.getChunkVisibilityGraph();

        chunkA = new ClientChunk(world, 0, 0, 0);
        chunkB = new ClientChunk(world, 1, 0, 0);

        world.addChunk(chunkA);
        world.addChunk(chunkB);
    }

    /**
     * Test that two loaded chunks become visible after marking neighbor non-empty.
     */
    @Test
    void testNeighborVisibility() {
        Frustum frustum = makeFrustum();
        Vector3 cameraPos = new Vector3(0.5f, 0.5f, 0.5f); // inside chunkA

        // Initially both chunks are empty: only chunkA visible
        List<ClientChunk> visible = graph.computeVisibleChunks(frustum, cameraPos);
        assertEquals(1, visible.size());
        assertTrue(visible.contains(chunkA));

        // Mark chunkB non-empty to allow visibility
        // Place one block in chunkB at its origin
        chunkB.setBlockAt(Blocks.STONE, 0,0,0);
        graph.blockUpdateInChunk(chunkB);

        visible = graph.computeVisibleChunks(frustum, cameraPos);
        assertEquals(2, visible.size(), "Both chunks should now be visible");
        assertTrue(visible.contains(chunkA));
        assertTrue(visible.contains(chunkB));
    }

    /**
     * Test that occlusion via sideMask prevents neighbor from being visible.
     */
    @Test
    void testOcclusionCullsNeighbor() {
        Frustum frustum = makeFrustum();
        Vector3 cameraPos = new Vector3(0.5f, 0.5f, 0.5f);

        // Occlude entire +X side of chunkA by filling edge and neighbor
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                chunkA.setBlockAt(Blocks.STONE, SX-1, y, z);
                graph.blockUpdateInChunk(chunkA);
                chunkB.setBlockAt(Blocks.STONE, 0, y, z);
                graph.blockUpdateInChunk(chunkB);
            }
        }

        List<ClientChunk> visible = graph.computeVisibleChunks(frustum, cameraPos);
        assertEquals(1, visible.size(), "ChunkB should be occluded");
        assertTrue(visible.contains(chunkA));
        assertFalse(visible.contains(chunkB));
    }
}
