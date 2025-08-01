package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.util.ThroughputBenchmark;

public interface ChunkMeshCalculator {
    ThroughputBenchmark chunkCalculatorThroughput = new ThroughputBenchmark("ChunkMeshes");

    void calculateChunkMesh(TerrainFaceStorage.ChunkFaceStorage blockFaces, ClientChunk chunk, int lodLevel);
}
