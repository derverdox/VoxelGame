package de.verdox.voxel.client.util;

import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.renderer.terrain.regions.RegionalizedTerrainManager;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.renderer.shader.Shaders;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.buffer.DynamicFloatBuffer;
import de.verdox.voxel.shared.util.buffer.DynamicIntBuffer;
import de.verdox.voxel.shared.util.buffer.PlainDynamicFloatBuffer;
import de.verdox.voxel.shared.util.buffer.PlainDynamicIntBuffer;
import gaiasky.util.gdx.mesh.IntMesh;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import lombok.Getter;

import java.util.Arrays;

public class TerrainMeshBuffer {
    @Getter
    private DynamicFloatBuffer vertexBuffer = new PlainDynamicFloatBuffer(1, true);
    @Getter
    private DynamicIntBuffer indexBuffer = new PlainDynamicIntBuffer(1, true);
    private BufferRange[] meshBuffersOnCPU;
    private BufferRange[] meshBuffersOnGPU;
    private final ShortList meshBuffersOnCPUOrder = new ShortArrayList();
    private final ShortList meshBuffersOnGPUOrder = new ShortArrayList();

    private final RegionalizedTerrainManager parent;
    private IntMesh mesh;

    public TerrainMeshBuffer(RegionalizedTerrainManager parent) {
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
