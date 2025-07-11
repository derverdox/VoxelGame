package de.verdox.voxel.shared.util.bitpackedarray;

import java.util.function.Function;

public class BitpackedArray<T> extends ThreeDimensionalArray<T> {
    private int bitsX;
    private int bitsY;
    private int bitsZ;
    private int shiftXY;

    public BitpackedArray(int dimX, int dimY, int dimZ, Function<Integer, T[]> arrayCreator) {
        super(dimX, dimY, dimZ, arrayCreator);
    }

    @Override
    protected T[] construct() {
        this.bitsX = dimX > 1 ? 32 - Integer.numberOfLeadingZeros(dimX - 1) : 1;
        this.bitsY = dimY > 1 ? 32 - Integer.numberOfLeadingZeros(dimY - 1) : 1;
        this.bitsZ = dimZ > 1 ? 32 - Integer.numberOfLeadingZeros(dimZ - 1) : 1;

        this.shiftXY = bitsY + bitsZ;

        int dimX = 1 << bitsX;
        int dimY = 1 << bitsY;
        int dimZ = 1 << bitsZ;

        long total = (long) dimX * dimY * dimZ;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Benötigte Array-Größe zu groß: " + total);
        }
        return arrayCreator.apply((int) total);
    }

    @Override
    public boolean isInBounds(int x, int y, int z) {
        return dimX > 0 && dimY > 0 && dimZ > 0 &&
            x >= 0 && x < dimX &&
            y >= 0 && y < dimY &&
            z >= 0 && z < dimZ;
    }

    @Override
    public int getIndex(int x, int y, int z) {
        return (x << shiftXY)
            | (y << bitsZ)
            | z;
    }

    @Override
    public int getMinX() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getMinZ() {
        return 0;
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
