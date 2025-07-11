package de.verdox.voxel.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.PerformanceCounter;
import com.esotericsoftware.kryonet.Client;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.input.ClientSettings;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.client.network.ClientConnectionListener;
import de.verdox.voxel.client.renderer.ClientRenderer;
import de.verdox.voxel.client.input.PlayerController;
import de.verdox.voxel.shared.data.types.BlockModels;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.Bootstrap;
import lombok.Getter;
import org.joml.Vector3i;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class ClientBase extends ApplicationAdapter implements DebuggableOnScreen {
    private static final int TICKS_PER_SECOND = 20;
    private float accumulator = 0f;
    private long clientTick = 0;

    public static Client client;
    public static ClientRenderer clientRenderer;
    public static ClientConnectionListener clientConnectionListener;
    public static ClientSettings clientSettings = new ClientSettings();

    private PlayerController playerController;
    @Getter
    private static long lastRenderCPUDurationNanos;
    private static long lastSimulationCPUDurationNanos;
    private int chunkX;
    private int chunkY;
    private int chunkZ;
    private final PerformanceCounter performanceCounter = new PerformanceCounter("");

    @Override
    public void create() {
        int writeBufferSize = 1024 * 1024 * 1024;   // z.B. 32 KB
        int objectBufferSize = 1024 * 1024 * 1024;   // z.B. 64 KB

        client = new Client(writeBufferSize, objectBufferSize);
        client.start();
        Bootstrap.bootstrap(client.getKryo());
        Gdx.input.setCursorCatched(true);
        Gdx.graphics.setVSync(false);
        Gdx.graphics.setResizable(true);

        Gdx.graphics.setForegroundFPS(0);

        BlockModels.bootstrap();
        TextureAtlasManager.getInstance().build();

        Camera camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 512 * 16;

        camera.position.set(new Vector3(0, 80, 0));
        playerController = new PlayerController(camera, client);
        clientRenderer = new ClientRenderer(camera, clientSettings, playerController);
        clientRenderer.getDebugScreen().attach(playerController);
        clientRenderer.getDebugScreen().attach(this);

        try {
            client.connect(5000, "localhost", 54555, 54777);
        } catch (IOException e) {
            e.printStackTrace();
        }


        clientConnectionListener = new ClientConnectionListener(clientRenderer);
        client.addListener(clientConnectionListener);
    }

    @Override
    public void pause() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void resume() {
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render() {
        long startSimulation = System.nanoTime();
        float delta = Gdx.graphics.getDeltaTime();
        accumulator += delta;

        // 1) Simulations-Ticks
        float tickRate = 1f / TICKS_PER_SECOND;
        while (accumulator >= tickRate) {
            updateGameLogic(tickRate);
            accumulator -= tickRate;
            clientTick++;
        }
        lastSimulationCPUDurationNanos = System.nanoTime() - startSimulation;

        // 2) Interpoliertes Rendering
        float alpha = accumulator / tickRate;

        long startRender = System.nanoTime();
        renderScene(alpha);
        lastRenderCPUDurationNanos = System.nanoTime() - startRender;
    }

    private void updateGameLogic(float dt) {
        ClientWorld current = VoxelClient.getInstance().getCurrentWorld();
        if (current == null) {
            return;
        }

        float x = playerController.getCamera().position.x;
        float y = playerController.getCamera().position.y;
        float z = playerController.getCamera().position.z;

        int chunkX = ChunkBase.chunkX(current, (int) x);
        int chunkY = ChunkBase.chunkY(current, (int) y);
        int chunkZ = ChunkBase.chunkZ(current, (int) z);

        if (chunkX != this.chunkX || chunkY != this.chunkY || chunkZ != this.chunkZ) {
            //current.getChunkVisibilityGraph().getChunkRenderRegionManager().updateRegionsAround(current, chunkX, chunkY, chunkZ);

            List<Vector3i> chunksInView = new ArrayList<>();
            int horizontalRadius = clientSettings.horizontalViewDistance / 2;
            int verticalRadius = clientSettings.verticalViewDistance / 2;

            for (int rx = chunkX - horizontalRadius; rx <= chunkX + horizontalRadius; rx++) {
                for (int ry = chunkY - verticalRadius; ry <= chunkY + verticalRadius; ry++) {
/*                    if (ry < current.getMinChunkY() || ry > current.getMaxChunkY()) {
                        continue;
                    }*/
                    for (int rz = chunkZ - horizontalRadius; rz <= chunkZ + horizontalRadius; rz++) {
                        chunksInView.add(new Vector3i(rx, ry, rz));
                    }
                }
            }

            // Nach quadratischer Entfernung sortieren
            chunksInView.sort(Comparator.comparingInt(coord -> {
                int dx = coord.x - chunkX;
                int dy = coord.y - chunkY;
                int dz = coord.z - chunkZ;
                return dx * dx + dy * dy + dz * dz;
            }));

            // Dann in ein LinkedHashSet (erhält Reihenfolge) oder gleich als List lassen
            LinkedHashSet<Vector3i> orderedChunks = new LinkedHashSet<>(chunksInView);

            // Übergabe an RequestManager
            current.getChunkRequestManager().setChunksToRequest(orderedChunks);

            this.chunkX = chunkX;
            this.chunkY = chunkY;
            this.chunkZ = chunkZ;
        }

        current.getChunkRequestManager().update(dt);
    }

    private void renderScene(float alpha) {
        // • Kamera interpoliert bewegen
        // • Chunks/Entities zeichnen mit interpolierten Positionen
        playerController.update(Gdx.graphics.getDeltaTime());
        clientRenderer.draw();
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        var renderMillis = TimeUnit.NANOSECONDS.toMillis(lastRenderCPUDurationNanos);
        var simMillis = TimeUnit.NANOSECONDS.toMillis(lastSimulationCPUDurationNanos);

        if (renderMillis > 0) {
            debugScreen.addDebugTextLine("Rendering: " + renderMillis + " ms");
        } else {
            debugScreen.addDebugTextLine("Rendering: " + lastRenderCPUDurationNanos + " ns");
        }

        if (simMillis > 0) {
            debugScreen.addDebugTextLine("Simulation: " + simMillis + " ms");
        } else {
            debugScreen.addDebugTextLine("Simulation: " + lastSimulationCPUDurationNanos + " ns");
        }
    }
}
