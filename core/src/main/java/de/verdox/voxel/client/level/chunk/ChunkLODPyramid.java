package de.verdox.voxel.client.level.chunk;

import de.verdox.voxel.client.level.chunk.occupancy.BitsetBasedOccupancyMask;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.util.lod.LODUtil;

public class ChunkLODPyramid {
    private final TerrainChunk parent;

    private BitsetBasedOccupancyMask[] masksForLevels;

    public ChunkLODPyramid(TerrainChunk parent) {
        this.parent = parent;
        int maxLODLevel = LODUtil.getMaxLod(parent.getWorld());

        this.masksForLevels = new BitsetBasedOccupancyMask[maxLODLevel];
    }

    public void blockChange(BlockBase newBlock, int localX, int localY, int localZ) {

    }

}
