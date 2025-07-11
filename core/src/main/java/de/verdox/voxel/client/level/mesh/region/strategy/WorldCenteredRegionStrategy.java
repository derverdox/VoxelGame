package de.verdox.voxel.client.level.mesh.region.strategy;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class WorldCenteredRegionStrategy implements ChunkRenderRegionStrategy {
    private final ClientWorld world;
    private final int viewDistanceX, viewDistanceY, viewDistanceZ;
    private int regionScaleX, regionScaleY, regionScaleZ;
    private int centerChunkX, centerChunkY, centerChunkZ;
    private int minViewChunkX, minViewChunkY, minViewChunkZ, maxViewChunkX, maxViewChunkY, maxViewChunkZ;

    private final Map<Long, RenderRegion> regions = new HashMap<>();

    public WorldCenteredRegionStrategy(ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ) {
        this.world = world;
        this.viewDistanceX = viewDistanceX;
        this.viewDistanceY = viewDistanceY;
        this.viewDistanceZ = viewDistanceZ;
        changeViewDistance(viewDistanceX, viewDistanceY, viewDistanceZ);
    }

    @Override
    public void changeViewDistance(int viewDistanceX, int viewDistanceY, int viewDistanceZ) {
        regionScaleX = Math.max(1, viewDistanceX / 4);
        regionScaleY = Math.max(1, viewDistanceY / 4);
        regionScaleZ = Math.max(1, viewDistanceZ / 4);
        System.out.println("Region scale: [" + regionScaleX + ", " + regionScaleY + ", " + regionScaleZ + "]");
    }

    @Override
    public void chunkLoad(ClientChunk chunk) {
        getRegionOfChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ()).setDirty(true);
    }

    @Override
    public void chunkUnload(ClientChunk chunk) {
        getRegionOfChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ()).setDirty(true);
    }

    @Override
    public int getAmountOfRegions() {
        return regions.size();
    }

    @Override
    public RenderRegion getRegionOfChunk(int chunkX, int chunkY, int chunkZ) {
        int regionX = Math.floorDiv(chunkX, regionScaleX);
        int regionY = Math.floorDiv(chunkY, regionScaleY);
        int regionZ = Math.floorDiv(chunkZ, regionScaleZ);

        long regionKey = ChunkBase.computeChunkKey(regionX, regionY, regionZ);
        return regions.computeIfAbsent(regionKey, aLong -> {
            RenderRegion renderRegion = new RenderRegion(this);

            int minChunkX = regionX * regionScaleX;
            int minChunkY = regionY * regionScaleY;
            int minChunkZ = regionZ * regionScaleZ;

            int maxChunkX = minChunkX + regionScaleX - 1;
            int maxChunkY = minChunkY + regionScaleY - 1;
            int maxChunkZ = minChunkZ + regionScaleZ - 1;

            //System.out.println("Put chunk [" + chunkX + ", " + chunkY + ", " + chunkZ + "] to region [" + regionX + ", " + regionY + ", " + regionZ + "] with minChunk [" + minChunkX + ", " + minChunkY + ", " + minChunkZ + "] and maxChunk [" + maxChunkX + ", " + maxChunkY + ", " + maxChunkZ + "]");

            renderRegion.rebuildRegionGeometry(world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ(), minChunkX, minChunkY, minChunkZ, maxChunkX, maxChunkY, maxChunkZ, 3);
            return renderRegion;
        });
    }

    @Override
    public void onChangeCameraCenter(int chunkSizeX, int chunkSizeY, int chunkSizeZ, int centerChunkX, int centerChunkY, int centerChunkZ) {
        this.centerChunkX = centerChunkX;
        this.centerChunkY = centerChunkY;
        this.centerChunkZ = centerChunkZ;

        this.minViewChunkX = this.centerChunkX - (viewDistanceX);
        this.minViewChunkY = this.centerChunkY - (viewDistanceY);
        this.minViewChunkZ = this.centerChunkZ - (viewDistanceZ);

        this.maxViewChunkX = this.centerChunkX + (viewDistanceX);
        this.maxViewChunkY = this.centerChunkY + (viewDistanceY);
        this.maxViewChunkZ = this.centerChunkZ + (viewDistanceZ);
    }

    @Override
    public void filterRegionsInFrustum(Camera camera, ClientWorld world, List<RenderRegion> result) {
        RenderRegion center = getRegionOfChunk(centerChunkX, centerChunkY, centerChunkZ);
        if (center != null) {
            result.add(center);
        }
        for (int x = minViewChunkX; x <= maxViewChunkX; x += regionScaleX) {
            for (int y = minViewChunkY; y <= maxViewChunkY; y += regionScaleY) {
                for (int z = minViewChunkZ; z <= maxViewChunkZ; z += regionScaleZ) {

                    RenderRegion renderRegion = getRegionOfChunk(x, y, z);
                    if (renderRegion == null || !camera.frustum.boundsInFrustum(renderRegion.getBoundingBox())) {
                        continue;
                    }
                    result.add(renderRegion);
                }
            }
        }
    }
}
