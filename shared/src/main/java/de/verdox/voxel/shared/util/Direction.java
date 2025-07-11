package de.verdox.voxel.shared.util;

import lombok.Getter;
import org.joml.Vector3f;

public enum Direction {
    WEST(0, -1, 0, 0),
    EAST(1, 1, 0, 0),
    DOWN(2, 0, -1, 0),
    UP(3, 0, 1, 0),
    NORTH(4, 0, 0, -1),
    SOUTH(5, 0, 0, 1);

    @Getter
    private final int id;
    private final int dx;
    private final int dy;
    private final int dz;

    Direction(int id, int dx, int dy, int dz) {
        this.id = id;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int getOffsetX() {
        return dx;
    }

    public int getOffsetY() {
        return dy;
    }

    public int getOffsetZ() {
        return dz;
    }

    public Vector3f uvAxis() {
        return new Vector3f(dx == 0 ? 1 : 0, dy == 0 ? 1 : 0, dz == 0 ? 1 : 0);
    }

    /**
     * Liefert die Direction, die genau diesen Offsets entspricht.
     *
     * @throws IllegalArgumentException bei ungültigen Offsets
     */
    public static Direction fromOffsets(int dx, int dy, int dz) {
        for (Direction dir : values()) {
            if (dir.dx == dx && dir.dy == dy && dir.dz == dz) {
                return dir;
            }
        }
        throw new IllegalArgumentException(
            "No direction for offsets: (" + dx + "," + dy + "," + dz + ")"
        );
    }
}
