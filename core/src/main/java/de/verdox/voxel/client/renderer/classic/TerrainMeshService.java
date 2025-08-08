package de.verdox.voxel.client.renderer.classic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorageImpl;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.client.level.mesh.proto.ProtoMask;
import de.verdox.voxel.client.level.mesh.terrain.RenderableChunk;
import de.verdox.voxel.client.level.mesh.terrain.TerrainChunk;
import de.verdox.voxel.client.level.mesh.terrain.TerrainManager;
import de.verdox.voxel.client.level.mesh.terrain.TerrainRegion;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.ThreadUtil;
import de.verdox.voxel.shared.util.buffer.*;
import gaiasky.util.gdx.mesh.IntMesh;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import lombok.Getter;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class TerrainMeshService implements DebuggableOnScreen {
    public static final Logger LOGGER = Logger.getLogger(TerrainMeshService.class.getSimpleName());
    private final ExecutorService executor = Executors.newFixedThreadPool(1, ThreadUtil.createFactoryForName("Meshing Thread", true));
    private final TerrainManager terrainManager;
    private final ChunkMeshCalculator chunkMeshCalculator;

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
        int regionLodLevel = terrainManager.computeLodLevel(terrainManager.getCenterRegionX(), terrainManager.getCenterRegionY(), terrainManager.getCenterRegionZ(), terrainRegion.getRegionX(), terrainRegion.getRegionY(), terrainRegion.getRegionZ());

        if (skip(chunk)) {
            return;
        }

        executor.execute(() -> {
            chunkMeshCalculator.calculateChunkMesh(chunk, regionLodLevel);
            updateTerrainMesh(terrainRegion, regionLodLevel);
        });
    }

    public void removeChunkMesh(TerrainRegion terrainRegion, TerrainChunk chunk) {

    }

    public void updateChunkMesh(TerrainRegion terrainRegion, TerrainChunk chunk) {

        //TODO: Update chunk mesh and neighbors

    }

    private void updateTerrainMesh(TerrainRegion terrainRegion, int lodLevel) {
        TerrainMesh terrainMesh = terrainRegion.getOrCreateMesh();

        TextureAtlas textureAtlas = TextureAtlasManager.getInstance().getBlockTextureAtlas();

        int minChunkX = terrainRegion.getBounds().getMinChunkX(terrainRegion.getRegionX());
        int minChunkY = terrainRegion.getBounds().getMinChunkY(terrainRegion.getRegionY());
        int minChunkZ = terrainRegion.getBounds().getMinChunkZ(terrainRegion.getRegionZ());

        int maxChunkX = terrainRegion.getBounds().getMaxChunkX(terrainRegion.getRegionX());
        int maxChunkY = terrainRegion.getBounds().getMaxChunkY(terrainRegion.getRegionY());
        int maxChunkZ = terrainRegion.getBounds().getMaxChunkZ(terrainRegion.getRegionZ());

        FloatArray verts = new FloatArray();
        IntArray idxs = new IntArray();

        int numVertices = 0;
        int numIndices = 0;
        int amountFaces = 0;
        int vertexIndexOffset = 0;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int y = minChunkY; y <= maxChunkY; y++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    TerrainChunk terrainChunk = terrainRegion.getTerrainManager().getChunkNow(x, y, z);
                    if (terrainChunk == null) {
                        continue;
                    }
                    RenderableChunk renderableChunk = terrainChunk;
                    if (lodLevel != 0) {
                        renderableChunk = terrainChunk.getLodChunk(lodLevel);
                    }

                    int offsetXInBlocks = terrainRegion.getBounds().getOffsetX(x) * terrainRegion.getTerrainManager().getWorld().getChunkSizeX();
                    int offsetYInBlocks = terrainRegion.getBounds().getOffsetY(y) * terrainRegion.getTerrainManager().getWorld().getChunkSizeY();
                    int offsetZInBlocks = terrainRegion.getBounds().getOffsetZ(z) * terrainRegion.getTerrainManager().getWorld().getChunkSizeZ();

                    float[] vertices = renderableChunk.getChunkProtoMesh().createArrayForVertices(ProtoMask.Type.OPAQUE);
                    int[] indices = renderableChunk.getChunkProtoMesh().createArrayForIndices(ProtoMask.Type.OPAQUE);

                    renderableChunk.getChunkProtoMesh().appendToBuffers(ProtoMask.Type.OPAQUE, vertices, indices, vertexIndexOffset, textureAtlas, (byte) lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

                    int storageVertices = renderableChunk.getChunkProtoMesh().getAmountVertices(ProtoMask.Type.OPAQUE);
                    int storageIndices = renderableChunk.getChunkProtoMesh().getAmountIndices(ProtoMask.Type.OPAQUE);
                    int storageFaces = renderableChunk.getChunkProtoMesh().getAmountFaces(ProtoMask.Type.OPAQUE);

                    numVertices += storageVertices;
                    numIndices += storageIndices;
                    amountFaces += storageFaces;
                    vertexIndexOffset += storageVertices;

                    verts.addAll(vertices);
                    idxs.addAll(indices);
                }
            }
        }

        if (amountFaces > 0) {

            if(terrainRegion.getRegionX() == 0 && terrainRegion.getRegionY() == 1 && terrainRegion.getRegionZ() == 0) {
                System.out.println("Updated with "+amountFaces+" faces ");
            }

            terrainMesh.setMeshData(verts.toArray(), idxs.toArray(), amountFaces, numVertices, numIndices);
        }
    }

    private boolean skip(TerrainChunk chunk) {
        return chunk.isEmpty() || !chunk.hasNeighborsToAllSides();
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine(ChunkMeshCalculator.chunkCalculatorThroughput.format());
    }

    public static class TerrainMeshBuffer {
        @Getter
        private DynamicFloatBuffer vertexBuffer = new PlainDynamicFloatBuffer(1, true);
        @Getter
        private DynamicIntBuffer indexBuffer = new PlainDynamicIntBuffer(1, true);
        private BufferRange[] meshBuffersOnCPU;
        private BufferRange[] meshBuffersOnGPU;
        private final ShortList meshBuffersOnCPUOrder = new ShortArrayList();
        private final ShortList meshBuffersOnGPUOrder = new ShortArrayList();

        private final TerrainManager parent;
        private IntMesh mesh;

        public TerrainMeshBuffer(TerrainManager parent) {
            this.parent = parent;
            RegionBounds bounds = parent.getBounds();

            int regionSize = bounds.regionSizeX() * bounds.regionSizeY() * bounds.regionSizeZ();
            this.meshBuffersOnCPU = new BufferRange[regionSize];
            this.meshBuffersOnGPU = new BufferRange[regionSize];
        }

        public IntMesh getOrCreateMesh() {
            if (meshBuffersOnCPUOrder.isEmpty()) {
                return null;
            }

            DynamicFloatBuffer gpuVertices;
            DynamicIntBuffer gpuIndices;
            if (mesh == null) {
                gpuVertices = vertexBuffer;
                gpuIndices = indexBuffer;

                this.meshBuffersOnGPU = Arrays.copyOf(this.meshBuffersOnCPU, this.meshBuffersOnCPU.length);
                this.meshBuffersOnGPUOrder.clear();
                this.meshBuffersOnGPUOrder.addAll(this.meshBuffersOnCPUOrder);

                this.meshBuffersOnCPUOrder.clear();
                vertexBuffer = new PlainDynamicFloatBuffer(1, true);
                indexBuffer = new PlainDynamicIntBuffer(1, true);
            } else {
                mesh.dispose();

                float[] vertices = new float[mesh.getNumVertices()];
                mesh.getVertices(vertices);
                int[] indices = new int[mesh.getNumIndices()];
                mesh.getIndices(indices);

                gpuVertices = new PlainDynamicFloatBuffer(vertices, true);
                gpuIndices = new PlainDynamicIntBuffer(indices, true);

                int vertShift = 0;
                int idxShift = 0;

                for (int i = 0; i < meshBuffersOnGPUOrder.size(); i++) {
                    short idx = meshBuffersOnGPUOrder.getShort(i);
                    BufferRange bufferRangeInGPU = meshBuffersOnGPU[idx];
                    if (bufferRangeInGPU == null) {
                        continue;
                    }

                    int indexInCPUOrder = meshBuffersOnCPUOrder.indexOf(idx);

                    int actualVertStart = bufferRangeInGPU.vertexStart + vertShift;
                    int actualVertEnd = bufferRangeInGPU.vertexEndExclusive + vertShift;
                    int actualIdxStart = bufferRangeInGPU.indexStart + idxShift;
                    int actualIdxEnd = bufferRangeInGPU.indexEndExclusive + idxShift;


                    int deltaVert = 0;
                    int deltaIdx = 0;

                    // No new data found for this buffer part on the cpu side buffer
                    if (indexInCPUOrder != -1) {
                        BufferRange bufferRangeInCPU = meshBuffersOnCPU[indexInCPUOrder];

                        float[] meshVerticesUpdated = Arrays.copyOfRange(
                                vertexBuffer.getBuffer(),
                                bufferRangeInCPU.vertexStart,
                                bufferRangeInCPU.vertexEndExclusive
                        );
                        int[] meshIndicesUpdated = Arrays.copyOfRange(
                                indexBuffer.getBuffer(),
                                bufferRangeInCPU.indexStart,
                                bufferRangeInCPU.indexEndExclusive
                        );

                        int newVertEnd = gpuVertices.update(actualVertStart, actualVertEnd, meshVerticesUpdated);
                        int newIdxEnd = gpuIndices.update(actualIdxStart, actualIdxEnd, meshIndicesUpdated);

                        deltaVert = newVertEnd - actualVertEnd;
                        deltaIdx = newIdxEnd - actualIdxEnd;
                    }

                    meshBuffersOnGPU[idx] = new BufferRange(actualVertStart, actualVertEnd + deltaVert, actualIdxStart, actualIdxEnd + deltaIdx);

                    vertShift += deltaVert;
                    idxShift += deltaIdx;
                }
            }

            if (gpuVertices.getBuffer().length == 1) {
                return null;
            }

            mesh = new IntMesh(true, gpuVertices.getBuffer().length / Shaders.SINGLE_OPAQUE_ATTRIBUTES.vertexSize, gpuIndices.getBuffer().length, Shaders.SINGLE_OPAQUE_ATTRIBUTES_ARRAY);
            mesh.setVertices(gpuVertices.getBuffer());
            mesh.setIndices(gpuIndices.getBuffer());
            return mesh;
        }

        public synchronized void addChunkMesh(byte lodLevel, TerrainFaceStorage.ChunkFaceStorage chunkFaces, int offsetXInRegion, int offsetYInRegion, int offsetZInRegion) {
            if (chunkFaces.isEmpty()) {
                return;
            }

            float[] vertices = new float[chunkFaces.getAmountFloats()];
            int[] indices = new int[chunkFaces.getAmountIndices()];

            int offsetXInBlocks = offsetXInRegion * parent.getWorld().getChunkSizeX();
            int offsetYInBlocks = offsetYInRegion * parent.getWorld().getChunkSizeY();
            int offsetZInBlocks = offsetZInRegion * parent.getWorld().getChunkSizeZ();

            chunkFaces.collectFaces(vertices, indices, lodLevel, TextureAtlasManager.getInstance().getBlockTextureAtlas(), offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

            int idx = computeIdx(offsetXInRegion, offsetYInRegion, offsetZInRegion);

            BufferRange bufferRange = meshBuffersOnCPU[idx];

            if (bufferRange != null) {
                int vertexStartNew = bufferRange.vertexStart;
                int vertexEndNew = vertexBuffer.update(bufferRange.vertexStart, bufferRange.vertexEndExclusive, vertices);
                int indexStartNew = bufferRange.indexStart;
                int indexEndNew = indexBuffer.update(bufferRange.indexStart, bufferRange.indexEndExclusive, indices);

                int vertexDelta = vertexEndNew - bufferRange.vertexEndExclusive;
                int indexDelta = indexEndNew - bufferRange.indexEndExclusive;

                meshBuffersOnCPU[idx] = new BufferRange(vertexStartNew, vertexEndNew, indexStartNew, indexEndNew);

                int indexInOrder = meshBuffersOnCPUOrder.indexOf((short) idx);

                for (int i = indexInOrder; i < meshBuffersOnCPUOrder.size(); i++) {
                    int idxOfSubsequentBufferRange = meshBuffersOnCPUOrder.getShort(i);
                    BufferRange subsequentRange = meshBuffersOnCPU[idxOfSubsequentBufferRange];
                    meshBuffersOnCPU[idxOfSubsequentBufferRange] = new BufferRange(subsequentRange.vertexStart + vertexDelta, subsequentRange.vertexEndExclusive + vertexDelta, subsequentRange.indexStart + indexDelta, subsequentRange.indexEndExclusive + indexDelta);
                }
            } else {
                int vertexStart = vertexBuffer.size();
                int vertexEnd = vertexStart + vertices.length;
                vertexBuffer.insert(vertexBuffer.size(), vertices);
                int indexStart = indexBuffer.size();
                int indexEnd = indexStart + indices.length;
                indexBuffer.insert(indexBuffer.size(), indices);
                meshBuffersOnCPU[idx] = new BufferRange(vertexStart, vertexEnd, indexStart, indexEnd);
                meshBuffersOnCPUOrder.add((short) idx);
            }
        }

        private record BufferRange(int vertexStart, int vertexEndExclusive, int indexStart, int indexEndExclusive) {
        }

        private int computeIdx(int regionOffsetX, int regionOffsetY, int regionOffsetZ) {
            RegionBounds regionBounds = this.parent.getBounds();
            return regionOffsetX + regionOffsetY * regionBounds.regionSizeX() + regionOffsetZ * regionBounds.regionSizeX() * regionBounds.regionSizeY();
        }
    }

    private TerrainFaceStorage createStorageForLodLevel(int lodLevel) {
        return new TerrainFaceStorageImpl(terrainManager, (byte) lodLevel);
    }
}
