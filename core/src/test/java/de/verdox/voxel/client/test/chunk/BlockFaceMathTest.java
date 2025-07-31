package de.verdox.voxel.client.test.chunk;

import de.verdox.voxel.client.level.mesh.block.face.SingleBlockFace;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.util.Direction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlockFaceMathTest {

    @Test
    public void testSimpleUVOffset() {
        SingleBlockFace face1 = new SingleBlockFace(BlockModelType.BlockFace.full(Direction.UP), (byte) 0, (byte) 0, (byte) 0, (byte) 0, null, 0f, (byte) 0);
        SingleBlockFace face2 = new SingleBlockFace(BlockModelType.BlockFace.full(Direction.UP), (byte) 1, (byte) 0, (byte) 1, (byte) 0, null, 0f, (byte) 0);

        int uCoord1 = face1.getUCoord(Direction.UP);
        int vCoord1 = face1.getVCoord(Direction.UP);

        SingleBlockFace result = (SingleBlockFace) face2.addOffset(-1, -1);
        int uCoord2 = result.getUCoord(Direction.UP);
        int vCoord2 = result.getVCoord(Direction.UP);

        Assertions.assertEquals(uCoord1, uCoord2);
        Assertions.assertEquals(vCoord1, vCoord2);

        Assertions.assertEquals(face1.getCorner1X(), result.getCorner1X());
        Assertions.assertEquals(face1.getCorner1Y(), result.getCorner1Y());
        Assertions.assertEquals(face1.getCorner1Z(), result.getCorner1Z());

        Assertions.assertEquals(face1.getCorner2X(), result.getCorner2X());
        Assertions.assertEquals(face1.getCorner2Y(), result.getCorner2Y());
        Assertions.assertEquals(face1.getCorner2Z(), result.getCorner2Z());

        Assertions.assertEquals(face1.getCorner3X(), result.getCorner3X());
        Assertions.assertEquals(face1.getCorner3Y(), result.getCorner3Y());
        Assertions.assertEquals(face1.getCorner3Z(), result.getCorner3Z());

        Assertions.assertEquals(face1.getCorner4X(), result.getCorner4X());
        Assertions.assertEquals(face1.getCorner4Y(), result.getCorner4Y());
        Assertions.assertEquals(face1.getCorner4Z(), result.getCorner4Z());
    }
}
