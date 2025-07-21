package de.verdox.voxel.client.level.chunk.occupancy;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.util.Direction;

import java.util.BitSet;

public class BitsetBasedOccupancyMask implements OccupancyMask {
    private int sx, sy, sz;
    /** flache BitMap für alle (x,y,z): index = (x*sy + y)*sz + z **/
    private BitSet occupancy;
    private int sideOcclusionMask;
    private int totalOpaqueCount;

    @Override
    public void initFromChunk(ClientChunk chunk) {
        this.sx = chunk.getBlockSizeX();
        this.sy = chunk.getBlockSizeY();
        this.sz = chunk.getBlockSizeZ();
        int totalBits = sx * sy * sz;
        occupancy = new BitSet(totalBits);
        totalOpaqueCount = 0;

        // füllen
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                int base = (x * sy + y) * sz;
                for (int z = 0; z < sz; z++) {
                    if (!chunk.getBlockAt(x, y, z).isTransparent()) {
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
        return totalOpaqueCount == sx * sy * sz;
    }

    @Override
    public boolean isChunkEmpty() {
        return totalOpaqueCount == 0;
    }

    @Override
    public long getZColumn(int x, int y) {
        long mask = 0L;
        int base = (x * sy + y) * sz;
        for (int z = 0; z < sz; z++) {
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
        int idx = (x * sy + y) * sz + z;
        return occupancy.get(idx);
    }

    @Override
    public void updateOccupancyMask(BlockBase block, int x, int y, int z) {
        int idx = (x * sy + y) * sz + z;
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
        if (isFaceFullX(0))    m |= 1 << Direction.WEST .getId();
        if (isFaceFullX(sx-1)) m |= 1 << Direction.EAST .getId();
        if (isFaceFullY(0))    m |= 1 << Direction.DOWN .getId();
        if (isFaceFullY(sy-1)) m |= 1 << Direction.UP   .getId();
        if (isFaceFullZ(0))    m |= 1 << Direction.NORTH.getId();
        if (isFaceFullZ(sz-1)) m |= 1 << Direction.SOUTH.getId();
        sideOcclusionMask = m;
    }

    private boolean isFaceFullX(int x) {
        for (int y = 0; y < sy; y++) {
            long col = getZColumn(x, y);
            if (col != (~0L >>> (64 - sz))) return false;
        }
        return true;
    }

    private boolean isFaceFullY(int y) {
        for (int x = 0; x < sx; x++) {
            long col = getZColumn(x, y);
            if (col != (~0L >>> (64 - sz))) return false;
        }
        return true;
    }

    private boolean isFaceFullZ(int z) {
        long bit = 1L << z;
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                int idx = (x * sy + y) * sz + z;
                if (!occupancy.get(idx)) return false;
            }
        }
        return true;
    }
}
