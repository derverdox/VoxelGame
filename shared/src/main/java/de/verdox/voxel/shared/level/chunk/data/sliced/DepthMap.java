package de.verdox.voxel.shared.level.chunk.data.sliced;

import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.ChunkData;
import lombok.Getter;
import lombok.Setter;

public class DepthMap extends AbstractSliceMap implements ChunkData<ChunkBase<?>> {
    @Getter
    @Setter
    private ChunkBase<?> owner;

    @Override
    public int getSizeX() {
        return owner.getBlockSizeX();
    }

    @Override
    public int getSizeZ() {
        return owner.getBlockSizeZ();
    }
}
