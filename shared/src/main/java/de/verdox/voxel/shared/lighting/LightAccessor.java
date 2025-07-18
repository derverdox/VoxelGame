package de.verdox.voxel.shared.lighting;

import de.verdox.voxel.shared.util.Direction;

public interface LightAccessor {

    int getRegionX();
    int getRegionY();
    int getRegionZ();

    int sizeX();

    int sizeY();

    int sizeZ();

    boolean isOpaque(int localX, int localY, int localZ);

    byte getEmissionRed(int localX, int localY, int localZ);

    byte getEmissionBlue(int localX, int localY, int localZ);

    byte getEmissionGreen(int localX, int localY, int localZ);

    byte getBlockLightRed(int localX, int localY, int localZ);

    byte getBlockLightBlue(int localX, int localY, int localZ);

    byte getBlockLightGreen(int localX, int localY, int localZ);

    void setBlockLight(int localX, int localY, int localZ, byte red, byte green, byte blue);

    byte getSkyLight(int localX, int localY, int localZ);

    void setSkyLight(int localX, int localY, int localZ, byte light);

    byte getWorldSkyLight();

    boolean isInBounds(int localX, int localY, int localZ);

    int getHighestNonAirBlockAt(int localX, int localZ);

    boolean isAirRegion();

    LightAccessor getNeighbor(Direction direction);

    LightAccessor getRelative(int x, int y, int z);
}
