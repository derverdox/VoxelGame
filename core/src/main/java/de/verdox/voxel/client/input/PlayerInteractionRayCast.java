package de.verdox.voxel.client.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.level.block.BlockBase;

public class PlayerInteractionRayCast {
    private static final float RAY_CAST_STEP_SIZE = 0.05f;
    private static final float PLAYER_INTERACTION_RANGE = 8f;

    private final Vector3 step = new Vector3();
    private final Vector3 rayPos = new Vector3();
    private final GridPoint3 block = new GridPoint3();

    private BlockRayCastResult lastHit;

    /**
     * Schießt einen Strahl aus der Kamera und liefert den ersten Block (BlockBase),
     * der nicht AIR ist. Rückgabe null, wenn kein Block innerhalb der Reichweite getroffen wurde.
     */
    public BlockRayCastResult rayCastNearestBlockTarget(Camera camera, ClientWorld world) {
        step.set(camera.direction).nor().scl(RAY_CAST_STEP_SIZE);
        rayPos.set(camera.position);

        float traveled = 0f;
        while (traveled < PLAYER_INTERACTION_RANGE) {
            rayPos.add(step);
            traveled += RAY_CAST_STEP_SIZE;

            block.x = (int) Math.floor(rayPos.x);
            block.y = (int) Math.floor(rayPos.y);
            block.z = (int) Math.floor(rayPos.z);

            BlockBase hit = world.getBlockAt(block.x, block.y, block.z);
            if (hit != null && hit.isInteractableByRayCast()) {

                if (lastHit != null && this.lastHit.globalX == block.x && this.lastHit.globalY == block.y && this.lastHit.globalZ == block.z) {
                    return lastHit;
                }

                lastHit = new BlockRayCastResult(hit, block.x, block.y, block.z);
                return lastHit;
            }
        }

        // Nichts getroffen
        return null;
    }

    public record BlockRayCastResult(BlockBase castBlock, int globalX, int globalY, int globalZ) {
        public void render(Camera camera, ShapeRenderer shapeRenderer) {
            Gdx.graphics.getGL20().glLineWidth(4);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            // Align correctly with LibGDX cube shape
            shapeRenderer.box(globalX, globalY, (globalZ + 1), 1.02f, 1.02f, 1.02f);
            shapeRenderer.end();
        }

    }
}
