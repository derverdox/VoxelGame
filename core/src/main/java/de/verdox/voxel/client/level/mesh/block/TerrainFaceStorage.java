package de.verdox.voxel.client.level.mesh.block;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.client.level.TerrainManager;
import de.verdox.voxel.client.level.mesh.block.face.BlockFace;
import de.verdox.voxel.client.level.chunk.RenderableChunk;
import de.verdox.voxel.client.renderer.mesh.BlockRenderer;
import de.verdox.voxel.client.util.RegionalLock;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.util.Direction;

public interface TerrainFaceStorage {
    int getScaleX();

    int getScaleY();

    int getScaleZ();

    int getAmountFloats();

    int getAmountIndices();

    int getSize();

    TerrainFaceStorage createGreedyMeshedCopy(int lodLevel);

    ChunkFaceStorage getOrCreateChunkFaces(int chunkCoordinateInRegionX, int chunkCoordinateInRegionY, int chunkCoordinateInRegionZ);

    void forEachChunkFace(ChunkFaceStorageConsumer consumer);

    RegionalLock getRegionalLock();

    void collectFaces(float[] vertices, int[] indices, byte lodLevel, TextureAtlas textureAtlas);

    boolean hasFacesForChunk(int chunkCoordinateInRegionX, int chunkCoordinateInRegionY, int chunkCoordinateInRegionZ);

    default int getSizeU(Direction direction) {
        return switch (direction) {
            case WEST, EAST -> this.getScaleZ();
            case DOWN, UP, NORTH, SOUTH -> this.getScaleX();
        };
    }

    default int getSizeV(Direction direction) {
        return switch (direction) {
            case WEST, EAST, DOWN, UP -> this.getScaleZ();
            case NORTH, SOUTH -> this.getScaleY();
        };
    }

    interface ChunkFaceStorage {
        void addBlockFace(BlockFace blockFace);

        void removeBlockFace(Direction direction, short u, short v, short w);

        boolean isEmpty();

        default void generateFace(TerrainManager terrainManager, RenderableChunk chunk, ResourceLocation textureKey, BlockModelType.BlockFace blockFace, byte lodLevel, int localX, int localY, int localZ) {
            addBlockFace(BlockRenderer.generateBlockFace(terrainManager, chunk, textureKey, blockFace, lodLevel, localX, localY, localZ));
        }

        void forEachFace(BlockFacesConsumer consumer);

        void collectFaces(float[] vertices, int[] indices, byte lodLevel, TextureAtlas textureAtlas, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks);

        int getAmountFloats();
        int getAmountVertices();

        int getAmountIndices();

        int getSize();
    }

    interface ChunkFaceStorageConsumer {
        void consume(ChunkFaceStorage storage, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks);
    }

    interface BlockFacesConsumer {
        void consume(BlockFace blockFace, int localX, int localY, int localZ);
    }
}
