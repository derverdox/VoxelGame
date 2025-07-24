package de.verdox.voxel.shared.util;

import lombok.Getter;
import org.joml.Vector3f;

@Getter
public enum Direction {
    WEST(0, -1, 0, 0,
            Constants.POS_Y,
            Constants.POS_Z
    ),
    EAST(1, 1, 0, 0,
            Constants.POS_Y,
            Constants.POS_Z
    ),
    DOWN(2, 0, -1, 0,
            Constants.POS_X,
            Constants.POS_Z
    ),
    UP(3, 0, 1, 0,
            Constants.POS_X,
            Constants.POS_Z
    ),
    NORTH(4, 0, 0, -1,
            Constants.POS_X,
            Constants.POS_Y
    ),
    SOUTH(5, 0, 0, 1,
            Constants.POS_X,
            Constants.POS_Y
    );

    private final byte id;
    private final byte nx;
    private final byte ny;
    private final byte nz;
    private final float[] uDirection;
    private final float[] vDirection;

    Direction(int id, int nx, int ny, int nz, float[] uDirection, float[] vDirection) {
        this.id = (byte) id;
        this.nx = (byte) nx;
        this.ny = (byte) ny;
        this.nz = (byte) nz;
        this.uDirection = uDirection;
        this.vDirection = vDirection;
    }

    public Direction getOpposite() {
        return fromOffsets(nx * -1, ny * -1, nz * -1);
    }

    public byte getOffsetX() {
        return nx;
    }

    public byte getOffsetY() {
        return ny;
    }

    public byte getOffsetZ() {
        return nz;
    }

    public Vector3f uvAxis() {
        return new Vector3f(nx == 0 ? 1 : 0, ny == 0 ? 1 : 0, nz == 0 ? 1 : 0);
    }

    /**
     * Liefert die Direction, die genau diesen Offsets entspricht.
     *
     * @throws IllegalArgumentException bei ungültigen Offsets
     */
    public static Direction fromOffsets(int dx, int dy, int dz) {
        for (Direction dir : values()) {
            if (dir.nx == dx && dir.ny == dy && dir.nz == dz) {
                return dir;
            }
        }
        throw new IllegalArgumentException(
                "No direction for offsets: (" + dx + "," + dy + "," + dz + ")"
        );
    }

    public boolean isNegative() {
        return nx < 0 || ny < 0 || nz < 0;
    }

    public boolean isOnX() {
        return nx != 0;
    }

    public boolean isOnY() {
        return ny != 0;
    }

    public boolean isOnZ() {
        return nz != 0;
    }

    @Override
    public String toString() {
        return this.name() + " [" + id + "]" + "(" + nx + ", " + ny + ", " + nz + ")";
    }

    private static class Constants {
        public static final float[] POS_X = {1, 0, 0};
        public static final float[] NEG_X = {-1, 0, 0};

        public static final float[] POS_Y = {0, 1, 0};
        public static final float[] NEG_Y = {0, -1, 0};

        public static final float[] POS_Z = {0, 0, 1};
        public static final float[] NEG_Z = {0, 0, -1};
    }
}
