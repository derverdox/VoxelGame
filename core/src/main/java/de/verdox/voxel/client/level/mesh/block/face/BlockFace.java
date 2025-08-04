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
            byte lodLevel,
            int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks
    );

    BlockModelType.BlockFace getBlockFaceDefinition();

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

    default float getCorner1X(float lodScale) {
        return getCornerX(getBlockFaceDefinition().c1(), lodScale);
    }

    default float getCorner1Y(float lodScale) {
        return getCornerY(getBlockFaceDefinition().c1(), lodScale);
    }

    default float getCorner1Z(float lodScale) {
        return getCornerZ(getBlockFaceDefinition().c1(), lodScale);
    }

    default float getCorner2X(float lodScale) {
        return getCornerX(getBlockFaceDefinition().c2(), lodScale);
    }

    default float getCorner2Y(float lodScale) {
        return getCornerY(getBlockFaceDefinition().c2(), lodScale);
    }

    default float getCorner2Z(float lodScale) {
        return getCornerZ(getBlockFaceDefinition().c2(), lodScale);
    }

    default float getCorner3X(float lodScale) {
        return getCornerX(getBlockFaceDefinition().c3(), lodScale);
    }

    default float getCorner3Y(float lodScale) {
        return getCornerY(getBlockFaceDefinition().c3(), lodScale);
    }

    default float getCorner3Z(float lodScale) {
        return getCornerZ(getBlockFaceDefinition().c3(), lodScale);
    }

    default float getCorner4X(float lodScale) {
        return getCornerX(getBlockFaceDefinition().c4(), lodScale);
    }

    default float getCorner4Y(float lodScale) {
        return getCornerY(getBlockFaceDefinition().c4(), lodScale);
    }

    default float getCorner4Z(float lodScale) {
        return getCornerZ(getBlockFaceDefinition().c4(), lodScale);
    }

    default float getNormalX() {
        return getBlockFaceDefinition().normalX();
    }

    default float getNormalY() {
        return getBlockFaceDefinition().normalY();
    }

    default float getNormalZ() {
        return getBlockFaceDefinition().normalZ();
    }

    float getCornerX(BlockModelType.BlockFace.BlockModelCoordinate blockModelCoordinate, float lodScale);

    float getCornerY(BlockModelType.BlockFace.BlockModelCoordinate blockModelCoordinate, float lodScale);

    float getCornerZ(BlockModelType.BlockFace.BlockModelCoordinate blockModelCoordinate, float lodScale);

    default float getCorner1X() {
        return getCorner1X(1);
    }

    default float getCorner1Y() {
        return getCorner1Y(1);
    }

    default float getCorner1Z() {
        return getCorner1Z(1);
    }

    default float getCorner2X() {
        return getCorner2X(1);
    }

    default float getCorner2Y() {
        return getCorner2Y(1);
    }

    default float getCorner2Z() {
        return getCorner2Z(1);
    }

    default float getCorner3X() {
        return getCorner3X(1);
    }

    default float getCorner3Y() {
        return getCorner3Y(1);
    }

    default float getCorner3Z() {
        return getCorner3Z(1);
    }

    default float getCorner4X() {
        return getCorner4X(1);
    }

    default float getCorner4Y() {
        return getCorner4Y(1);
    }

    default float getCorner4Z() {
        return getCorner4Z(1);
    }

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
