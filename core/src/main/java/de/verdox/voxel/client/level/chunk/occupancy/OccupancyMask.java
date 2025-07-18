package de.verdox.voxel.client.level.chunk.occupancy;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.level.block.BlockBase;

public interface OccupancyMask {
    boolean isOpaque(int localX, int localY, int localZ);

    void updateOccupancyMask(BlockBase block, int x, int y, int z);

    long getTotalOpaque();

    int getColumnOpaqueCount(int x, int y);

    void initFromChunk(ClientChunk chunk);

    boolean isChunkFullOpaque();

    boolean isChunkEmpty();
}
