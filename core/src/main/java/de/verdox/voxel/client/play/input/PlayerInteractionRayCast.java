package de.verdox.voxel.client.play.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint3;
import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.renderer.debug.DebugScreen;
import de.verdox.voxel.client.renderer.debug.DebuggableOnScreen;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.Chunk;

public class PlayerInteractionRayCast implements DebuggableOnScreen {
    private static final float RAY_CAST_STEP_SIZE = 0.05f;
    private static final float PLAYER_INTERACTION_RANGE = 8f;

    private final Vector3 step = new Vector3();
    private final Vector3 rayPos = new Vector3();
    private final GridPoint3 block = new GridPoint3();

    private BlockRayCastResult lastHit;
    private BlockRayCastResult blockOfPlayerPos;

    /**
     * Schießt einen Strahl aus der Kamera und liefert den ersten Block (BlockBase),
     * der nicht AIR ist. Rückgabe null, wenn kein Block innerhalb der Reichweite getroffen wurde.
     */
    public BlockRayCastResult rayCastNearestBlockTarget(Camera camera, ClientWorld world) {
        step.set(camera.direction).nor().scl(RAY_CAST_STEP_SIZE);
        rayPos.set(camera.position);

        blockOfPlayerPos = new BlockRayCastResult(world, world.getBlockAt((int) camera.position.x, (int) camera.position.y, (int) camera.position.z), (int) camera.position.x, (int) camera.position.y, (int) camera.position.z);

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

                lastHit = new BlockRayCastResult(world, hit, block.x, block.y, block.z);
                return lastHit;
            }
        }

        // Nichts getroffen
        return null;
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        if (lastHit != null) {
            debugScreen.addDebugTextLine("Block [" + lastHit.globalX + ", " + lastHit.globalY + ", " + lastHit.globalZ + "]: " + lastHit.castBlock.findKey());
            debugScreen.addDebugTextLine(" ".repeat(4) + "Chunk: "+ Chunk.chunkX(lastHit.world, lastHit.globalX)+", "+ Chunk.chunkY(lastHit.world, lastHit.globalY)+", "+ Chunk.chunkZ(lastHit.world, lastHit.globalZ));
            debugScreen.addDebugTextLine(" ".repeat(4) + "Sky Light: "+ lastHit.getSkyLightOfHitBlock());
            debugScreen.addDebugTextLine(" ".repeat(4) + "Block Light: ("+ lastHit.getBlockLightRedOfHitBlock()+", "+lastHit.getBlockLightGreenOfHitBlock()+", "+lastHit.getBlockLightBlueOfHitBlock()+")");
        }
    }

    public record BlockRayCastResult(ClientWorld world, BlockBase castBlock, int globalX, int globalY, int globalZ) {
        public void render(Camera camera, ShapeRenderer shapeRenderer) {
            Gdx.graphics.getGL20().glLineWidth(1);
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.box(globalX, globalY, (globalZ + 1), 1f, 1f, 1f);
            shapeRenderer.end();
        }

        public Chunk getChunk() {
            return world.getChunkNow(Chunk.chunkX(world, globalX), Chunk.chunkY(world, globalY), Chunk.chunkZ(world, globalZ));
        }

        public byte getSkyLightOfHitBlock() {
            Chunk chunk = getChunk();
            if(chunk == null) {
                return -1;
            }
            return chunk.getChunkLightData().getSkyLight((byte) chunk.localX(globalX), (byte) chunk.localY(globalY), (byte) chunk.localZ(globalZ));
        }

        public byte getBlockLightRedOfHitBlock() {
            Chunk chunk = getChunk();
            if(chunk == null) {
                return -1;
            }
            return chunk.getChunkLightData().getBlockRed((byte) chunk.localX(globalX), (byte) chunk.localY(globalY), (byte) chunk.localZ(globalZ));
        }

        public byte getBlockLightGreenOfHitBlock() {
            Chunk chunk = getChunk();
            if(chunk == null) {
                return -1;
            }
            return chunk.getChunkLightData().getBlockGreen((byte) chunk.localX(globalX), (byte) chunk.localY(globalY), (byte) chunk.localZ(globalZ));
        }

        public byte getBlockLightBlueOfHitBlock() {
            Chunk chunk = getChunk();
            if(chunk == null) {
                return -1;
            }
            return chunk.getChunkLightData().getBlockBlue((byte) chunk.localX(globalX), (byte) chunk.localY(globalY), (byte) chunk.localZ(globalZ));
        }
    }
}
