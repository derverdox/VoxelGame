package de.verdox.voxel.shared.level.chunk.data.sliced;

import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.chunk.data.ChunkData;
import lombok.Getter;
import lombok.Setter;

public class HeightMap extends AbstractSliceMap implements ChunkData<Chunk> {
    @Getter
    @Setter
    private Chunk owner;
    @Override
    public int getSizeX() {
        return owner.getSizeX();
    }

    @Override
    public int getSizeZ() {
        return owner.getSizeZ();
    }
}
