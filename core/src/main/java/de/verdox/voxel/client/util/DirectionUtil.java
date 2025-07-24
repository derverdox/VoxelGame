package de.verdox.voxel.client.util;

import de.verdox.voxel.shared.util.Direction;

public class DirectionUtil {
    public static float[] getUDirByFaceDirection(Direction direction) {
        return switch (direction) {
            case WEST -> new float[]{0, -1, 0};
            case EAST -> new float[]{0, -1, 0};
            case DOWN -> new float[]{0, 0, -1};
            case UP -> new float[]{0, 0, 1};
            case NORTH -> new float[]{0, -1, 0};
            case SOUTH -> new float[]{0, -1, 0};
        };
    }

    public static float[] getVDirByFaceDirection(Direction direction) {
        return switch (direction) {
            case WEST -> new float[]{0, 0, 1};
            case EAST -> new float[]{0, 0, -1};
            case DOWN -> new float[]{1, 0, 0};
            case UP -> new float[]{1, 0, 0};
            case NORTH -> new float[]{1, 0, 0};
            case SOUTH -> new float[]{1, 0, 0};
        };
    }
}
