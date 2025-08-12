package de.verdox.voxel.shared.util.lod;

@FunctionalInterface
public interface OccupancyFn {
    boolean test(int id);
}
