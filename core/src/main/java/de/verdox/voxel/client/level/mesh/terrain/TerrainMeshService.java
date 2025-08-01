package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorageImpl;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.client.renderer.ClientRenderer;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class TerrainMeshService implements DebuggableOnScreen {
    private final ExecutorService executor = Executors.newFixedThreadPool(1, ThreadUtil.createFactoryForName("Meshing Thread", true));
    private final TerrainManager terrainManager;
    private final ChunkMeshCalculator chunkMeshCalculator;
    private final Long2ObjectMap<TerrainMeshJob> jobs = new Long2ObjectOpenHashMap<>();

    public TerrainMeshService(TerrainManager terrainManager, ChunkMeshCalculator chunkMeshCalculator) {
        this.terrainManager = terrainManager;
        this.chunkMeshCalculator = chunkMeshCalculator;

        if(ClientBase.clientRenderer != null) {
            ClientBase.clientRenderer.getDebugScreen().attach(this);
        }
    }

    public RegionBounds getBounds() {
        return terrainManager.getBounds();
    }

    public void createChunkMesh(TerrainRegion terrainRegion, ClientChunk chunk) {
        long regionKey = terrainRegion.getRegionKey();

        int regionLodLevel = chunk.getWorld()
                                  .computeLodLevel(terrainManager.getCenterRegionX(), terrainManager.getCenterRegionY(), terrainManager.getCenterRegionZ(), terrainRegion.getRegionX(), terrainRegion.getRegionY(), terrainRegion.getRegionZ());

        if (!jobs.containsKey(regionKey) || jobs.get(regionKey).lodLevel != regionLodLevel) {
            jobs.put(regionKey, new TerrainMeshJob(terrainRegion, regionLodLevel));
        }

        TerrainMeshJob terrainMeshJob = jobs.get(regionKey);
        terrainMeshJob.addChunk(chunk);
    }

    public void removeChunkMesh(TerrainRegion terrainRegion, ClientChunk chunk) {

    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine(ChunkMeshCalculator.chunkCalculatorThroughput.format());
    }

    @Getter
    private class TerrainMeshJob {
        private final TerrainFaceStorage storageOfJob;
        private final TerrainRegion terrainRegion;
        private final int lodLevel;
        private final AtomicLong computedChunks = new AtomicLong();

        public TerrainMeshJob(TerrainRegion terrainRegion, int lodLevel) {
            this.terrainRegion = terrainRegion;
            this.lodLevel = lodLevel;
            this.storageOfJob = createStorageForLodLevel(lodLevel);
        }

        public boolean isDone() {
            return computedChunks.get() >= (long) getBounds().regionSizeX() * getBounds().regionSizeY() * getBounds().regionSizeZ();
        }

        public void addChunk(ClientChunk chunk) {
            terrainRegion.checkIfChunkInRegion(chunk);
            if (skip(chunk)) {
                return;
            }


            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            if (storageOfJob.hasFacesForChunk(offsetX, offsetY, offsetZ)) {
                return;
            }

            computedChunks.getAndIncrement();

            executor.submit(() -> {
                recomputeChunksAndNeighbors(chunk, offsetX, offsetY, offsetZ);
                terrainRegion.getOrCreateMesh().setRawBlockFaces(getStorageOfJob(), true, lodLevel);
            });
        }

        public void updateChunk(ClientChunk chunk) {
            terrainRegion.checkIfChunkInRegion(chunk);
            if (skip(chunk)) {
                return;
            }

            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            recomputeChunksAndNeighbors(chunk, offsetX, offsetY, offsetZ);
        }

        public void removeChunk(ClientChunk chunk) {
            terrainRegion.checkIfChunkInRegion(chunk);
            if (skip(chunk)) {
                return;
            }
            computedChunks.getAndDecrement();

            //TODO
        }

        private void recomputeChunksAndNeighbors(ClientChunk chunk, int offsetX, int offsetY, int offsetZ) {

            storageOfJob.getRegionalLock().withLock(offsetX, offsetY, offsetZ, 1, () -> {
                computeChunk(chunk, lodLevel);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction direction = Direction.values()[i];
                    ClientChunk neighborChunk = chunk.getWorld().getChunkNeighborNow(chunk, direction);

                    if (neighborChunk == null || skip(neighborChunk)) {
                        continue;
                    }
                    computeChunk(neighborChunk, lodLevel);
                }
            });
        }

        private void computeChunk(ClientChunk chunk, int lodLevel) {
            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            TerrainFaceStorage.ChunkFaceStorage chunkFaceStorage = storageOfJob.getOrCreateChunkFaces(offsetX, offsetY, offsetZ);
            chunkMeshCalculator.calculateChunkMesh(chunkFaceStorage, chunk, lodLevel);
        }

        public boolean skip(ClientChunk chunk) {
            if (chunk.isEmpty()) {
                return true;
            }
            int regionX = getBounds().getRegionX(chunk.getChunkX());
            int regionY = getBounds().getRegionY(chunk.getChunkY());
            int regionZ = getBounds().getRegionZ(chunk.getChunkZ());

            if (regionX != terrainRegion.getRegionX() || regionY != terrainRegion.getRegionY() || regionZ != terrainRegion.getRegionZ()) {
                return true;
            }

            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            if (!chunk.getWorld().hasNeighborsToAllSides(chunk)) {
                return true;
            }

            return offsetX < 0 || offsetX >= getBounds().regionSizeX() || offsetY < 0 || offsetY >= getBounds().regionSizeY() || offsetZ < 0 || offsetZ >= getBounds().regionSizeZ();
        }
    }

    private TerrainFaceStorage createStorageForLodLevel(int lodLevel) {
        return new TerrainFaceStorageImpl(terrainManager.getWorld(), (byte) lodLevel);
    }
}
