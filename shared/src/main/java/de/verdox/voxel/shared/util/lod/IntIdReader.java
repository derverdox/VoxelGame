package de.verdox.voxel.shared.util.lod;

@FunctionalInterface
public interface IntIdReader {
    int readId(int x, int y, int z);
}
