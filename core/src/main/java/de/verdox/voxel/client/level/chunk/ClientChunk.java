package de.verdox.voxel.client.level.chunk;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.palette.ChunkBlockPalette;
import lombok.Getter;

@Getter
public class ClientChunk extends ChunkBase<ClientWorld> {
    private final ChunkOccupancyMask occupancyMask;
    private final BoundingBox boundingBox = new BoundingBox();

    public ClientChunk(ClientWorld world, int chunkX, int chunkY, int chunkZ) {
        super(world, chunkX, chunkY, chunkZ);
        this.occupancyMask = new ChunkOccupancyMask(this, world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ());
        this.occupancyMask.initOccupancyMask();
        this.updateBoundingBox();
    }

    public ClientChunk(ClientWorld world, int chunkX, int chunkY, int chunkZ, ChunkBlockPalette chunkBlockPalette, byte[][] heightmap, byte[][] depthMap) {
        super(world, chunkX, chunkY, chunkZ, chunkBlockPalette, heightmap, depthMap);
        this.occupancyMask = new ChunkOccupancyMask(this, world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ());
        this.occupancyMask.initOccupancyMask();
        this.updateBoundingBox();
    }

    @Override
    public void setBlockAt(BlockBase newBlock, int localX, int localY, int localZ) {
        super.setBlockAt(newBlock, localX, localY, localZ);
        this.occupancyMask.updateOccupancyMask(newBlock, localX, localY, localZ);
        this.getWorld().chunkUpdate(this, (byte) localX, (byte) localY, (byte) localZ);
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
