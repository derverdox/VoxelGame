package de.verdox.voxel.client.input;

import com.badlogic.gdx.Input;

public class ClientSettings {
    // # Controls
    public int forwardKey = Input.Keys.W;
    public int backwardKey = Input.Keys.S;
    public int leftKey = Input.Keys.A;
    public int rightKey = Input.Keys.D;
    public int jumpKey = Input.Keys.SPACE;
    public int sneakKey = Input.Keys.SHIFT_LEFT;

    // Rendering
    public int horizontalViewDistance = 16;
    public int verticalViewDistance = 16;
    public boolean useOcclusionCulling = false;

    public boolean useMinMaxHeightImprovement = true;
}

