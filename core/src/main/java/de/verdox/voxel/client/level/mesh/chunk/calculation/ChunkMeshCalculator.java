package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.mesh.terrain.TerrainChunk;
import de.verdox.voxel.client.util.ThroughputBenchmark;

public interface ChunkMeshCalculator {
    ThroughputBenchmark chunkCalculatorThroughput = new ThroughputBenchmark("ChunkMeshes");

    void calculateChunkMesh(TerrainChunk chunk, int lodLevel);
}
