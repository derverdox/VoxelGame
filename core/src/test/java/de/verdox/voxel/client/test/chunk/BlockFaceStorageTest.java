package de.verdox.voxel.client.test.chunk;

import de.verdox.voxel.client.level.mesh.block.BlockFace;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.util.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BlockFaceStorageTest {
    @Test
    void testUpDirection() throws Exception {
        BlockFace faceUp = new BlockFace(
            3f, 0f, 5f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 1f, 0f,
            null
        );

        int u = BlockFaceStorage.getUCoord(faceUp, Direction.UP);
        int v = BlockFaceStorage.getVCoord(faceUp, Direction.UP);
        assertEquals(3, u, "U coordinate for UP should be corner1X");
        assertEquals(5, v, "V coordinate for UP should be corner1Z");
    }

    @Test
    void testEastDirection() throws Exception {
        BlockFace faceEast = new BlockFace(
            0f, 2f, 7f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            1f, 0f, 0f,
            null
        );

        int u = BlockFaceStorage.getUCoord(faceEast, Direction.EAST);
        int v = BlockFaceStorage.getVCoord(faceEast, Direction.EAST);
        assertEquals(7, u, "U coordinate for EAST should be corner1Z");
        assertEquals(2, v, "V coordinate for EAST should be corner1Y");
    }

    @Test
    void testNorthDirection() throws Exception {
        BlockFace faceNorth = new BlockFace(
            4f, 6f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, -1f,
            null
        );

        int u = BlockFaceStorage.getUCoord(faceNorth, Direction.NORTH);
        int v = BlockFaceStorage.getVCoord(faceNorth, Direction.NORTH);
        assertEquals(4, u, "U coordinate for NORTH should be corner1X");
        assertEquals(6, v, "V coordinate for NORTH should be corner1Y");
    }

    @Test
    void testDownDirection() throws Exception {
        BlockFace faceDown = new BlockFace(
            1f, 0f, 9f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, -1f, 0f,
            null
        );

        int u = BlockFaceStorage.getUCoord(faceDown, Direction.DOWN);
        int v = BlockFaceStorage.getVCoord(faceDown, Direction.DOWN);
        assertEquals(1, u, "U coordinate for DOWN should be corner1X");
        assertEquals(9, v, "V coordinate for DOWN should be corner1Z");
    }

    @Test
    void testWestDirection() throws Exception {
        BlockFace faceWest = new BlockFace(
            0f, 3f, 8f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            -1f, 0f, 0f,
            null
        );

        int u = BlockFaceStorage.getUCoord(faceWest, Direction.WEST);
        int v = BlockFaceStorage.getVCoord(faceWest, Direction.WEST);
        assertEquals(8, u, "U coordinate for WEST should be corner1Z");
        assertEquals(3, v, "V coordinate for WEST should be corner1Y");
    }

    @Test
    void testSouthDirection() throws Exception {
        BlockFace faceSouth = new BlockFace(
            2f, 5f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 0f,
            0f, 0f, 1f,
            null
        );

        int u = BlockFaceStorage.getUCoord(faceSouth, Direction.SOUTH);
        int v = BlockFaceStorage.getVCoord(faceSouth, Direction.SOUTH);
        assertEquals(2, u, "U coordinate for SOUTH should be corner1X");
        assertEquals(5, v, "V coordinate for SOUTH should be corner1Y");
    }

    @Test
    void testCreateQuadUnitFace() throws Exception {
        // 1) Erzeuge ein Basis-BlockFace für eine UP-Face an (3,0,5)
        BlockFace base = new BlockFace(
            3f, 0f, 5f,  // corner1
            4f, 0f, 5f,  // corner2 (1× in +X)
            4f, 0f, 6f,  // corner3 (1× +X, +Z)
            3f, 0f, 6f,  // corner4 (1× +Z)
            0f, 1f, 0f,  // normal UP
            ResourceLocation.of("voxel","stone")
        );

        // 2) Rufe createQuad mit u=0,v=0,w=1,h=1 auf
        BlockFace result = BlockFaceStorage.createQuad(base, Direction.UP, 0, 0, 1, 1);

        System.out.println(base);
        System.out.println(result);

        // 3) Überprüfe, dass result exakt alle Eckpunkte des base übernimmt:
        assertEquals(base.corner1X, result.corner1X);
        assertEquals(base.corner1Y, result.corner1Y);
        assertEquals(base.corner1Z, result.corner1Z);

        assertEquals(base.corner2X, result.corner2X);
        assertEquals(base.corner2Y, result.corner2Y);
        assertEquals(base.corner2Z, result.corner2Z);

        assertEquals(base.corner3X, result.corner3X);
        assertEquals(base.corner3Y, result.corner3Y);
        assertEquals(base.corner3Z, result.corner3Z);

        assertEquals(base.corner4X, result.corner4X);
        assertEquals(base.corner4Y, result.corner4Y);
        assertEquals(base.corner4Z, result.corner4Z);

        assertEquals(base.normalX, result.normalX);
        assertEquals(base.normalY, result.normalY);
        assertEquals(base.normalZ, result.normalZ);

        assertEquals(base.textureId, result.textureId);
    }
}

