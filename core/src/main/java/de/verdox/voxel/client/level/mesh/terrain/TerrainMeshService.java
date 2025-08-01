package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorageImpl;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class TerrainMeshService {
    private final ExecutorService executor = Executors.newFixedThreadPool(4, ThreadUtil.createFactoryForName("Meshing Thread", true));
    private final TerrainManager terrainManager;
    private final ChunkMeshCalculator chunkMeshCalculator;
    private final Long2ObjectMap<TerrainMeshJob> jobs = new Long2ObjectOpenHashMap<>();

    public TerrainMeshService(TerrainManager terrainManager, ChunkMeshCalculator chunkMeshCalculator) {
        this.terrainManager = terrainManager;
        this.chunkMeshCalculator = chunkMeshCalculator;
    }

    public RegionBounds getBounds() {
        return terrainManager.getMeshPipeline().getRegionBounds();
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
            storageOfJob.getRegionalLock().withLock(offsetX, offsetY, offsetZ, 1, () -> {
                computeChunk(chunk, lodLevel);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction direction = Direction.values()[i];
                    ClientChunk neighborChunk = chunk.getWorld().getChunkNeighborNow(chunk, direction);

                    if (neighborChunk == null) {
                        continue;
                    }
                    computeChunk(neighborChunk, lodLevel);
                }
            });

            terrainRegion.getOrCreateMesh().setRawBlockFaces(getStorageOfJob(), true, lodLevel);
        }

        public void updateChunk(ClientChunk chunk) {
            if (skip(chunk)) {
                return;
            }

            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            storageOfJob.getRegionalLock().withLock(offsetX, offsetY, offsetZ, 1, () -> {
                computeChunk(chunk, lodLevel);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction direction = Direction.values()[i];
                    ClientChunk neighborChunk = chunk.getWorld().getChunkNeighborNow(chunk, direction);

                    if (neighborChunk == null) {
                        continue;
                    }
                    computeChunk(neighborChunk, lodLevel);
                }
            });
        }

        public void removeChunk(ClientChunk chunk) {
            if (skip(chunk)) {
                return;
            }
            computedChunks.getAndDecrement();

            //TODO
        }

        private void computeChunk(ClientChunk chunk, int lodLevel) {
            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            TerrainFaceStorage.ChunkFaceStorage chunkFaceStorage = storageOfJob.getOrCreateChunkFaces(offsetX, offsetY, offsetZ);
            chunkMeshCalculator.calculateChunkMesh(chunkFaceStorage, chunk, lodLevel);
        }

        public boolean skip(ClientChunk chunk) {
            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            return chunk.isEmpty() || offsetX < 0 || offsetX >= getBounds().regionSizeX() || offsetY < 0 || offsetY >= getBounds().regionSizeY() || offsetZ < 0 || offsetZ >= getBounds().regionSizeZ();
        }
    }

    private TerrainFaceStorage createStorageForLodLevel(int lodLevel) {
        return new TerrainFaceStorageImpl(terrainManager.getWorld(), (byte) lodLevel);
    }
}
