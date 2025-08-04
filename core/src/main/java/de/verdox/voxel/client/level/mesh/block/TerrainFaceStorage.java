package de.verdox.voxel.client.level.mesh.block;

import com.esotericsoftware.kryo.util.Null;
import de.verdox.voxel.client.level.mesh.block.face.BlockFace;
import de.verdox.voxel.client.level.mesh.terrain.TerrainChunk;
import de.verdox.voxel.client.level.mesh.terrain.TerrainManager;
import de.verdox.voxel.client.util.RegionalLock;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;

import java.util.function.Consumer;

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

        void generateFace(
                TerrainManager terrainManager,
                Chunk chunk, @Null ResourceLocation textureKey, BlockModelType.BlockFace blockFace, byte lodLevel,
                int localX, int localY, int localZ
        );

        void forEachFace(BlockFacesConsumer consumer);

        int getAmountFloats();

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
