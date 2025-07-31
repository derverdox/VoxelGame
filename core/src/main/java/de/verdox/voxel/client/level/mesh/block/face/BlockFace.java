package de.verdox.voxel.client.level.mesh.block.face;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.util.Direction;

public interface BlockFace {
    void appendToBuffers(
            float[] vertices,
            short[] shortIndices,
            int[] intIndices,
            int vertexOffsetFloats,
            int indexOffset,
            int baseVertexIndex,
            TextureAtlas textureAtlas,
            int floatsPerVertex,
            int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks
    );

    BlockModelType.BlockFace getFaceDefinition();

    BlockFace addOffset(float offsetX, float offsetY, float offsetZ);

    BlockFace addOffset(int offsetX, int offsetY, int offsetZ);

    BlockFace addOffset(int u, int v);

    BlockFace expandU(int i);

    int getFloatsPerVertex();

    int getVerticesPerFace();

    int getIndicesPerFace();

    int getWCoord(Direction dir);

    Direction getDirection();

    BlockFace expandV(int i);

    int getUCoord(Direction dir);

    int getVCoord(Direction dir);

    boolean isGreedyGroup(BlockFace other, Direction direction);

    boolean isLodGroup(BlockFace other, Direction direction, int lodStep);

    float getCorner1X();

    float getCorner1Y();

    float getCorner1Z();

    float getCorner2X();

    float getCorner2Y();

    float getCorner2Z();

    float getCorner3X();

    float getCorner3Y();

    float getCorner3Z();

    float getCorner4X();

    float getCorner4Y();

    float getCorner4Z();

    byte getBlockXInChunk();

    byte getBlockYInChunk();

    byte getBlockZInChunk();

    static int getUCoord(Direction direction, short x, short y, short z) {
        return switch (direction) {
            case UP, DOWN, NORTH, SOUTH -> Short.toUnsignedInt(x);
            case EAST, WEST -> Short.toUnsignedInt(z);
        };
    }

    static int getVCoord(Direction direction, short x, short y, short z) {
        return switch (direction) {
            case UP, DOWN -> Short.toUnsignedInt(z);
            case EAST, WEST, NORTH, SOUTH -> Short.toUnsignedInt(y);
        };
    }

    static int getWCoord(Direction direction, short x, short y, short z) {
        return (int) (x * direction.getOffsetX() + y * direction.getOffsetY() + z * direction.getOffsetZ());
    }
}
