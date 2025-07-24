package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;

public interface ChunkMeshCalculator {
    BlockFaceStorage calculateChunkMesh(BlockFaceStorage blockFaces, ClientChunk chunk, float chunkOffsetX, float chunkOffsetY, float chunkOffsetZ);
}
