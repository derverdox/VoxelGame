package de.verdox.voxel.test.light;

import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.util.Direction;

import java.util.Arrays;

public class DummyLightAccessor implements LightAccessor {
    private final int sizeX, sizeY, sizeZ;
    private final boolean[][][] opaque;
    private final byte[][][] skyLight;
    private final byte worldSkyLight;

    public DummyLightAccessor(int sizeX, int sizeY, int sizeZ, byte worldSkyLight) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.worldSkyLight = worldSkyLight;

        this.opaque    = new boolean[sizeX][sizeY][sizeZ];
        this.skyLight  = new byte   [sizeX][sizeY][sizeZ];

        // Initialisiere SkyLight-Array mit 0
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                Arrays.fill(this.skyLight[x][y], (byte)0);
            }
        }
    }

    @Override
    public int getRegionX() {
        return 0;
    }

    @Override
    public int getRegionY() {
        return 0;
    }

    @Override
    public int getRegionZ() {
        return 0;
    }

    @Override
    public int sizeX() { return (byte) sizeX; }

    @Override
    public int sizeY() { return (byte) sizeY; }

    @Override
    public int sizeZ() { return (byte) sizeZ; }

    /** Setze, ob ein Block opak ist (Testdaten). */
    public void setOpaque(int x, int y, int z, boolean isOpaque) {
        opaque[x][y][z] = isOpaque;
    }

    @Override
    public boolean isOpaque(int localX, int localY, int localZ) {
        return opaque[localX][localY][localZ];
    }

    // Da wir hier nur SkyLight testen, liefern alle Emissions-Methoden 0:
    @Override public byte getEmissionRed  (int x, int y, int z) { return 0; }
    @Override public byte getEmissionGreen(int x, int y, int z) { return 0; }

    @Override
    public byte getBlockLightRed(int localX, int localY, int localZ) {
        return 0;
    }

    @Override
    public byte getBlockLightBlue(int localX, int localY, int localZ) {
        return 0;
    }

    @Override
    public byte getBlockLightGreen(int localX, int localY, int localZ) {
        return 0;
    }

    @Override public byte getEmissionBlue (int x, int y, int z) { return 0; }

    @Override
    public void setBlockLight(int localX, int localY, int localZ,
                              byte red, byte green, byte blue) {
        // nicht benötigt für Skylight-Tests
    }

    @Override
    public byte getSkyLight(int localX, int localY, int localZ) {
        return skyLight[localX][localY][localZ];
    }

    @Override
    public void setSkyLight(int localX, int localY, int localZ, byte light) {
        skyLight[localX][localY][localZ] = light;
    }

    @Override
    public byte getWorldSkyLight() {
        return worldSkyLight;
    }

    @Override
    public boolean isInBounds(int localX, int localY, int localZ) {
        return false;
    }

    @Override
    public int getHighestNonAirBlockAt(int localX, int localZ) {
        return 0;
    }

    @Override
    public boolean isAirRegion() {
        return false;
    }

    @Override
    public LightAccessor getNeighbor(Direction direction) {
        return null;
    }

    @Override
    public LightAccessor getRelative(int x, int y, int z) {
        return null;
    }
}

