package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;

@Deprecated
public class PlayerCamera {
    /**
     * -- GETTER --
     * Direkter Zugriff auf die interne libGDX-Kamera.
     */
    @Getter
    private final PerspectiveCamera camera;

    // Winkel in Grad
    private float yaw;
    private float pitch;

    // Limits für Pitch (um "Gimbal Lock" zu verhindern)
    private final float minPitch;
    private final float maxPitch;

    /**
     * Erzeugt eine neue Kamera.
     *
     * @param fovY           Sichtfeld in Y
     * @param viewportWidth  Breite des Viewports
     * @param viewportHeight Höhe des Viewports
     * @param near           Near-Plane
     * @param far            Far-Plane
     * @param minPitch       Minimum für Blick nach unten (z.B. -89)
     * @param maxPitch       Maximum für Blick nach oben (z.B. 89)
     */
    public PlayerCamera(float fovY,
                        float viewportWidth,
                        float viewportHeight,
                        float near,
                        float far,
                        float minPitch,
                        float maxPitch) {
        camera = new PerspectiveCamera(fovY, viewportWidth, viewportHeight);
        camera.near = near;
        camera.far = far;
        camera.up.set(0f, 1f, 0f); // Welt-Up
        camera.update();

        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
    }

    /**
     * Yaw um die Welt-Up-Achse (Y). Keine Roll-Komponente.
     *
     * @param delta Winkeländerung in Grad
     */
    public void yaw(float delta) {
        yaw = (yaw + delta) % 360f;
        updateOrientation();
    }

    /**
     * Pitch um die Kamer-Seitenachse. Dabei auf [minPitch, maxPitch] geclamped.
     *
     * @param delta Winkeländerung in Grad
     */
    public void pitch(float delta) {
        pitch = MathUtils.clamp(pitch + delta, minPitch, maxPitch);
        updateOrientation();
    }

    /**
     * Setzt die Kamera-Position und updated Matrices.
     */
    public void setPosition(float x, float y, float z) {
        camera.position.set(x, y, z);
        camera.update();
    }

    /**
     * Intern: Berechnet direction aus yaw/pitch und updated die Camera.
     */
    private void updateOrientation() {
        float radYaw = yaw * MathUtils.degreesToRadians;
        float radPitch = pitch * MathUtils.degreesToRadians;

        // Sphärische Koordinaten → Kartesisch
        Vector3 dir = new Vector3();
        dir.x = MathUtils.cos(radPitch) * MathUtils.sin(radYaw);
        dir.y = MathUtils.sin(radPitch);
        dir.z = MathUtils.cos(radPitch) * MathUtils.cos(radYaw);
        dir.nor();

        camera.direction.set(dir);
        // Up bleibt (0,1,0) ohne Roll
        camera.up.set(0f, 1f, 0f);
        camera.update();
    }

    /**
     * Optionale Bewegung: Vorwärts/Rückwärts in Blickrichtung bei distance.
     */
    public void moveForward(float distance) {
        Vector3 tmp = new Vector3(camera.direction).scl(distance);
        camera.position.add(tmp);
        camera.update();
    }

    /**
     * Seitwärtsbewegung (Strafe) relativ zu Blickrichtung.
     */
    public void strafe(float distance) {
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor();
        camera.position.add(right.scl(distance));
        camera.update();
    }
}
