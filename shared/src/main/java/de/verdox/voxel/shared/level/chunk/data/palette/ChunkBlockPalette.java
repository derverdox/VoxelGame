package de.verdox.voxel.shared.level.chunk.data.palette;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.ChunkData;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import lombok.Getter;
import lombok.Setter;

public class ChunkBlockPalette extends ThreeDimensionalPalette<ResourceLocation> implements ChunkData<ChunkBase<?>> {
    @Getter
    @Setter
    private ChunkBase<?> owner;

    public ChunkBlockPalette(ResourceLocation defaultValue) {
        super(defaultValue);
    }

    @Override
    public int getSizeX() {
        return owner.getBlockSizeX();
    }

    @Override
    public int getSizeY() {
        return owner.getBlockSizeZ();
    }


    @Override
    public int getSizeZ() {
        return owner.getBlockSizeZ();
    }
}
