package de.verdox.voxel.client.level.chunk;

import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

@Getter
public class ChunkOccupancyMask {
    private final ClientChunk gameChunk;
    private final int sx, sy, sz;

    private final long[][] occupancyMask;
    private final int[] columnOpaqueCount;

    private long totalOpaqueBlocks;
    private boolean chunkFullOpaque;
    private boolean chunkEmpty = true;
    private int sideOcclusionMask;

    public ChunkOccupancyMask(ClientChunk gameChunk, int sx, int sy, int sz) {
        if (sx > World.MAX_CHUNK_SIZE || sy > World.MAX_CHUNK_SIZE || sz > World.MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException("Max chunk size in all dimensions is " + World.MAX_CHUNK_SIZE);
        }
        this.gameChunk = gameChunk;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;

        this.occupancyMask = new long[sx][sy];
        this.columnOpaqueCount = new int[sx * sy];
    }

    public boolean isOpaque(int localX, int localY, int localZ) {
        return (occupancyMask[localX][localY] & (1L << localZ)) != 0;
    }

    public void initOccupancyMask() {
        totalOpaqueBlocks = 0;
        chunkEmpty = true;
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long mask = 0L;
                int count = 0;
                for (int z = 0; z < sz; z++) {
                    BlockBase b = gameChunk.getBlockAt(x, y, z);
                    if (!b.isTransparent()) {
                        mask |= 1L << z;
                        count++;
                    }
                }
                occupancyMask[x][y] = mask;
                columnOpaqueCount[x * sy + y] = count;
                totalOpaqueBlocks += count;
                if (count > 0) chunkEmpty = false;
            }
        }
        chunkFullOpaque = (totalOpaqueBlocks == (long) sx * sy * sz);
        updateSideOcclusionMask();  // untenstehend
    }

    public void updateOccupancyMask(BlockBase b, int x, int y, int z) {
        boolean wasOpaque = (occupancyMask[x][y] & (1L << z)) != 0;
        boolean nowOpaque = !b.isTransparent();
        if (wasOpaque == nowOpaque) return;

        if (nowOpaque) {
            occupancyMask[x][y] |= 1L << z;
            columnOpaqueCount[x * sy + y]++;
            totalOpaqueBlocks++;
        } else {
            occupancyMask[x][y] &= ~(1L << z);
            columnOpaqueCount[x * sy + y]--;
            totalOpaqueBlocks--;
        }

        chunkEmpty = (totalOpaqueBlocks == 0);
        chunkFullOpaque = (totalOpaqueBlocks == (long) sx * sy * sz);

        updateSideOcclusionMask();
    }

    /**
     * Inkrementell oder komplett neu berechnet:
     * setzt sideOcclusionMask (6 Bits)
     */
    private void updateSideOcclusionMask() {
        int mask = 0;
        FaceMasks fm = computeFaceMasks(); // billig, denn sx,sy klein
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            long[][] m = fm.getMaskForDirection(direction);
            boolean allZero = true;
            outer:
            for (int x = 0; x < sx; x++) {
                for (int y = 0; y < sy; y++) {
                    if (m[x][y] != 0L) {
                        allZero = false;
                        break outer;
                    }
                }
            }
            if (allZero) mask |= 1 << direction.getId();
        }
        sideOcclusionMask = mask;
    }

    public int getColumnOpaqueCount(int x, int y) {
        return columnOpaqueCount[x * sy + y];
    }

    public static class FaceMasks {
        public final long[][] xPlus, xMinus, yPlus, yMinus, zPlus, zMinus;

        public FaceMasks(int sx, int sy) {
            this.xPlus = new long[sx][sy];
            this.xMinus = new long[sx][sy];
            this.yPlus = new long[sx][sy];
            this.yMinus = new long[sx][sy];

            this.zPlus = new long[sx][sy];
            this.zMinus = new long[sx][sy];
        }

        public long[][] getMaskForDirection(Direction faceDir) {
            return switch (faceDir) {
                case WEST -> xMinus;
                case EAST -> xPlus;
                case DOWN -> yMinus;
                case UP -> yPlus;
                case NORTH -> zMinus;
                case SOUTH -> zPlus;
            };
        }
    }


    /**
     * Liefert das occupancyMask-Array des Nachbar-Chunks,
     * der um (dx,dy) vom aktuellen Chunk verschoben ist.
     * Oder eine leere Maske (alles 0), wenn der Chunk nicht geladen ist.
     */
    private long[][] getNeighbourMask(int dx, int dy) {
        ClientChunk neigh = gameChunk.getWorld()
                                     .getChunk(
                                         gameChunk.getChunkX() + dx,
                                         gameChunk.getChunkY() + dy,
                                         gameChunk.getChunkZ()
                                     );
        if (neigh == null) {
            return new long[sx][sy];
        }
        return neigh.getOccupancyMask().occupancyMask;
    }

    public FaceMasks computeFaceMasks() {
        FaceMasks masks = new FaceMasks(sx, sy);

        // X±-Faces
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long curr = occupancyMask[x][y];
                long next = (x + 1 < sx)
                    ? occupancyMask[x + 1][y]
                    : getNeighbourMask(+1, 0)[0][y];        // immer [0][y] im Nachbar
                long prev = (x - 1 >= 0)
                    ? occupancyMask[x - 1][y]
                    : getNeighbourMask(-1, 0)[sx - 1][y];   // immer [sx-1][y] im Nachbar

                masks.xPlus[x][y]  = curr & ~next;
                masks.xMinus[x][y] = curr & ~prev;
            }
        }

        // Y±-Faces
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long curr = occupancyMask[x][y];
                long up   = (y + 1 < sy)
                    ? occupancyMask[x][y + 1]
                    : getNeighbourMask(0, +1)[x][0];
                long down = (y - 1 >= 0)
                    ? occupancyMask[x][y - 1]
                    : getNeighbourMask(0, -1)[x][sy - 1];

                masks.yPlus[x][y]  = curr & ~up;
                masks.yMinus[x][y] = curr & ~down;
            }
        }

        // Z±-Faces (bitwise in-col masks)
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long col = occupancyMask[x][y];
                masks.zPlus [x][y] = col & ~(col >>> 1);
                masks.zMinus[x][y] = col & ~(col << 1);
            }
        }

        return masks;
    }
}
