package de.verdox.voxel.shared.data.types;

import de.verdox.voxel.shared.data.registry.Registry;
import de.verdox.voxel.shared.level.block.BlockBase;

public class Registries {
    public static final Registry<BlockBase> BLOCKS = new Registry<>();

    public static void bootstrap() {
        Blocks.bootstrap();
    }
}
