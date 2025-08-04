package de.verdox.voxel.client.level.chunk.occupancy;

import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;
import lombok.Setter;

@Getter
public class NaiveChunkOccupancyMask implements OccupancyMask {
    @Getter @Setter
    private Chunk owner;
    private int sx, sy, sz;

    private long[][] occupancyMask;

    private long totalOpaqueBlocks;
    private boolean chunkFullOpaque;
    private boolean chunkEmpty = true;
    private int sideOcclusionMask;

    @Override
    public boolean isOpaque(int localX, int localY, int localZ) {
        return (occupancyMask[localX][localY] & (1L << localZ)) != 0;
    }

    @Override
    public void updateOccupancyMask(BlockBase block, int x, int y, int z) {
        //TODO:
    }

    /**
     * Initial befüllen der occupancyMask und der Side‐Occlusion‐Mask.
     */
    @Override
    public void initFromOwner() {
        int newSx = owner.getSizeX();
        int newSy = owner.getSizeY();
        int newSz = owner.getSizeZ();
        if (newSx > World.MAX_CHUNK_SIZE
                || newSy > World.MAX_CHUNK_SIZE
                || newSz > World.MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("Max chunk size is " + World.MAX_CHUNK_SIZE);
        }

        // (Re-)Allokation, falls nötig
        if (newSx != sx || newSy != sy || newSz != sz) {
            sx = newSx;
            sy = newSy;
            sz = newSz;
            occupancyMask = new long[sx][sy];
        }

        totalOpaqueBlocks = 0;
        chunkEmpty = true;

        // 1) occupancyMask füllen
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long mask = 0L;
                int count = 0;
                for (int z = 0; z < sz; z++) {
                    BlockBase b = owner.getBlockAt(x, y, z);
                    if (!b.isTransparent()) {
                        mask |= 1L << z;
                        count++;
                    }
                }
                occupancyMask[x][y] = mask;
                totalOpaqueBlocks += count;
                if (count > 0) {
                    chunkEmpty = false;
                }
            }
        }

        chunkFullOpaque = (totalOpaqueBlocks == (long) sx * sy * sz);

        // 2) Side-Occlusion-Mask berechnen
        computeSideOcclusionMask();
    }

    @Override
    public long getZColumn(int x, int y) {
        return occupancyMask[x][y];
    }

    @Override
    public long getSideMask() {
        return sideOcclusionMask;
    }

    /**
     * Setzt sideOcclusionMask:
     * Bit i gesetzt, wenn an der entsprechenden Chunk-Seite keine Faces entstehen.
     */
    private void computeSideOcclusionMask() {
        int mask = 0;

        // WEST (-X): x=0 Spalten
        if (isFaceFullyOpaqueAtX(0)) mask |= 1 << Direction.WEST.getId();
        // EAST (+X): x=sx-1
        if (isFaceFullyOpaqueAtX(sx - 1)) mask |= 1 << Direction.EAST.getId();
        // DOWN (-Y): y=0
        if (isFaceFullyOpaqueAtY(0)) mask |= 1 << Direction.DOWN.getId();
        // UP (+Y): y=sy-1
        if (isFaceFullyOpaqueAtY(sy - 1)) mask |= 1 << Direction.UP.getId();
        // NORTH (-Z): für jede (x,y) gilt bit 0 gesetzt
        if (isFaceFullyOpaqueAtZ(0)) mask |= 1 << Direction.NORTH.getId();
        // SOUTH (+Z): für jede (x,y) gilt bit sz-1 gesetzt
        if (isFaceFullyOpaqueAtZ(sz - 1)) mask |= 1 << Direction.SOUTH.getId();

        sideOcclusionMask = mask;
    }

    private boolean isFaceFullyOpaqueAtX(int x) {
        // Für alle (x,y,z) muss isOpaque(x,y,z) == true
        for (int y = 0; y < sy; y++) {
            long colMask = occupancyMask[x][y];
            // wenn Spalte nicht komplett voll (alle z-Bits gesetzt), ist Seite nicht komplett occluded
            if (colMask != (~0L >>> (64 - sz))) {
                return false;
            }
        }
        return true;
    }

    private boolean isFaceFullyOpaqueAtY(int y) {
        for (int x = 0; x < sx; x++) {
            long colMask = occupancyMask[x][y];
            if (colMask != (~0L >>> (64 - sz))) {
                return false;
            }
        }
        return true;
    }

    private boolean isFaceFullyOpaqueAtZ(int z) {
        // Testet für jedes (x,y), ob Bit z gesetzt ist
        long bit = 1L << z;
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                if ((occupancyMask[x][y] & bit) == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public long getTotalOpaque() {
        return totalOpaqueBlocks;
    }
}
