package de.verdox.voxel.shared.data.types;

import de.verdox.voxel.shared.level.block.BlockModelType;

public class BlockModelTypes {
    public static final BlockModelType CUBE = new BlockModelType()
        .addFace("top", BlockModelType.BlockFace.top())
        .addFace("bottom", BlockModelType.BlockFace.bottom())
        .addFace("back", BlockModelType.BlockFace.north())
        .addFace("front", BlockModelType.BlockFace.south())
        .addFace("left", BlockModelType.BlockFace.east())
        .addFace("right", BlockModelType.BlockFace.west());
}
