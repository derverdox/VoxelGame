package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorageImpl;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.ThreadUtil;
import de.verdox.voxel.shared.util.buffer.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TerrainMeshService implements DebuggableOnScreen {
    private final ExecutorService executor = Executors.newFixedThreadPool(1, ThreadUtil.createFactoryForName("Meshing Thread", true));
    private final TerrainManager terrainManager;
    private final ChunkMeshCalculator chunkMeshCalculator;
    private final Long2ObjectMap<TerrainMeshJob> jobs = new Long2ObjectOpenHashMap<>();

    public TerrainMeshService(TerrainManager terrainManager, ChunkMeshCalculator chunkMeshCalculator) {
        this.terrainManager = terrainManager;
        this.chunkMeshCalculator = chunkMeshCalculator;

        if (ClientBase.clientRenderer != null) {
            ClientBase.clientRenderer.getDebugScreen().attach(this);
        }
    }

    public RegionBounds getBounds() {
        return terrainManager.getBounds();
    }

    public void createChunkMesh(TerrainRegion terrainRegion, TerrainChunk chunk) {
        long regionKey = terrainRegion.getRegionKey();

        int regionLodLevel = terrainManager.computeLodLevel(terrainManager.getCenterRegionX(), terrainManager.getCenterRegionY(), terrainManager.getCenterRegionZ(), terrainRegion.getRegionX(), terrainRegion.getRegionY(), terrainRegion.getRegionZ());

        if (!jobs.containsKey(regionKey) || jobs.get(regionKey).lodLevel != regionLodLevel) {
            jobs.put(regionKey, new TerrainMeshJob(terrainRegion, regionLodLevel));
        }

        TerrainMeshJob terrainMeshJob = jobs.get(regionKey);
        terrainMeshJob.addChunk(chunk);
    }

    public void removeChunkMesh(TerrainRegion terrainRegion, TerrainChunk chunk) {

    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine(ChunkMeshCalculator.chunkCalculatorThroughput.format());
    }

    @Getter
    private class TerrainMeshJob {
        private final TerrainFaceStorage storageOfJob;
        private final TerrainMeshBuffer terrainMeshBuffer = new TerrainMeshBuffer(this);
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

        public void addChunk(TerrainChunk chunk) {
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
                //terrainMeshBuffer.addChunkMesh((byte) lodLevel, storageOfJob.getOrCreateChunkFaces(offsetX, offsetY, offsetZ), offsetX, offsetY, offsetZ);
            });
        }

        public void updateChunk(TerrainChunk chunk) {
            terrainRegion.checkIfChunkInRegion(chunk);
            if (skip(chunk)) {
                return;
            }

            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            recomputeChunksAndNeighbors(chunk, offsetX, offsetY, offsetZ);
        }

        public void removeChunk(TerrainChunk chunk) {
            terrainRegion.checkIfChunkInRegion(chunk);
            if (skip(chunk)) {
                return;
            }
            computedChunks.getAndDecrement();

            //TODO
        }

        private void recomputeChunksAndNeighbors(TerrainChunk chunk, int offsetX, int offsetY, int offsetZ) {

            storageOfJob.getRegionalLock().withLock(offsetX, offsetY, offsetZ, 1, () -> {
                computeChunk(chunk, lodLevel);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction direction = Direction.values()[i];
                    TerrainChunk neighborChunk = (TerrainChunk) chunk.getNeighborChunk(direction);

                    if (neighborChunk == null || skip(neighborChunk)) {
                        continue;
                    }
                    computeChunk(neighborChunk, lodLevel);
                }
            });
        }

        private void computeChunk(TerrainChunk chunk, int lodLevel) {
            int offsetX = (byte) getBounds().getOffsetX(chunk.getChunkX());
            int offsetY = (byte) getBounds().getOffsetY(chunk.getChunkY());
            int offsetZ = (byte) getBounds().getOffsetZ(chunk.getChunkZ());

            TerrainFaceStorage.ChunkFaceStorage chunkFaceStorage = storageOfJob.getOrCreateChunkFaces(offsetX, offsetY, offsetZ);
            chunkMeshCalculator.calculateChunkMesh(chunkFaceStorage, chunk, lodLevel);
        }

        public boolean skip(TerrainChunk chunk) {
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

            if (!chunk.hasNeighborsToAllSides()) {
                return true;
            }

            return offsetX < 0 || offsetX >= getBounds().regionSizeX() || offsetY < 0 || offsetY >= getBounds().regionSizeY() || offsetZ < 0 || offsetZ >= getBounds().regionSizeZ();
        }
    }

    private static class TerrainMeshBuffer {
        @Getter
        private final DynamicFloatBuffer vertexBuffer = new PlainDynamicFloatBuffer(1, true);
        @Getter
        private final DynamicIntBuffer indexBuffer = new PlainDynamicIntBuffer(1, true);
        private final Long2ObjectMap<BufferRange> bufferRanges = new Long2ObjectOpenHashMap<>();
        private final TerrainMeshJob parent;

        public TerrainMeshBuffer(TerrainMeshJob parent) {
            this.parent = parent;
        }

        public synchronized void addChunkMesh(byte lodLevel, TerrainFaceStorage.ChunkFaceStorage chunkFaces, int offsetXInRegion, int offsetYInRegion, int offsetZInRegion) {
            if (chunkFaces.isEmpty()) {
                return;
            }

            float[] vertices = new float[chunkFaces.getAmountFloats()];
            int[] indices = new int[chunkFaces.getAmountIndices()];

            AtomicInteger vertexOffset = new AtomicInteger();
            AtomicInteger indexOffset = new AtomicInteger();
            AtomicInteger baseVertex = new AtomicInteger();
            AtomicInteger amountVertices = new AtomicInteger();

            int offsetXInBlocks = offsetXInRegion * parent.getTerrainRegion().getTerrainManager().getWorld().getChunkSizeX();
            int offsetYInBlocks = offsetYInRegion * parent.getTerrainRegion().getTerrainManager().getWorld().getChunkSizeY();
            int offsetZInBlocks = offsetZInRegion * parent.getTerrainRegion().getTerrainManager().getWorld().getChunkSizeZ();

            chunkFaces.forEachFace((face, localX, localY, localZ) -> {
                face.appendToBuffers(vertices, null, indices, vertexOffset.get(), indexOffset.get(), baseVertex.get(), TextureAtlasManager.getInstance().getBlockTextureAtlas(), lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);
                vertexOffset.addAndGet(face.getVerticesPerFace() * face.getFloatsPerVertex());
                indexOffset.addAndGet(face.getIndicesPerFace());
                baseVertex.addAndGet(face.getVerticesPerFace());
                amountVertices.addAndGet(face.getVerticesPerFace());
            });

            long offsetKey = Chunk.computeChunkKey(offsetXInRegion, offsetYInRegion, offsetZInRegion);

            // Replace data in buffer
            if (bufferRanges.containsKey(offsetKey)) {
                // REPLACE OLD DATA
                BufferRange bufferRange = bufferRanges.get(offsetKey);

                int vertexStartNew = bufferRange.vertexStart;
                int vertexEndNew = vertexBuffer.update(bufferRange.vertexStart, bufferRange.vertexEndExclusive, vertices);
                int indexStartNew = bufferRange.indexStart;
                int indexEndNew = indexBuffer.update(bufferRange.indexStart, bufferRange.indexEndExclusive, indices);
                bufferRanges.put(offsetKey, new BufferRange(vertexStartNew, vertexEndNew, indexStartNew, indexEndNew));
            }
            // Insert data into buffer
            else {
                int vertexStart = vertexBuffer.size();
                int vertexEnd = vertexStart + vertices.length;
                vertexBuffer.insert(vertexBuffer.size(), vertices);
                int indexStart = indexBuffer.size();
                int indexEnd = indexStart + indices.length;
                indexBuffer.insert(indexBuffer.size(), indices);
                BufferRange bufferRange = new BufferRange(vertexStart, vertexEnd, indexStart, indexEnd);
                bufferRanges.put(offsetKey, bufferRange);
            }
        }

        private record BufferRange(int vertexStart, int vertexEndExclusive, int indexStart, int indexEndExclusive) {
        }
    }

    private TerrainFaceStorage createStorageForLodLevel(int lodLevel) {
        return new TerrainFaceStorageImpl(terrainManager, (byte) lodLevel);
    }
}
