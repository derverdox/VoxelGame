package de.verdox.voxel.client.renderer.classic;

import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.mesh.calculation.chunk.ChunkMeshCalculator;
import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.level.mesh.TerrainManager;
import de.verdox.voxel.client.level.mesh.TerrainRegion;
import de.verdox.voxel.client.level.mesh.calculation.region.RegionMeshCalculator;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.ThreadUtil;
import de.verdox.voxel.shared.util.concurrent.CoalescingScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class TerrainMeshService implements DebuggableOnScreen {
    public static final Logger LOGGER = Logger.getLogger(TerrainMeshService.class.getSimpleName());
    private final ExecutorService chunkMeshing = Executors.newFixedThreadPool(4, ThreadUtil.createFactoryForName("Chunk Meshing Thread", true));
    private final ExecutorService regionMeshing = Executors.newFixedThreadPool(4, ThreadUtil.createFactoryForName("Region Meshing Thread", true));
    private final TerrainManager terrainManager;
    private final RegionMeshCalculator regionMeshCalculator;
    private final ChunkMeshCalculator chunkMeshCalculator;

    private final CoalescingScheduler<Long> chunkSched;
    private final CoalescingScheduler<Long> regionSched;

    public TerrainMeshService(TerrainManager terrainManager, RegionMeshCalculator regionMeshCalculator, ChunkMeshCalculator chunkMeshCalculator) {
        this.terrainManager = terrainManager;
        this.regionMeshCalculator = regionMeshCalculator;
        this.chunkMeshCalculator = chunkMeshCalculator;

        this.chunkSched = new CoalescingScheduler<>(chunkMeshing);
        this.regionSched = new CoalescingScheduler<>(regionMeshing);

        if (ClientBase.clientRenderer != null) {
            ClientBase.clientRenderer.getDebugScreen().attach(this);
        }
    }

    public RegionBounds getBounds() {
        return terrainManager.getBounds();
    }


    /**
     * Entry: markiert einen Chunk als "dirty" und stößt Remeshing an.
     */
    public void createChunkMesh(TerrainRegion region, TerrainChunk chunk) {
        if (skip(chunk)) return;

        long ck = chunk.getChunkKey();
        long rk = region.getRegionKey();

        chunkSched.request(ck, () -> {
            int lod = terrainManager.computeLodLevel(
                    terrainManager.getCenterRegionX(), terrainManager.getCenterRegionY(), terrainManager.getCenterRegionZ(),
                    region.getRegionX(), region.getRegionY(), region.getRegionZ()
            );
            chunkMeshCalculator.calculateChunkMesh(chunk, lod);

            regionSched.request(rk, () -> {
                int regionLod = terrainManager.computeLodLevel(
                        terrainManager.getCenterRegionX(), terrainManager.getCenterRegionY(), terrainManager.getCenterRegionZ(),
                        region.getRegionX(), region.getRegionY(), region.getRegionZ()
                );
                regionMeshCalculator.updateTerrainMesh(region, regionLod);
            });
        });
    }

    public void removeChunkMesh(TerrainRegion region, TerrainChunk chunk) {
        regionSched.request(region.getRegionKey(), () -> {
            int lod = terrainManager.computeLodLevel(
                    terrainManager.getCenterRegionX(), terrainManager.getCenterRegionY(), terrainManager.getCenterRegionZ(),
                    region.getRegionX(), region.getRegionY(), region.getRegionZ()
            );
            regionMeshCalculator.updateTerrainMesh(region, lod);
        });
    }

    public void updateChunkMesh(TerrainRegion region, TerrainChunk chunk) {
        createChunkMesh(region, chunk);
    }

    private boolean skip(TerrainChunk chunk) {
        return chunk.isEmpty() || !chunk.hasNeighborsToAllSides();
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine(ChunkMeshCalculator.chunkCalculatorThroughput.format());
        debugScreen.addDebugTextLine(RegionMeshCalculator.regionCalculatorThroughput.format());
        debugScreen.addDebugTextLine(chunkSched.metrics("Chunks") + " | " + regionSched.metrics("Regions"));
    }
}
