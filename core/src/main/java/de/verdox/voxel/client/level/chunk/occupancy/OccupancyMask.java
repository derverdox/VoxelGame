package de.verdox.voxel.client.level.chunk.occupancy;

import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.ChunkData;

public interface OccupancyMask extends ChunkData<ChunkBase<?>> {
    boolean isOpaque(int localX, int localY, int localZ);

    void updateOccupancyMask(BlockBase block, int x, int y, int z);

    long getTotalOpaque();

    void initFromOwner();

    boolean isChunkFullOpaque();

    boolean isChunkEmpty();

    long getZColumn(int x, int y);

    default long getLodColumn(int x, int y, int stepSize, int sz) {
        long base = getZColumn(x, y);
        if (stepSize == 1) {
            return base;
        }
        long lod = 0L;
        for (int bit = 0, shift = 0; bit < sz; bit += stepSize, shift++) {
            long segmentMask = (((1L << stepSize) - 1L) << bit);
            if ((base & segmentMask) != 0L) {
                lod |= (1L << shift);
            }
        }
        return lod;
    }

    long getSideMask();
}
