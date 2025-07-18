package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.BufferUtils;
import de.verdox.voxel.client.VoxelClient;
import de.verdox.voxel.client.input.ClientSettings;
import de.verdox.voxel.client.input.PlayerController;
import de.verdox.voxel.client.input.PlayerInteractionRayCast;
import de.verdox.voxel.client.level.world.RegionBasedWorldRenderPipeline;
import de.verdox.voxel.client.level.world.WorldRenderPipeline;
import de.verdox.voxel.client.renderer.level.WorldRenderer;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.util.Benchmark;
import lombok.Getter;

import java.nio.IntBuffer;

@Getter
public class ClientRenderer implements DebuggableOnScreen {
    private final DebugScreen debugScreen;
    private final Camera camera;
    private final ClientSettings clientSettings;
    private final PlayerController playerController;

    private final WorldRenderer worldRenderer;
    private final WorldRenderPipeline worldRenderPipeline;

    private ModelBatch renderBatch;
    private Environment environment;
    private final ShapeRenderer blockOutlineRenderer = new ShapeRenderer();
    private final ShapeRenderer chunkRegionOutlineRenderer = new ShapeRenderer();


    public ClientRenderer(Camera camera, ClientSettings clientSettings, PlayerController playerController) {
        this.camera = camera;
        this.clientSettings = clientSettings;
        this.playerController = playerController;

        renderBatch = new ModelBatch();
        debugScreen = new DebugScreen();

        this.worldRenderer = new WorldRenderer();
        this.worldRenderPipeline = new RegionBasedWorldRenderPipeline();
        debugScreen.attach(worldRenderer);
        debugScreen.attach(worldRenderPipeline);
        debugScreen.attach(this);

    }

    public void draw(Benchmark benchmark) {
        benchmark.startSection("GL-Clear");
        // 1) Framebuffer ganz oben einmal clearen
        Gdx.gl.glClearColor(0.2f, 0.4f, 0.6f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        benchmark.endSection();

        if (VoxelClient.getInstance().getCurrentWorld() == null) {
            return;
        }

        if (environment == null) {
            environment = WorldRenderer.createEnvironment(VoxelClient.getInstance().getCurrentWorld());
        }


        benchmark.startSection("Batch");
        benchmark.startSection("Batch start");
        //renderBatch.begin(camera);
        benchmark.endSection();
        this.worldRenderPipeline.renderWorld(camera, VoxelClient.getInstance().getCurrentWorld(), renderBatch, environment, benchmark);
        benchmark.startSection("Batch End");
        //renderBatch.end();
        benchmark.endSection();
        benchmark.endSection();

        benchmark.startSection("Player ray cast");
        PlayerInteractionRayCast.BlockRayCastResult rayCastResult = playerController.getBlockInteractionResult();
        if (rayCastResult != null) {
            rayCastResult.render(camera, blockOutlineRenderer);
        }
        benchmark.endSection();
        camera.update();
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        if (VoxelClient.getInstance().getCurrentWorld() != null) {
            var storage = VoxelClient.getInstance().getCurrentWorld().getTerrainManager().getMeshStorage();
            debugScreen.addDebugTextLine("Queues: " + storage.getAmountOfQueues());
        }
    }
}
