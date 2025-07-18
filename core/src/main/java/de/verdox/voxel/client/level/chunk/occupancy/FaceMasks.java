package de.verdox.voxel.client.level.chunk.occupancy;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.util.Direction;

public class FaceMasks {
    public long[][] xPlus, xMinus, yPlus, yMinus, zPlus, zMinus;
    private final int sx;
    private final int sy;

    public FaceMasks(int sx, int sy) {
        this.sx = sx;
        this.sy = sy;
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

    public void computeMasks(ClientWorld world, int chunkX, int chunkY, int chunkZ, long[][] occupancyMask) {
        initMasks();

        long[][] xPosN = getNeighbourMask(world, chunkX, chunkY, chunkZ, 1, 0);
        long[][] xNegN = getNeighbourMask(world, chunkX, chunkY, chunkZ, -1, 0);
        long[][] yPosN = getNeighbourMask(world, chunkX, chunkY, chunkZ, 0, 1);
        long[][] yNegN = getNeighbourMask(world, chunkX, chunkY, chunkZ, 0, -1);

        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long curr = occupancyMask[x][y];
                long next = (x + 1 < sx) ?
                    occupancyMask[x + 1][y] :
                    xPosN != null ? xPosN[0][y] : 0;
                long prev = (x - 1 >= 0)
                    ? occupancyMask[x - 1][y]
                    : xNegN != null ? xNegN[sx - 1][y] : 0;

                xPlus[x][y] = curr & ~next;
                xMinus[x][y] = curr & ~prev;
            }
        }

        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long curr = occupancyMask[x][y];
                long up = (y + 1 < sy)
                    ? occupancyMask[x][y + 1]
                    : yPosN != null ? yPosN[x][0] : 0;
                long down = (y - 1 >= 0)
                    ? occupancyMask[x][y - 1]
                    : yNegN != null ? yNegN[x][sy - 1] : 0;

                yPlus[x][y] = curr & ~up;
                yMinus[x][y] = curr & ~down;
            }
        }

        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                long col = occupancyMask[x][y];
                zPlus[x][y] = col & ~(col >>> 1);
                zMinus[x][y] = col & ~(col << 1);
            }
        }
    }

    private void initMasks() {
        this.xPlus = new long[sx][sy];
        this.xMinus = new long[sx][sy];

        this.yPlus = new long[sx][sy];
        this.yMinus = new long[sx][sy];

        this.zPlus = new long[sx][sy];
        this.zMinus = new long[sx][sy];
    }

    public void clearMasks() {
        this.xPlus = null;
        this.xMinus = null;
        this.yPlus = null;
        this.yMinus = null;
        this.zPlus = null;
        this.zMinus = null;
    }

    private long[][] getNeighbourMask(ClientWorld world, int chunkX, int chunkY, int chunkZ, int dx, int dy) {
        ClientChunk neigh = world
            .getChunk(
                chunkX + dx,
                chunkY + dy,
                chunkZ
            );
        if (neigh == null) {
            return null;
        }
        return neigh.getOccupancyMask().getOccupancyMask();
    }
}
