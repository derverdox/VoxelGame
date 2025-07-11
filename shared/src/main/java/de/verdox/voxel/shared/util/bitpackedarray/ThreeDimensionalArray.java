package de.verdox.voxel.shared.util.bitpackedarray;

import lombok.Getter;

import java.util.function.Function;

public abstract class ThreeDimensionalArray<T> {

    private final T[] array;

    @Getter
    protected final int dimX;
    @Getter
    protected final int dimY;
    @Getter
    protected final int dimZ;
    protected final Function<Integer, T[]> arrayCreator;

    public ThreeDimensionalArray(int dimX, int dimY, int dimZ, Function<Integer, T[]> arrayCreator) {
        this.dimX = dimX;
        this.dimY = dimY;
        this.dimZ = dimZ;
        this.arrayCreator = arrayCreator;
        if (dimX < 0 || dimY < 0 || dimZ < 0) {
            throw new IllegalArgumentException("Dimensions must be >= 0");
        }
        this.array = construct();
    }

    public int size() {
        return array.length;
    }

    protected abstract T[] construct();

    public abstract boolean isInBounds(int x, int y, int z);

    public abstract int getIndex(int x, int y, int z);

    public T get(int x, int y, int z) {
        return array[getIndex(x, y, z)];
    }

    public T get(int index) {
        return array[index];
    }

    public T getOrPut(T data, int x, int y, int z) {
        int index = getIndex(x, y, z);
        T oldData = array[index];
        if (oldData != null) {
            return oldData;
        }
        array[index] = data;
        return data;
    }

    public T set(T data, int x, int y, int z) {
        int index = getIndex(x, y, z);
        T oldData = array[index];
        array[index] = data;
        return oldData;
    }

    public abstract int getMinX();
    public abstract int getMinY();
    public abstract int getMinZ();

    public abstract int getMaxX();
    public abstract int getMaxY();
    public abstract int getMaxZ();
}
