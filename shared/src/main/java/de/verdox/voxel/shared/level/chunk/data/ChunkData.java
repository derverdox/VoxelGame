package de.verdox.voxel.shared.level.chunk.data;

import de.verdox.voxel.shared.level.chunk.ChunkBase;

public interface ChunkData<CHUNK extends ChunkBase<?>> {
    CHUNK getOwner();
    void setOwner(CHUNK owner);
}