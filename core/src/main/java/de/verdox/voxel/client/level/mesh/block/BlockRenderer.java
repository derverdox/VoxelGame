package de.verdox.voxel.client.level.mesh.block;

import com.esotericsoftware.kryo.util.Null;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;

public class BlockRenderer {
    public static BlockFace generateBlockFace(@Null ResourceLocation textureKey, BlockModelType.BlockFace blockFace, int blockXInMesh, int blockYInMesh, int blockZInMesh) {
        float cubeBoundingBoxHalf = 0.5f;

        float x1 = (blockXInMesh + blockFace.c1().cornerX() + cubeBoundingBoxHalf);
        float y1 = (blockYInMesh + blockFace.c1().cornerY() + cubeBoundingBoxHalf);
        float z1 = (blockZInMesh + blockFace.c1().cornerZ() + cubeBoundingBoxHalf);

        float x2 = (blockXInMesh + blockFace.c2().cornerX() + cubeBoundingBoxHalf);
        float y2 = (blockYInMesh + blockFace.c2().cornerY() + cubeBoundingBoxHalf);
        float z2 = (blockZInMesh + blockFace.c2().cornerZ() + cubeBoundingBoxHalf);

        float x3 = (blockXInMesh + blockFace.c3().cornerX() + cubeBoundingBoxHalf);
        float y3 = (blockYInMesh + blockFace.c3().cornerY() + cubeBoundingBoxHalf);
        float z3 = (blockZInMesh + blockFace.c3().cornerZ() + cubeBoundingBoxHalf);

        float x4 = (blockXInMesh + blockFace.c4().cornerX() + cubeBoundingBoxHalf);
        float y4 = (blockYInMesh + blockFace.c4().cornerY() + cubeBoundingBoxHalf);
        float z4 = (blockZInMesh + blockFace.c4().cornerZ() + cubeBoundingBoxHalf);

        return new BlockFace(
            x1, y1, z1,
            x2, y2, z2,
            x3, y3, z3,
            x4, y4, z4,
            blockFace.normalX(), blockFace.normalY(), blockFace.normalZ(),
            textureKey
        );
    }
}
