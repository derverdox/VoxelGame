package de.verdox.voxel.shared.util.palette;

import de.verdox.voxel.shared.data.registry.ResourceLocation;

public class ChunkBlockPalette extends ThreeDimensionalPalette<ResourceLocation> {
    /**
     * Create a palette region of given dimensions, all initialized to defaultValue.
     *
     * @param defaultValue
     * @param dimensionX
     * @param dimensionY
     * @param dimensionZ
     */
    public ChunkBlockPalette(ResourceLocation defaultValue, short dimensionX, short dimensionY, short dimensionZ) {
        super(defaultValue, dimensionX, dimensionY, dimensionZ);
    }
}
