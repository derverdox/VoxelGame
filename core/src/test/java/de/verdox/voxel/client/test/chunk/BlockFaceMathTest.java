package de.verdox.voxel.client.test.chunk;

import de.verdox.voxel.client.level.mesh.block.face.SingleBlockFace;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.util.Direction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlockFaceMathTest {

    @Test
    public void testSimpleUVOffset() {
        SingleBlockFace face1 = new SingleBlockFace(BlockModelType.BlockFace.full(Direction.UP), (byte) 0, (byte) 0, (byte) 0, null, 0f, (byte) 0);
        SingleBlockFace face2 = new SingleBlockFace(BlockModelType.BlockFace.full(Direction.UP), (byte) 1, (byte) 0, (byte) 1, null, 0f, (byte) 0);

        int uCoord1 = face1.getUCoord(Direction.UP);
        int vCoord1 = face1.getVCoord(Direction.UP);

        SingleBlockFace result = (SingleBlockFace) face2.addOffset(-1, -1);
        int uCoord2 = result.getUCoord(Direction.UP);
        int vCoord2 = result.getVCoord(Direction.UP);

        Assertions.assertEquals(uCoord1, uCoord2);
        Assertions.assertEquals(vCoord1, vCoord2);

        Assertions.assertEquals(face1.getCorner1X(1), result.getCorner1X(1));
        Assertions.assertEquals(face1.getCorner1Y(1), result.getCorner1Y(1));
        Assertions.assertEquals(face1.getCorner1Z(1), result.getCorner1Z(1));

        Assertions.assertEquals(face1.getCorner2X(1), result.getCorner2X(1));
        Assertions.assertEquals(face1.getCorner2Y(1), result.getCorner2Y(1));
        Assertions.assertEquals(face1.getCorner2Z(1), result.getCorner2Z(1));

        Assertions.assertEquals(face1.getCorner3X(1), result.getCorner3X(1));
        Assertions.assertEquals(face1.getCorner3Y(1), result.getCorner3Y(1));
        Assertions.assertEquals(face1.getCorner3Z(1), result.getCorner3Z(1));

        Assertions.assertEquals(face1.getCorner4X(1), result.getCorner4X(1));
        Assertions.assertEquals(face1.getCorner4Y(1), result.getCorner4Y(1));
        Assertions.assertEquals(face1.getCorner4Z(1), result.getCorner4Z(1));
    }
}
