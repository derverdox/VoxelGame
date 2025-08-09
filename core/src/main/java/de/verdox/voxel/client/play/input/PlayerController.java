package de.verdox.voxel.client.play.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Client;
import de.verdox.voxel.client.GameSession;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import lombok.Getter;

@Getter
public class PlayerController implements DebuggableOnScreen {
    private final Camera camera;
    private final Client kryoClient;

    private final Vector3 position = new Vector3();
    private final Vector3 movementVec = new Vector3();
    private final PlayerInteractionRayCast playerInteractionRayCast = new PlayerInteractionRayCast();

    private float yaw;
    private float pitch;

    private final float speed = 5f;
    private final float mouseSensitivity = 0.2f;  // degree per pixel
    private final float maxPitch = 89f;
    private final float minPitch = -89f;
    private PlayerInteractionRayCast.BlockRayCastResult blockInteractionResult;

    public PlayerController(Camera camera, Client kryoClient) {
        this.camera = camera;
        this.kryoClient = kryoClient;
        // Startwerte aus initialer Kameraausrichtung gewinnen
        Vector3 dir = camera.direction;
        this.yaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));
        this.pitch = (float) Math.toDegrees(Math.asin(dir.y));
        this.position.set(camera.position);
        updateCamera();
    }

    /**
     * In Render-/Update-Schleife aufrufen.
     *
     * @param delta Zeit seit letztem Frame in Sekunden
     */
    public void update(float delta) {
        // 1) Maus bewegen → Yaw/Pitch
        float dx = Gdx.input.getDeltaX();
        float dy = Gdx.input.getDeltaY();
        yaw += -dx * mouseSensitivity;
        pitch -= dy * mouseSensitivity;
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
        updateCamera();

        // 2) Tastaturbewegung (WASD)
        movementVec.set(0, 0, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.W)) movementVec.z += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) movementVec.z -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) movementVec.x -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) movementVec.x += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) movementVec.y += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) movementVec.y -= 1f;

        if (!movementVec.isZero()) {
            movementVec.nor().scl(speed * delta);
            // Bewegungsvektor relativ zur Kamera drehen: Transform XZ-Ebene
            Vector3 forward = new Vector3(camera.direction.x, 0, camera.direction.z).nor();
            Vector3 right = forward.cpy().crs(camera.up).nor();
            Vector3 moveDir = new Vector3();
            moveDir.mulAdd(forward, movementVec.z);
            moveDir.mulAdd(right, movementVec.x);
            moveDir.mulAdd(camera.up, movementVec.y);
            position.add(moveDir);
            camera.position.set(position);
        }

        if (GameSession.getInstance().getCurrentWorld() != null) {
            blockInteractionResult = playerInteractionRayCast.rayCastNearestBlockTarget(this.camera, GameSession.getInstance().getCurrentWorld());
        }

        // 4) Position & Blick an Server senden
        sendPosition();
    }

    /**
     * Erzeugt Direction aus yaw/pitch und setzt in Camera.
     */
    private void updateCamera() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        Vector3 dir = new Vector3();
        dir.x = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        dir.y = (float) (Math.sin(radPitch));
        dir.z = (float) (Math.cos(radYaw) * Math.cos(radPitch));
        dir.nor();
        camera.direction.set(dir);
        camera.up.set(0f, 1f, 0f);
    }

    /**
     * Sendet aktuelle Position + Blickwinkel an den Server.
     */
    private void sendPosition() {
/*        PlayerPositionPacket packet = new PlayerPositionPacket();
        packet.x     = position.x;
        packet.y     = position.y;
        packet.z     = position.z;
        packet.yaw   = yaw;
        packet.pitch = pitch;
        kryoClient.sendTCP(packet);*/
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine("FPS: " + Gdx.graphics.getFramesPerSecond());
        debugScreen.addDebugTextLine("Pos: " + position + ", Yaw: " + yaw + ", Pitch: " + pitch);
        debugScreen.addDebugTextLine("Velocity: " + movementVec);
    }
}
