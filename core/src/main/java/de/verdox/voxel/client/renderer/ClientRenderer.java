package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.verdox.voxel.client.GameSession;
import de.verdox.voxel.client.input.ClientSettings;
import de.verdox.voxel.client.input.PlayerController;
import de.verdox.voxel.client.input.PlayerInteractionRayCast;
import de.verdox.voxel.client.level.world.WorldRenderPipeline;
import de.verdox.voxel.shared.util.Benchmark;
import lombok.Getter;

@Getter
public class ClientRenderer implements DebuggableOnScreen {
    private final DebugScreen debugScreen;
    private final Camera camera;
    private final ClientSettings clientSettings;
    private final PlayerController playerController;

    private final WorldRenderPipeline worldRenderPipeline;

    private final ShapeRenderer blockOutlineRenderer = new ShapeRenderer();
    private final ShapeRenderer chunkRegionOutlineRenderer = new ShapeRenderer();


    public ClientRenderer(Camera camera, ClientSettings clientSettings, PlayerController playerController) {
        this.camera = camera;
        this.clientSettings = clientSettings;
        this.playerController = playerController;

        debugScreen = new DebugScreen();

        this.worldRenderPipeline = new WorldRenderPipeline();
        debugScreen.attach(worldRenderPipeline);
        debugScreen.attach(this);

    }

    public void draw(Benchmark benchmark) {
        benchmark.startSection("GL-Clear");
        // 1) Framebuffer ganz oben einmal clearen
        Gdx.gl.glClearColor(0.2f, 0.4f, 0.6f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        benchmark.endSection();

        if (GameSession.getInstance().getCurrentWorld() == null) {
            return;
        }




        benchmark.startSection("Batch");
        benchmark.startSection("Batch start");
        //renderBatch.begin(camera);
        benchmark.endSection();
        this.worldRenderPipeline.renderWorld(camera, GameSession.getInstance().getCurrentWorld().getTerrainManager(), benchmark);
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
/*        if (VoxelClient.getInstance().getCurrentWorld() != null) {
            var storage = VoxelClient.getInstance().getCurrentWorld().getTerrainManager().getMeshStorage();
            debugScreen.addDebugTextLine("Queues: " + storage.getAmountOfQueues());
            debugScreen.addDebugTextLine("Terrain Graph Regions: " + VoxelClient.getInstance().getCurrentWorld().getTerrainManager().getTerrainGraph().getAmountOfRegions());
        }*/
    }
}
