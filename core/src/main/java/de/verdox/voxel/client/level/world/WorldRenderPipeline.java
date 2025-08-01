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
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.terrain.TerrainMesh;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Benchmark;
import lombok.Getter;

public abstract class WorldRenderPipeline implements DebuggableOnScreen {
    @Getter
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
        world.getTerrainManager().getOctreeTerrainGraph().queryVisibleRegions(camera, terrainRegion -> {
            TerrainMesh terrainMesh = terrainRegion.getTerrainMesh();

            if (terrainMesh == null || terrainMesh.getAmountOfBlockFaces() == 0) {
                return;
            }

            int lodLevel = world.computeLodLevel(world.getTerrainManager().getCenterRegionX(), world.getTerrainManager()
                                                                                                    .getCenterRegionY(), world
                    .getTerrainManager()
                    .getCenterRegionZ(), terrainRegion.getRegionX(), terrainRegion.getRegionY(), terrainRegion.getRegionZ());

            if (terrainMesh.getLodLevel() != lodLevel) {
                //TODO: Recompute
                return;
            }
            MeshWithBounds mesh = terrainMesh.getOrGenerateMeshFromFaces(world, terrainRegion);
            if(mesh == null) {
                return;
            }

            mesh.render(camera);
        });
        //world.getTerrainManager().getTerrainGraph().renderTerrain(camera, world, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance);
        renderBenchmark.endSection();

        renderBenchmark.startSection("Profiler Reset");
        renderBenchmark.endSection();

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glEnable(GL20.GL_BLEND);

    }

    public abstract void onChangeChunk(ClientWorld world, int newChunkX, int newChunkY, int newChunkZ);

    @Override
    public void debugText(DebugScreen debugScreen) {

    }
}
