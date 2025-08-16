package de.verdox.voxel.shared.util.palette.strategy;

public interface PaletteIDMapper<T extends PaletteIDHolder> {
    T byID(int id);
}
