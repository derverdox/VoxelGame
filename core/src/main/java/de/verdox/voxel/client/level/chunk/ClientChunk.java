package de.verdox.voxel.client.level.chunk;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.occupancy.BitsetBasedOccupancyMask;
import de.verdox.voxel.client.level.chunk.occupancy.NaiveChunkOccupancyMask;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.DepthMap;
import de.verdox.voxel.shared.level.chunk.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.util.palette.ChunkBlockPalette;
import lombok.Getter;

@Getter
public class ClientChunk extends ChunkBase<ClientWorld> {
    private final OccupancyMask chunkOccupancyMask;
    private final BoundingBox boundingBox = new BoundingBox();

    public ClientChunk(ClientWorld world, int chunkX, int chunkY, int chunkZ) {
        super(world, chunkX, chunkY, chunkZ);
        this.updateBoundingBox();
        this.chunkOccupancyMask = new BitsetBasedOccupancyMask();
        this.chunkOccupancyMask.initFromChunk(this);
    }

    public ClientChunk(ClientWorld world, int chunkX, int chunkY, int chunkZ, ChunkBlockPalette chunkBlockPalette, HeightMap heightMap, DepthMap depthMap, ChunkLightData chunkLightData) {
        super(world, chunkX, chunkY, chunkZ, chunkBlockPalette, heightMap, depthMap, chunkLightData);
        this.updateBoundingBox();
        this.chunkOccupancyMask = new BitsetBasedOccupancyMask();
        this.chunkOccupancyMask.initFromChunk(this);
    }

    @Override
    public void setBlockAt(BlockBase newBlock, int localX, int localY, int localZ) {
        boolean wasEmptyBefore = isEmpty();
        super.setBlockAt(newBlock, localX, localY, localZ);
        this.chunkOccupancyMask.updateOccupancyMask(newBlock, localX, localY, localZ);
        this.getWorld().chunkUpdate(this, (byte) localX, (byte) localY, (byte) localZ, wasEmptyBefore);
    }

    private void updateBoundingBox() {
        float sizeX = getWorld().getChunkSizeX();
        float sizeY = getWorld().getChunkSizeY();
        float sizeZ = getWorld().getChunkSizeZ();

        float minX = getChunkX() * sizeX;
        float minY = getChunkY() * sizeY;
        float minZ = getChunkZ() * sizeZ;
        float maxX = minX + sizeX;
        float maxY = minY + sizeY;
        float maxZ = minZ + sizeZ;

        boundingBox.set(new Vector3(minX, minY, minZ), new Vector3(maxX, maxY, maxZ));
    }
}
