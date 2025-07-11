package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.verdox.voxel.client.VoxelClient;
import de.verdox.voxel.client.input.ClientSettings;
import de.verdox.voxel.client.input.PlayerController;
import de.verdox.voxel.client.input.PlayerInteractionRayCast;
import de.verdox.voxel.client.level.mesh.region.ChunkRenderRegion;
import de.verdox.voxel.client.level.world.RegionBasedWorldRenderPipeline;
import de.verdox.voxel.client.level.world.WorldRenderPipeline;
import de.verdox.voxel.client.renderer.level.WorldRenderer;
import lombok.Getter;

@Getter
public class ClientRenderer {
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
        debugScreen.attach(worldRenderer.getMeshMaster());
    }

    public void draw() {
        Gdx.gl.glClearColor(0.2f, 0.4f, 0.6f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (VoxelClient.getInstance().getCurrentWorld() == null) {
            debugScreen.render();
            return;
        }

        if (environment == null) {
            environment = WorldRenderer.createEnvironment(VoxelClient.getInstance().getCurrentWorld());
        }


        renderBatch.begin(camera);
        this.worldRenderPipeline.renderWorld(camera, VoxelClient.getInstance().getCurrentWorld(), renderBatch, environment);
        worldRenderer.renderWorld(camera, VoxelClient.getInstance().getCurrentWorld(), (int) camera.position.x, (int) camera.position.y, (int) camera.position.z, renderBatch, environment);
        renderBatch.end();

        PlayerInteractionRayCast.BlockRayCastResult rayCastResult = playerController.getBlockInteractionResult();
        if (rayCastResult != null) {
            rayCastResult.render(camera, blockOutlineRenderer);
        }

        debugScreen.render();

/*        chunkRegionOutlineRenderer.begin(ShapeRenderer.ShapeType.Line);
        if(VoxelClient.getInstance().getCurrentWorld() != null) {
            for (ChunkRenderRegion value : VoxelClient.getInstance().getCurrentWorld().getChunkVisibilityGraph().getChunkRenderRegionManager().getRegions().values()) {
                value.renderBoundariesForDebug(VoxelClient.getInstance().getCurrentWorld(), camera, chunkRegionOutlineRenderer);
            }
        }
        chunkRegionOutlineRenderer.end();*/
    }
}
