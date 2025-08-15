package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.math.MathUtils;
import de.verdox.voxel.shared.util.Direction;

public class GraphicalConstants {
    public static final int MAX_BYTE_SIZE_SHADER_COORDINATES = 8;
    public static final int DIRECTION_BIT_SIZE = (int) Math.ceil(MathUtils.log2(Direction.values().length));
}
