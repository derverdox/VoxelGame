package de.verdox.voxel.shared.level.chunk.data;

import de.verdox.voxel.shared.level.chunk.Chunk;

public interface ChunkData<CHUNK extends Chunk> {
    CHUNK getOwner();
    void setOwner(CHUNK owner);
}