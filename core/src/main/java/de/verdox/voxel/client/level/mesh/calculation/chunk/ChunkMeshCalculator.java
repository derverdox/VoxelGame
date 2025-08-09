package de.verdox.voxel.client.level.mesh.calculation.chunk;

import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.util.ThroughputBenchmark;

public interface ChunkMeshCalculator {
    ThroughputBenchmark chunkCalculatorThroughput = new ThroughputBenchmark("ChunkMeshes");

    void calculateChunkMesh(TerrainChunk chunk, int lodLevel);
}
