package de.verdox.voxel.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.esotericsoftware.kryonet.Client;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.input.ClientSettings;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.client.network.ClientConnectionListener;
import de.verdox.voxel.client.renderer.ClientRenderer;
import de.verdox.voxel.client.input.PlayerController;
import de.verdox.voxel.shared.data.types.BlockModels;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.Bootstrap;
import de.verdox.voxel.shared.network.packet.serializer.ChunkSerializer;
import de.verdox.voxel.shared.util.Benchmark;
import lombok.Getter;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

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
    private int chunkX;
    private int chunkY;
    private int chunkZ;

    private static int frameCounter = 0;
    private static int benchmarkWindow = 60 * 30;
    private final Benchmark benchmark = new Benchmark(benchmarkWindow);
    private List<String> benchmarkInfoSampled = new ArrayList<>();
    private GLProfiler worldRendererProfiler;


    @Override
    public void create() {
        int writeBufferSize = 1024 * 1024 * 16;   // z.B. 32 KB
        int objectBufferSize = 1024 * 1024 * 16;   // z.B. 64 KB

        Shaders.initShaders();

        client = new Client(writeBufferSize, objectBufferSize);
        client.start();
        Bootstrap.bootstrap(client.getKryo());

        client.getKryo().register(ClientChunk.class, new ChunkSerializer<>());

        Gdx.input.setCursorCatched(true);
        Gdx.graphics.setVSync(false);
        Gdx.graphics.setResizable(true);

        Gdx.graphics.setForegroundFPS(0);

        IntBuffer depthBits = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL20.GL_DEPTH_BITS, depthBits);
        Gdx.app.log("DEPTH", "Depth bits: " + depthBits.get(0));

        BlockModels.bootstrap();
        TextureAtlasManager.getInstance().build();

        Camera camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 512 * 16;

        camera.position.set(new Vector3(0, 80, 0));
        playerController = new PlayerController(camera, client);
        clientRenderer = new ClientRenderer(camera, clientSettings, playerController);
        clientRenderer.getDebugScreen().attach(playerController);
        clientRenderer.getDebugScreen().attach(playerController.getPlayerInteractionRayCast());
        clientRenderer.getDebugScreen().attach(this);

        try {
            client.connect(5000, "localhost", 54000);
        } catch (IOException e) {
            e.printStackTrace();
        }


        clientConnectionListener = new ClientConnectionListener(clientRenderer);
        client.addListener(clientConnectionListener);
        worldRendererProfiler = new GLProfiler(Gdx.graphics);
        worldRendererProfiler.enable();
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
        benchmark.start();
        benchmark.startSection("Simulation");

        float delta = Gdx.graphics.getDeltaTime();
        accumulator += delta;

        // 1) Simulations-Ticks
        float tickRate = 1f / TICKS_PER_SECOND;
        while (accumulator >= tickRate) {
            updateGameLogic(tickRate, benchmark);

            accumulator -= tickRate;
            clientTick++;
        }
        float alpha = accumulator / tickRate;
        benchmark.endSection();

        benchmark.startSection("Rendering");
        renderScene(alpha, benchmark);
        benchmark.endSection();
        benchmark.end();

        clientRenderer.getDebugScreen().render();
        frameCounter++;

        worldRendererProfiler.reset();
    }

    private void updateGameLogic(float dt, Benchmark benchmark) {
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
            this.chunkX = chunkX;
            this.chunkY = chunkY;
            this.chunkZ = chunkZ;
            current.getChunkRequestManager().changeCenter(chunkX, chunkY, chunkZ);
        }
    }

    private void renderScene(float alpha, Benchmark benchmark) {
        benchmark.startSection("PlayerController");
        playerController.update(Gdx.graphics.getDeltaTime());
        benchmark.endSection();
        benchmark.startSection("Draw");
        clientRenderer.draw(benchmark);
        benchmark.endSection();
    }

    @Override
    public void debugText(DebugScreen debugScreen) {

        debugScreen.addDebugTextLine("Draw calls: " + worldRendererProfiler.getDrawCalls());
        debugScreen.addDebugTextLine("GL calls: " + worldRendererProfiler.getCalls());
        debugScreen.addDebugTextLine("Shader switches: " + worldRendererProfiler.getShaderSwitches());
        debugScreen.addDebugTextLine("Vertex count: " + worldRendererProfiler.getVertexCount().total);
        debugScreen.addDebugTextLine("Texture bindings: " + worldRendererProfiler.getTextureBindings());

        if (frameCounter % benchmarkWindow == 0) {
            benchmarkInfoSampled = benchmark.printToLines("Draw");
            frameCounter = 0;
        }

        for (String draw : benchmarkInfoSampled) {
            debugScreen.addDebugTextLine(draw);
        }
    }
}
