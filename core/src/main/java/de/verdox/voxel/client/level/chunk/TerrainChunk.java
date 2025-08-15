package de.verdox.voxel.client.level.chunk;

import de.verdox.voxel.client.level.chunk.occupancy.BitsetBasedOccupancyMask;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.level.chunk.proto.ChunkProtoMesh;
import de.verdox.voxel.client.level.mesh.TerrainManager;
import de.verdox.voxel.shared.util.lod.LODUtil;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.chunk.DelegateChunk;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

public class TerrainChunk extends DelegateChunk implements RenderableChunk {
    @Getter
    private final OccupancyMask chunkOccupancyMask = new BitsetBasedOccupancyMask();
    @Getter
    private final TerrainManager terrainManager;
    @Getter
    private LODChunk currentUsedLod;
    @Getter
    private final ChunkProtoMesh chunkProtoMesh;

    private final LODChunk[] lods;

    private final ChunkLODPyramid chunkLODPyramid;

    public TerrainChunk(TerrainManager terrainManager, Chunk owner) {
        super(owner);
        this.terrainManager = terrainManager;
        this.chunkOccupancyMask.setOwner(this);
        this.chunkOccupancyMask.initFromOwner();
        this.chunkProtoMesh = new ChunkProtoMesh(this);
        this.chunkLODPyramid = new ChunkLODPyramid(this);

        this.lods = new LODChunk[LODUtil.getMaxLod(getWorld())];
    }

    public synchronized LODChunk getLodChunk(int lodLevel) {
/*        int maxLod = LODUtil.getMaxLod(getWorld());
        if (lodLevel < 0 || lodLevel > maxLod) {
            throw new IllegalArgumentException("Lod Level must be between 0 and " + maxLod);
        }
        // Only cache the last used lod chunk
        if (currentUsedLod == null || currentUsedLod.getLodLevel() != lodLevel) {
            currentUsedLod = LODChunk.of(this, lodLevel);
            currentUsedLod.init();
        }*/
        return lods[lodLevel - 1];
    }

    @Override
    public <SELF extends Chunk> SELF getNeighborChunk(Direction direction) {
        return (SELF) terrainManager.getChunkNow(owner.getChunkX() + direction.getOffsetX(), owner.getChunkY() + direction.getOffsetY(), owner.getChunkZ() + direction.getOffsetZ());
    }

    @Override
    public boolean hasNeighborsToAllSides() {
        return terrainManager.getWorld().hasNeighborsToAllSides(this);
    }

    @Override
    public void notifySetBlock(BlockBase newBlock, int localX, int localY, int localZ) {
        this.chunkOccupancyMask.updateOccupancyMask(newBlock, localX, localY, localZ);
        this.chunkLODPyramid.blockChange(newBlock, localX, localY, localZ);
    }
}
