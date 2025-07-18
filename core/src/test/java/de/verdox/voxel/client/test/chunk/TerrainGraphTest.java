package de.verdox.voxel.client.test.chunk;


import de.verdox.voxel.client.level.mesh.terrain.TerrainGraph;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainGraphTest {
    private TerrainGraph graph;

    @BeforeEach
    void setUp() {
        graph = new TerrainGraph(null, new RegionBounds(4, 4, 4), null, 16, 16, 16);
    }

    @Test
    void testAddAndGetRegion() {
        // Initially no region
        assertNull(graph.getRegion(0, 0, 0));

        // Add a single region
        graph.addRegion(0, 0, 0);
        TerrainGraph.RegionNode r = graph.getRegion(0, 0, 0);
        assertNotNull(r);
        assertEquals(ChunkBase.computeChunkKey(0, 0, 0), r.pos);

        // No neighbors on any axis
        assertNull(r.nextXPos);
        assertNull(r.nextXNeg);
        assertNull(r.nextYPos);
        assertNull(r.nextYNeg);
        assertNull(r.nextZPos);
        assertNull(r.nextZNeg);
    }

    @Test
    void testNeighborLinkingAlongZ() {
        // Add two regions along Z: (0,0,0) and (0,0,2)
        graph.addRegion(0, 0, 0);
        graph.addRegion(0, 0, 2);

        TerrainGraph.RegionNode a = graph.getRegion(0, 0, 0);
        TerrainGraph.RegionNode b = graph.getRegion(0, 0, 2);

        // They should link as neighbors in Z
        assertNull(a.nextZNeg);
        assertEquals(b, a.nextZPos);
        assertEquals(a, b.nextZNeg);
        assertNull(b.nextZPos);
    }

    @Test
    void testInsertionSkipsEmptyBetween() {
        // Add regions at z=0 and z=2
        graph.addRegion(0, 0, 0);
        graph.addRegion(0, 0, 2);
        // Now add at z=1
        graph.addRegion(0, 0, 1);

        TerrainGraph.RegionNode low = graph.getRegion(0, 0, 0);
        TerrainGraph.RegionNode mid = graph.getRegion(0, 0, 1);
        TerrainGraph.RegionNode high = graph.getRegion(0, 0, 2);

        // Check ordering: low <-> mid <-> high
        assertEquals(mid, low.nextZPos);
        assertEquals(low, mid.nextZNeg);
        assertEquals(high, mid.nextZPos);
        assertEquals(mid, high.nextZNeg);
    }

    @Test
    void testRemoveRegionUpdatesLinks() {
        // Create a chain along X: (-1,0,0), (0,0,0), (1,0,0)
        graph.addRegion(-1, 0, 0);
        graph.addRegion(0, 0, 0);
        graph.addRegion(1, 0, 0);

        TerrainGraph.RegionNode left = graph.getRegion(-1, 0, 0);
        TerrainGraph.RegionNode center = graph.getRegion(0, 0, 0);
        TerrainGraph.RegionNode right = graph.getRegion(1, 0, 0);

        // Verify initial links
        assertEquals(center, left.nextXPos);
        assertEquals(left, center.nextXNeg);
        assertEquals(right, center.nextXPos);
        assertEquals(center, right.nextXNeg);

        // Remove center
        graph.removeRegion(0, 0, 0);
        assertNull(graph.getRegion(0, 0, 0));

        // Left and right should now link directly
        left = graph.getRegion(-1, 0, 0);
        right = graph.getRegion(1, 0, 0);
        assertEquals(right, left.nextXPos);
        assertEquals(left, right.nextXNeg);
    }

    @Test
    void testRemoveEndRegion() {
        graph.addRegion(2, 2, 2);
        TerrainGraph.RegionNode end = graph.getRegion(2, 2, 2);
        assertNotNull(end);
        graph.removeRegion(2, 2, 2);
        assertNull(graph.getRegion(2, 2, 2));
    }

    @Test
    void testReinsertionRestoresLinks() {
        graph.addRegion(0, 0, 0);
        graph.addRegion(0, 1, 0);
        graph.removeRegion(0, 1, 0);
        assertNull(graph.getRegion(0, 1, 0));

        graph.addRegion(0, 1, 0);
        TerrainGraph.RegionNode a = graph.getRegion(0, 0, 0);
        TerrainGraph.RegionNode b = graph.getRegion(0, 1, 0);
        assertEquals(b, a.nextYPos);
        assertEquals(a, b.nextYNeg);
    }

    @Test
    void testAddingSameRegionTwice() {
        graph.addRegion(5, 5, 5);
        TerrainGraph.RegionNode first = graph.getRegion(5, 5, 5);
        graph.addRegion(5, 5, 5);
        TerrainGraph.RegionNode second = graph.getRegion(5, 5, 5);
        assertSame(first, second);
    }
}
