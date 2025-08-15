package de.verdox.voxel.shared.level.chunk;

import de.verdox.voxel.shared.level.block.BlockBase;

public interface SuperChunk extends Box {
    BlockBase getDominantType(byte x, byte y, byte z);

    boolean isFullyTransparent(byte x, byte y, byte z);
}
