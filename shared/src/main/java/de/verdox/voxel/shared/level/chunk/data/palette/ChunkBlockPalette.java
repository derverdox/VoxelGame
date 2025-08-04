package de.verdox.voxel.shared.level.chunk.data.palette;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.chunk.data.ChunkData;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import lombok.Getter;
import lombok.Setter;

public class ChunkBlockPalette extends ThreeDimensionalPalette<ResourceLocation> implements ChunkData<Chunk> {
    @Getter
    @Setter
    private Chunk owner;

    public ChunkBlockPalette(ResourceLocation defaultValue) {
        super(defaultValue);
    }

    @Override
    public int getSizeX() {
        return owner.getSizeX();
    }

    @Override
    public int getSizeY() {
        return owner.getSizeZ();
    }


    @Override
    public int getSizeZ() {
        return owner.getSizeZ();
    }
}
