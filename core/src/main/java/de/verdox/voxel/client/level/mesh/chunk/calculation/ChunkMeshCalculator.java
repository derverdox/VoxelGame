package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;

public interface ChunkMeshCalculator {
    void calculateChunkMesh(TerrainFaceStorage.ChunkFaceStorage blockFaces, ClientChunk chunk, int lodLevel);
}
