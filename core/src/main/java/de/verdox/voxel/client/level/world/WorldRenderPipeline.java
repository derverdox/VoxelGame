package de.verdox.voxel.client.level.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.region.strategy.CameraCenteredRegionStrategy;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Benchmark;

public abstract class WorldRenderPipeline implements DebuggableOnScreen {
    private int centerChunkX, centerChunkY, centerChunkZ;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private int visibleRegionsCount = 0;
    private int meshedVisibleRegions = 0;
    private int emptyRegions = 0;
    private int notCompletedRegions = 0;
    private int renderedBlockFaces = 0;
    private final long[] visibleRegions = new long[4096];


    public WorldRenderPipeline() {

    }

    public final void renderWorld(Camera camera, ClientWorld world, ModelBatch batch, Environment environment, Benchmark renderBenchmark) {
        // 2) Depth-Test & Writes (einmal in create() reicht, hier nur der Vollständigkeit halber)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClearDepthf(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Shaders.GREEDY_OPAQUE_BLOCK_SHADER.bind();
        Shaders.SINGLE_OPAQUE_BLOCK_SHADER.bind();
        Shaders.GREEDY_OPAQUE_BLOCK_SHADER.setUniformMatrix("u_projViewTrans", camera.combined);
        Shaders.SINGLE_OPAQUE_BLOCK_SHADER.setUniformMatrix("u_projViewTrans", camera.combined);

        Gdx.gl.glEnable(GL20.GL_CULL_FACE);


        renderBenchmark.startSection("Prepare World Render");
        visibleRegionsCount = 0;
        meshedVisibleRegions = 0;
        notCompletedRegions = 0;
        renderedBlockFaces = 0;
        emptyRegions = 0;
        int currentChunkX = ChunkBase.chunkX(world, (int) camera.position.x);
        int currentChunkY = ChunkBase.chunkY(world, (int) camera.position.y);
        int currentChunkZ = ChunkBase.chunkZ(world, (int) camera.position.z);
        renderBenchmark.endSection();

        renderBenchmark.startSection("Chunk center change");
        if (centerChunkX != currentChunkX || centerChunkY != currentChunkY || centerChunkZ != currentChunkZ) {
            centerChunkX = currentChunkX;
            centerChunkY = currentChunkY;
            centerChunkZ = currentChunkZ;
            world.onCenterChange(centerChunkX, centerChunkY, centerChunkZ);
        }
        renderBenchmark.endSection();

        renderBenchmark.startSection("Render Visible regions");
        world.getTerrainManager().getTerrainGraph().bsfRenderVisibleRegions(camera, world, batch, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance);
        renderBenchmark.endSection();

        renderBenchmark.startSection("Profiler Reset");
        renderBenchmark.endSection();

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_BLEND);

    }

    public abstract void onChangeChunk(ClientWorld world, int newChunkX, int newChunkY, int newChunkZ);

    public void renderRegionBoundsForDebugging(ClientWorld world, ShapeRenderer shapeRenderer, CameraCenteredRegionStrategy.RenderRegion renderRegion) {
        Gdx.graphics.getGL20().glLineWidth(1);
        shapeRenderer.setColor(Color.BLUE);
        var min = renderRegion.getBoundingBox().min;
        shapeRenderer.box(min.x, min.y, min.z + renderRegion.getBoundingBox().getDepth(), renderRegion.getBoundingBox()
            .getWidth(), renderRegion
            .getBoundingBox().getHeight(), renderRegion.getBoundingBox().getDepth());

    }

    @Override
    public void debugText(DebugScreen debugScreen) {

    }
}
