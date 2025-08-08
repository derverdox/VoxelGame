package de.verdox.voxel.client.test.chunk;

import de.verdox.voxel.client.level.mesh.proto.ChunkProtoMesh;
import de.verdox.voxel.client.level.mesh.proto.ProtoMask;
import de.verdox.voxel.client.level.mesh.proto.ProtoMasks;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.LightUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkProtoMesh: ensures writeBits/readBits correct across fields
 */
public class ChunkProtoMeshTest {

    private static class FakeWorld {
        int getLocalXByteSize() {
            return 4;
        }

        int getLocalYByteSize() {
            return 4;
        }

        int getLocalZByteSize() {
            return 4;
        }
    }

    private ChunkProtoMesh mesh;

    @BeforeEach
    void setUp() {
        mesh = new ChunkProtoMesh(null) {
            @Override
            public int getLocalXByteSize() {
                return 4;
            }

            @Override
            public int getLocalYByteSize() {
                return 4;
            }

            @Override
            public int getLocalZByteSize() {
                return 4;
            }
        };
    }

    @Test
    @DisplayName("Single face write-read consistency for fixed values")
    void testSingleFaceWriteRead() {
        byte x = 0xA;  // 10
        byte y = 0x5;  // 5
        byte z = 0xF;  // 15
        Direction dir = Direction.NORTH;
        byte ao = LightUtil.packAo((byte) 0, (byte) 1, (byte) 1, (byte) 0);
        byte skyLight = 15;
        byte redLight = 3;
        byte greenLight = 8;
        byte blueLight = 5;

        ProtoMasks.SINGLE.storeFace(mesh, ProtoMask.Type.OPAQUE, x, y, z, dir, ao, skyLight, redLight, greenLight, blueLight);
        ChunkProtoMesh.FaceData data = ProtoMasks.SINGLE.get(mesh, ProtoMask.Type.OPAQUE, 0);

        assertEquals(x, data.x, "X coordinate should match");
        assertEquals(y, data.y, "Y coordinate should match");
        assertEquals(z, data.z, "Z coordinate should match");
        assertEquals(dir, data.direction, "Direction should match");
        assertEquals(ao, data.ao, "AO value should match");
        assertEquals(skyLight, data.sky, "SkyLight value should match");
        assertEquals(redLight, data.red, "RedLight value should match");
        assertEquals(greenLight, data.green, "GreenLight value should match");
        assertEquals(blueLight, data.blue, "BlueLight value should match");
    }

    @Test
    @DisplayName("Multiple faces sequence consistency")
    void testMultipleFaces() {
        int count = 50;
        byte[] xs = new byte[count];
        byte[] ys = new byte[count];
        byte[] zs = new byte[count];
        byte[] aos = new byte[count];
        Direction[] dirs = Direction.values();

        // fill increasing patterns
        for (int i = 0; i < count; i++) {
            xs[i] = (byte) (i & 0xF);
            ys[i] = (byte) ((i + 3) & 0xF);
            zs[i] = (byte) ((i + 7) & 0xF);
            aos[i] = (byte) ((i * 5) & 0xFF);
            Direction dir = dirs[i % dirs.length];
            ProtoMasks.SINGLE.storeFace(mesh, ProtoMask.Type.OPAQUE, xs[i], ys[i], zs[i], dir, aos[i], (byte) 15, (byte) 0, (byte) 3, (byte) 4);
        }

        for (int i = 0; i < count; i++) {
            ChunkProtoMesh.FaceData data = ProtoMasks.SINGLE.get(mesh, ProtoMask.Type.OPAQUE, i);

            assertEquals(xs[i], data.x, "X mismatch at index " + i);
            assertEquals(ys[i], data.y, "Y mismatch at index " + i);
            assertEquals(zs[i], data.z, "Z mismatch at index " + i);
            assertEquals(dirs[i % dirs.length], data.direction, "Dir mismatch at index " + i);
            assertEquals(aos[i], data.ao, "AO mismatch at index " + i);
            assertEquals(15, data.sky, "SkyLight mismatch at index " + i);
            assertEquals(0, data.red, "RedLight mismatch at index " + i);
            assertEquals(3, data.green, "GreenLight mismatch at index " + i);
            assertEquals(4, data.blue, "BlueLight mismatch at index " + i);
        }
    }
}
