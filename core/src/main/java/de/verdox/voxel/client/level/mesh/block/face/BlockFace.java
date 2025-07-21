package de.verdox.voxel.client.level.mesh.block.face;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.util.Direction;

public interface BlockFace {
    void appendToBuffers(
            float[] vertices,
            short[] indices,
            int vertexOffsetFloats,
            int indexOffset,
            short baseVertexIndex,
            TextureAtlas textureAtlas,
            int floatsPerVertex,
            LightAccessor lightAccessor
    );

    BlockFace addOffset(float offsetX, float offsetY, float offsetZ);

    BlockFace addOffset(int offsetX, int offsetY, int offsetZ);

    BlockFace expandU(int i);

    BlockFace expandUBackward(int i);

    int getFloatsPerVertex();

    int getVerticesPerFace();

    int getIndicesPerFace();

    int getWCord(Direction dir);

    Direction getDirection();

    BlockFace expandVBackward(int i);

    BlockFace expandV(int i);

    int getUCoord(Direction dir);

    int getVCoord(Direction dir);

    boolean isMergeable(BlockFace other, Direction direction);
}
