package de.verdox.voxel.shared.util.bitpackedarray;

import java.util.function.Function;

public class LinearRadialArray<T> extends ThreeDimensionalArray<T> {
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    public LinearRadialArray(int dimX, int dimY, int dimZ, Function<Integer, T[]> arrayCreator) {
        super(dimX, dimY, dimZ, arrayCreator);
    }

    @Override
    protected T[] construct() {
        this.sizeX = 2 * dimX + 1;
        this.sizeY = 2 * dimY + 1;
        this.sizeZ = 2 * dimZ + 1;
        int total = sizeX * sizeY * sizeZ;
        return arrayCreator.apply(total);
    }

    public boolean isInBounds(int x, int y, int z) {
        return x >= -dimX && x <= dimX
            && y >= -dimY && y <= dimY
            && z >= -dimZ && z <= dimZ;
    }


    public int getIndex(int x, int y, int z) {
        if (!isInBounds(x, y, z)) throw new IllegalArgumentException();
        int ux = x + dimX; // [0..sizeX-1]
        int uy = y + dimY;
        int uz = z + dimZ;
        // klassisches Zeilen-Major-Mapping:
        return (ux * sizeY + uy) * sizeZ + uz;
    }

    @Override
    public int getMinX() {
        return -dimX;
    }

    @Override
    public int getMinY() {
        return -dimY;
    }

    @Override
    public int getMinZ() {
        return -dimZ;
    }

    @Override
    public int getMaxX() {
        return dimX;
    }

    @Override
    public int getMaxY() {
        return dimY;
    }

    @Override
    public int getMaxZ() {
        return dimZ;
    }
}
