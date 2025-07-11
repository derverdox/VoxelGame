package de.verdox.voxel.shared.util.bitpackedarray;

import lombok.Getter;

import java.util.function.Function;

@Getter
public class RadialBitpackedArray<T> extends ThreeDimensionalArray<T> {
    private int sizeZ;
    private int sizeX;
    private int sizeY;
    private int bitsX;
    private int bitsY;
    private int bitsZ;
    private int shiftXY;
    private int shiftYZ;

    public RadialBitpackedArray(int dimX, int dimY, int dimZ, Function<Integer, T[]> arrayCreator) {
        super(dimX, dimY, dimZ, arrayCreator);
    }

    @Override
    protected T[] construct() {
        this.sizeX = 2 * this.dimX + 1;
        this.sizeY = 2 * this.dimY + 1;
        this.sizeZ = 2 * this.dimZ + 1;

        // Bit-Breiten (ceil(log2(size))) je Achse
        this.bitsX = sizeX > 1 ? 32 - Integer.numberOfLeadingZeros(sizeX - 1) : 1;
        this.bitsY = sizeY > 1 ? 32 - Integer.numberOfLeadingZeros(sizeY - 1) : 1;
        this.bitsZ = sizeZ > 1 ? 32 - Integer.numberOfLeadingZeros(sizeZ - 1) : 1;

        // Shifts für Bitpacken
        this.shiftXY = bitsY + bitsZ;  // xBits ganz oben: bitsY+bitsZ
        this.shiftYZ = bitsZ;          // yBits oberhalb zBits

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
        return x >= -this.dimX && x <= this.dimX
            && y >= -this.dimY && y <= this.dimY
            && z >= -this.dimZ && z <= this.dimZ;
    }

    @Override
    public int getIndex(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            throw new IllegalArgumentException(
                String.format("Koordinate (%d,%d,%d) außerhalb ±(%d,%d,%d)",
                    x, y, z, this.dimX, this.dimY, this.dimZ));
        }
        int ux = x + this.dimX;
        int uy = y + this.dimY;
        int uz = z + this.dimZ;

        return (ux << shiftXY)
            | (uy << shiftYZ)
            | uz;
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
