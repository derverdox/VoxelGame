package de.verdox.voxel.client.level.chunk.occupancy;

import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.chunk.data.ChunkData;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;
import lombok.Setter;

import java.util.BitSet;

public class BitsetBasedOccupancyMask implements OccupancyMask, ChunkData<Chunk> {
    @Getter
    @Setter
    private Chunk owner;
    /**
     * flache BitMap für alle (x,y,z): index = (x*sy + y)*sz + z
     **/
    private BitSet occupancy;
    private int sideOcclusionMask;
    private int totalOpaqueCount;

    @Override
    public void initFromOwner() {
        int sx = owner.getSizeX();
        int sy = owner.getSizeY();
        int sz = owner.getSizeZ();
        int totalBits = sx * sy * sz;
        occupancy = new BitSet(totalBits);
        totalOpaqueCount = 0;
        // füllen
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                int base = (x * sy + y) * sz;
                for (int z = 0; z < sz; z++) {
                    if (!owner.getBlockAt(x, y, z).isTransparent()) {
                        occupancy.set(base + z);
                        totalOpaqueCount++;
                    }
                }
            }
        }

        computeSideMask();
    }

    @Override
    public boolean isChunkFullOpaque() {
        return totalOpaqueCount == owner.getSizeX() * owner.getSizeY() * owner.getSizeZ();
    }

    @Override
    public boolean isChunkEmpty() {
        return totalOpaqueCount == 0;
    }

    @Override
    public long getZColumn(int x, int y) {
        long mask = 0L;
        int base = (x * owner.getSizeY() + y) * owner.getSizeZ();
        for (int z = 0; z < owner.getSizeZ(); z++) {
            if (occupancy.get(base + z)) {
                mask |= 1L << z;
            }
        }
        return mask;
    }

    @Override
    public long getSideMask() {
        return sideOcclusionMask;
    }

    @Override
    public boolean isOpaque(int x, int y, int z) {
        int idx = (x * owner.getSizeY() + y) * owner.getSizeZ() + z;
        return occupancy.get(idx);
    }

    @Override
    public void updateOccupancyMask(BlockBase block, int x, int y, int z) {
        int idx = (x * owner.getSizeY() + y) * owner.getSizeZ() + z;
        boolean wasOpaque = occupancy.get(idx);
        boolean nowOpaque = !block.isTransparent();

        if (wasOpaque == nowOpaque) {
            return;
        }
        // Bit setzen oder löschen
        if (nowOpaque) {
            occupancy.set(idx);
            totalOpaqueCount++;
        } else {
            occupancy.clear(idx);
            totalOpaqueCount--;
        }
        // Side-Occlusion neu berechnen
        computeSideMask();
    }

    @Override
    public long getTotalOpaque() {
        return totalOpaqueCount;
    }

    private void computeSideMask() {
        int m = 0;
        if (isFaceFullX(0)) m |= 1 << Direction.WEST.getId();
        if (isFaceFullX(owner.getSizeX() - 1)) m |= 1 << Direction.EAST.getId();
        if (isFaceFullY(0)) m |= 1 << Direction.DOWN.getId();
        if (isFaceFullY(owner.getSizeY() - 1)) m |= 1 << Direction.UP.getId();
        if (isFaceFullZ(0)) m |= 1 << Direction.NORTH.getId();
        if (isFaceFullZ(owner.getSizeZ() - 1)) m |= 1 << Direction.SOUTH.getId();
        sideOcclusionMask = m;
    }

    private boolean isFaceFullX(int x) {
        for (int y = 0; y < owner.getSizeY(); y++) {
            long col = getZColumn(x, y);
            if (col != (~0L >>> (64 - owner.getSizeZ()))) return false;
        }
        return true;
    }

    private boolean isFaceFullY(int y) {
        for (int x = 0; x < owner.getSizeX(); x++) {
            long col = getZColumn(x, y);
            if (col != (~0L >>> (64 - owner.getSizeZ()))) return false;
        }
        return true;
    }

    private boolean isFaceFullZ(int z) {
        long bit = 1L << z;
        for (int x = 0; x < owner.getSizeX(); x++) {
            for (int y = 0; y < owner.getSizeY(); y++) {
                int idx = (x * owner.getSizeY() + y) * owner.getSizeZ() + z;
                if (!occupancy.get(idx)) return false;
            }
        }
        return true;
    }
}
