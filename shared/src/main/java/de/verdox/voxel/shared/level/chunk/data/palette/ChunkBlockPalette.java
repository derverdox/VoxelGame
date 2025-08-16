package de.verdox.voxel.shared.level.chunk.data.palette;

import de.verdox.voxel.shared.data.types.Registries;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.chunk.data.ChunkData;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import lombok.Getter;
import lombok.Setter;

public class ChunkBlockPalette extends ThreeDimensionalPalette<BlockBase> implements ChunkData<Chunk> {
    @Getter
    @Setter
    private Chunk owner;

    public ChunkBlockPalette(BlockBase defaultValue) {
        super(defaultValue, id -> Registries.BLOCKS.streamEntries().filter(blockBase -> blockBase.getMaterialID() == id).findFirst().orElse(null));
    }

    @Override
    public int getSizeX() {
        return owner.getSizeX();
    }

    @Override
    public int getSizeY() {
        return owner.getSizeY();
    }


    @Override
    public int getSizeZ() {
        return owner.getSizeZ();
    }
}
