package de.verdox.voxel.shared.level.block;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.BlockModels;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.data.types.Registries;
import lombok.Getter;
import lombok.Setter;

public class BlockBase {
    @Getter @Setter
    private short graphicsBlockId;

    private ResourceLocation key;

    public ResourceLocation findKey() {
        if (key == null) {
            this.key = Registries.BLOCKS.getKeyOrThrow(this);
        }
        return this.key;
    }

    public BlockModel getModel() {
        return BlockModels.STONE;
    }

    public boolean isOpaque() {
        return !this.equals(Blocks.AIR);
    }

    public boolean isTransparent() {
        return this.equals(Blocks.AIR);
    }

    public boolean isInteractableByRayCast() {
        return !this.equals(Blocks.AIR);
    }

    public byte getEmissionRed() {
        return 0;
    }

    public byte getEmissionGreen() {
        return 0;
    }

    public byte getEmissionBlue() {
        return 0;
    }
}
