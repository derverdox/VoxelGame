package de.verdox.voxel.shared.level.world;

import de.verdox.voxel.server.level.chunk.ChunkMap;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.DelegateBase;
import de.verdox.voxel.shared.util.Direction;

import java.util.UUID;

public interface World extends DelegateBase<DelegateWorld> {
    int MAX_CHUNK_SIZE = 64;

    UUID getUuid();

    byte getChunkSizeX();

    byte getChunkSizeY();

    byte getChunkSizeZ();

    default int getLocalXByteSize() {
        return Integer.SIZE - Integer.numberOfLeadingZeros(getChunkSizeX() - 1);
    }

    default int getLocalYByteSize() {
        return Integer.SIZE - Integer.numberOfLeadingZeros(getChunkSizeY() - 1);
    }

    default int getLocalZByteSize() {
        return Integer.SIZE - Integer.numberOfLeadingZeros(getChunkSizeZ() - 1);
    }

    WorldHeightMap getWorldHeightMap();

    Chunk getChunkNow(int chunkX, int chunkY, int chunkZ);

    Chunk getChunkNow(long chunkKey);

    ChunkMap getChunkMap();

    default void addChunk(Chunk chunk) {
        getWorldHeightMap().addChunk(chunk);
        for (int i = 0; i < getDelegates().size(); i++) {
            getDelegates().get(i).notifyAddChunk(chunk);
        }
    }

    default void removeChunk(Chunk chunk) {
        getWorldHeightMap().removeChunk(chunk);
        for (int i = 0; i < getDelegates().size(); i++) {
            getDelegates().get(i).notifyRemoveChunk(chunk);
        }
    }

    default void chunkUpdate(Chunk chunk, byte localX, byte localY, byte localZ, boolean wasEmptyBefore) {
        getWorldHeightMap().blockUpdate(chunk);
        for (int i = 0; i < getDelegates().size(); i++) {
            getDelegates().get(i).notifyChunkUpdate(chunk, localX, localY, localZ, wasEmptyBefore);
        }
    }

    default Chunk getChunkNeighborNow(int chunkX, int chunkY, int chunkZ, Direction direction) {
        return getChunkNow(chunkX + direction.getOffsetX(), chunkY + direction.getOffsetY(), chunkZ + direction.getOffsetZ());
    }

    default Chunk getChunkNeighborNow(Chunk chunk, Direction direction) {
        return getChunkNeighborNow(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), direction);
    }

    default boolean hasNeighborsToAllSides(Chunk chunk) {
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            if (getChunkNeighborNow(chunk, direction) == null) {
                return false;
            }
        }
        return true;
    }
}
