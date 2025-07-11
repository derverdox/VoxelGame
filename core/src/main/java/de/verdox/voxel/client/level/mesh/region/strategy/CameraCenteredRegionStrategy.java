package de.verdox.voxel.client.level.mesh.region.strategy;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.util.ChunkRenderRegionUtil;
import de.verdox.voxel.shared.util.bitpackedarray.LinearRadialArray;
import de.verdox.voxel.shared.util.bitpackedarray.ThreeDimensionalArray;
import lombok.Getter;

import java.util.List;

public class CameraCenteredRegionStrategy implements ChunkRenderRegionStrategy {
    @Getter
    private final ClientWorld world;

    @Getter
    private int cuboidRegionSteps;
    private ThreeDimensionalArray<RenderRegion> regions;
    @Getter
    private int centerChunkX, centerChunkY, centerChunkZ;

    public static long sumTime = 0;
    public static long samples = 1;
    @Getter
    private int minViewChunkX, minViewChunkY, minViewChunkZ, maxViewChunkX, maxViewChunkY, maxViewChunkZ;

    public CameraCenteredRegionStrategy(ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ) {
        this.world = world;
        changeViewDistance(viewDistanceX, viewDistanceY, viewDistanceZ);
        rebuildRegions(world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ(), 0, 0, 0);
    }

    @Override
    public void changeViewDistance(int viewDistanceX, int viewDistanceY, int viewDistanceZ) {
        this.cuboidRegionSteps = ChunkRenderRegionUtil.getRegionsCoveredOnDistanceToCenter(Math.max(viewDistanceX, Math.max(viewDistanceY, viewDistanceZ)));

        this.regions = new LinearRadialArray<>(this.cuboidRegionSteps, this.cuboidRegionSteps, this.cuboidRegionSteps, RenderRegion[]::new);
    }

    @Override
    public void chunkLoad(ClientChunk chunk) {

    }

    @Override
    public void chunkUnload(ClientChunk chunk) {

    }

    @Override
    public int getAmountOfRegions() {
        return regions.size();
    }

    @Override
    public RenderRegion getRegionOfChunk(int chunkX, int chunkY, int chunkZ) {
        int[] region = new int[3];
        ChunkRenderRegionUtil.findRegion(centerChunkX, centerChunkY, centerChunkZ, chunkX, chunkY, chunkZ, region);
        if (!regions.isInBounds(region[0], region[1], region[2])) {
            return null;
        }
        return regions.get(region[0], region[1], region[2]);
    }

    @Override
    public void onChangeCameraCenter(int chunkSizeX, int chunkSizeY, int chunkSizeZ, int centerChunkX, int centerChunkY, int centerChunkZ) {
        rebuildRegions(chunkSizeX, chunkSizeY, chunkSizeZ, centerChunkX, centerChunkY, centerChunkZ);
    }

    @Override
    public void filterRegionsInFrustum(Camera camera, ClientWorld world, List<RenderRegion> result) {
        int maxLevel = getCuboidRegionSteps();

        while (maxLevel > 0) {
            for (int regionIndexX = -maxLevel; regionIndexX <= maxLevel; regionIndexX += maxLevel) {
                for (int regionIndexY = -maxLevel; regionIndexY <= maxLevel; regionIndexY += maxLevel) {
                    for (int regionIndexZ = -maxLevel; regionIndexZ <= maxLevel; regionIndexZ += maxLevel) {
                        CameraCenteredRegionStrategy.RenderRegion renderRegion = regions.get(regionIndexX, regionIndexY, regionIndexZ);

                        if (!camera.frustum.boundsInFrustum(renderRegion.getBoundingBox())) {
                            //TODO: If we cannot see the large region we might skip other ones aswell?
                            continue;
                        }
                        result.add(renderRegion);
                    }
                }
            }
            maxLevel--;
        }
        result.add(regions.get(0, 0, 0));
    }

    public void rebuildRegions(int chunkSizeX, int chunkSizeY, int chunkSizeZ, int centerChunkX, int centerChunkY, int centerChunkZ) {
        int maxLevel = this.cuboidRegionSteps;

        this.centerChunkX = centerChunkX;
        this.centerChunkY = centerChunkY;
        this.centerChunkZ = centerChunkZ;

        int horizontalViewDistance = ClientBase.clientSettings.horizontalViewDistance;
        int verticalViewDistance = ClientBase.clientSettings.verticalViewDistance;

        this.minViewChunkX = centerChunkX - horizontalViewDistance;
        this.minViewChunkY = centerChunkY - verticalViewDistance;
        this.minViewChunkZ = centerChunkZ - horizontalViewDistance;

        this.maxViewChunkX = centerChunkX + horizontalViewDistance;
        this.maxViewChunkY = centerChunkY + verticalViewDistance;
        this.maxViewChunkZ = centerChunkZ + horizontalViewDistance;

        while (maxLevel > 0) {
            int sizeOfCube = ChunkRenderRegionUtil.getRegionSizeForLevel(maxLevel);
            for (int regionIndexX = -maxLevel; regionIndexX <= maxLevel; regionIndexX += maxLevel) {
                int minChunkX = ChunkRenderRegionUtil.computeMinChunkForRegion(centerChunkX, regionIndexX, maxLevel);
                int maxChunkX = minChunkX + sizeOfCube - 1;

                for (int regionIndexY = -maxLevel; regionIndexY <= maxLevel; regionIndexY += maxLevel) {
                    int minChunkY = ChunkRenderRegionUtil.computeMinChunkForRegion(centerChunkY, regionIndexY, maxLevel);
                    int maxChunkY = minChunkY + sizeOfCube - 1;
                    for (int regionIndexZ = -maxLevel; regionIndexZ <= maxLevel; regionIndexZ += maxLevel) {
                        int minChunkZ = ChunkRenderRegionUtil.computeMinChunkForRegion(centerChunkZ, regionIndexZ, maxLevel);
                        int maxChunkZ = minChunkZ + sizeOfCube - 1;

                        RenderRegion renderRegion = regions.get(regionIndexX, regionIndexY, regionIndexZ);
                        if (renderRegion == null) {
                            renderRegion = new RenderRegion(this);
                            regions.set(renderRegion, regionIndexX, regionIndexY, regionIndexZ);
                        }

                        int level = maxLevel;

                        if (regionIndexX == 0 && regionIndexY == 0 && regionIndexZ == 0) {
                            level = 0;
                        }
                        renderRegion.rebuildRegionGeometry(chunkSizeX, chunkSizeY, chunkSizeZ, minChunkX, minChunkY, minChunkZ, maxChunkX, maxChunkY, maxChunkZ, level);
                    }
                }
            }
            maxLevel--;
        }
    }
}
