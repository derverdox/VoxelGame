package de.verdox.voxel.shared.level.entity;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3f;

public abstract class Entity {
    private final Vector3f position = new Vector3f();
    private final Vector3f movementVector = new Vector3f();
    private float yaw;
    private float pitch;
    @Setter
    @Getter
    private float speed = 5f;

    public void setPosition(double x, double y, double z) {
        this.position.set((float) x, (float) y, (float) z);
    }

    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
    }

    public Vector3f getPosition() {
        return new Vector3f(position); // defensive copy
    }

    public void render(float deltaTime) {
        movementVector.mul(deltaTime);
        yaw *= deltaTime;
        pitch *= deltaTime;
    }
}
