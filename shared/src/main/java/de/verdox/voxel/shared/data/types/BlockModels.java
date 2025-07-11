package de.verdox.voxel.shared.data.types;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModel;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

public class BlockModels {
    @Getter
    private static final Set<BlockModel> blockModels = new HashSet<>();

    public static void bootstrap() {

    }

    public static final BlockModel STONE = register(new BlockModel()
        .withModelType(BlockModelTypes.CUBE)
        .withTextureMapping("top", ResourceLocation.of("stone"))
        .withTextureMapping("bottom", ResourceLocation.of("stone"))
        .withTextureMapping("back", ResourceLocation.of("stone"))
        .withTextureMapping("front", ResourceLocation.of("stone"))
        .withTextureMapping("left", ResourceLocation.of("stone"))
        .withTextureMapping("right", ResourceLocation.of("stone")));

    private static BlockModel register(BlockModel blockModel) {
        blockModels.add(blockModel);
        return blockModel;
    }
}
