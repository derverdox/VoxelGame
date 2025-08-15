package de.verdox.voxel.server.level.chunk.grid.tiles;

public final class TileCell<V> {
    public final int linearIndex; // 0..(sizeX*sizeY*sizeZ-1)
    public final V value;

    TileCell(int linearIndex, V value) {
        this.linearIndex = linearIndex;
        this.value = value;
    }
}
