package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.level.mesh.proto.ChunkProtoMesh;
import de.verdox.voxel.shared.level.chunk.Chunk;

public interface RenderableChunk extends Chunk {
    OccupancyMask getChunkOccupancyMask();

    ChunkProtoMesh getChunkProtoMesh();
}
