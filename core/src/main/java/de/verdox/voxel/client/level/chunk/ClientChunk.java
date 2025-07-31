package de.verdox.voxel.client.level.chunk;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.occupancy.BitsetBasedOccupancyMask;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.util.LODUtil;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ClientChunk extends ChunkBase<ClientWorld> {
    private final ClientWorld world;
    private final int chunkX;
    private final int chunkY;
    private final int chunkZ;

    private final OccupancyMask chunkOccupancyMask = new BitsetBasedOccupancyMask();
    private final BoundingBox boundingBox = new BoundingBox();

    private LODChunk currentUsedLod;

    public ClientChunk(ClientWorld world, int chunkX, int chunkY, int chunkZ) {
        super(world, chunkX, chunkY, chunkZ);
        this.world = world;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }

    @Override
    public void init() {
        this.updateBoundingBox();
        this.chunkOccupancyMask.setOwner(this);
        this.chunkOccupancyMask.initFromOwner();
    }

    @Override
    public void setBlockAt(BlockBase newBlock, int localX, int localY, int localZ) {
        boolean wasEmptyBefore = isEmpty();
        super.setBlockAt(newBlock, localX, localY, localZ);
        this.chunkOccupancyMask.updateOccupancyMask(newBlock, localX, localY, localZ);
        this.getWorld().chunkUpdate(this, (byte) localX, (byte) localY, (byte) localZ, wasEmptyBefore);

/*        for (int i = 0; i < lods.size(); i++) {
            lods.get(i).setBlockAt(newBlock, localX, localY, localZ);
        }*/
    }

    public synchronized LODChunk getLodChunk(int lodLevel) {
        if (lodLevel < 0 || lodLevel > computeMaxLod()) {
            throw new IllegalArgumentException("Lod Level must be between 0 and " + computeMaxLod());
        }
        // Only cache the last used lod chunk
        if (currentUsedLod == null || currentUsedLod.getLodLevel() != lodLevel) {
            currentUsedLod = LODChunk.of(this, lodLevel);
            currentUsedLod.init();
        }
        return currentUsedLod;
    }

    public int computeMaxLod() {
        return LODUtil.getMaxLod(getWorld());
    }

/*    private void initLods(int maxLevel) {
        if (maxLevel <= 0) {
            return;
        }
        this.lods = new ArrayList<>(maxLevel);
        for (int lodLevel = 1; lodLevel <= maxLevel; lodLevel++) {
            LODChunk lodChunk = LODChunk.of(this, lodLevel);
            lodChunk.init();
            lods.add(lodChunk);
        }
    }*/

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
