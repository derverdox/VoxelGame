package de.verdox.voxel.shared.data.types;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockBase;

public class Blocks {
    private static short materialCounter = 0;
    public static final BlockBase AIR = register(new BlockBase(), ResourceLocation.of("air"));
    public static final BlockBase STONE = register(new BlockBase(), ResourceLocation.of("stone"));

    private static <T extends BlockBase> T register(T block, ResourceLocation location) {
        Registries.BLOCKS.register(block, location);
        block.setMaterialID(materialCounter++);
        return block;
    }

    public static void bootstrap() {
    }
}
