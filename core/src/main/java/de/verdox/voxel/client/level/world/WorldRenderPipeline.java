package de.verdox.voxel.client.level.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.terrain.TerrainManager;
import de.verdox.voxel.client.level.mesh.terrain.TerrainMesh;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Benchmark;
import lombok.Getter;

public class WorldRenderPipeline implements DebuggableOnScreen {
    @Getter
    private int centerChunkX, centerChunkY, centerChunkZ;


    public WorldRenderPipeline() {

    }

    public final void renderWorld(Camera camera, TerrainManager terrainManager, Benchmark renderBenchmark) {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClearDepthf(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Shaders.SINGLE_OPAQUE_BLOCK_SHADER.bind();

        int counter = 0;
        for (Texture texture : TextureAtlasManager.getInstance().getBlockTextureAtlas().getTextures()) {
            texture.bind(counter++);
        }
        Shaders.SINGLE_OPAQUE_BLOCK_SHADER.setUniformMatrix("u_projViewTrans", camera.combined);


        int currentChunkX = Chunk.chunkX(terrainManager.getWorld(), (int) camera.position.x);
        int currentChunkY = Chunk.chunkY(terrainManager.getWorld(), (int) camera.position.y);
        int currentChunkZ = Chunk.chunkZ(terrainManager.getWorld(), (int) camera.position.z);

        if (centerChunkX != currentChunkX || centerChunkY != currentChunkY || centerChunkZ != currentChunkZ) {
            centerChunkX = currentChunkX;
            centerChunkY = currentChunkY;
            centerChunkZ = currentChunkZ;
            terrainManager.getWorld().onCenterChange(centerChunkX, centerChunkY, centerChunkZ);
        }

        renderBenchmark.startSection("Render Visible regions");

        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        terrainManager.getOctreeTerrainGraph().queryVisibleRegions(camera, terrainRegion -> {
            TerrainMesh terrainMesh = terrainRegion.getTerrainMesh();

            if (terrainMesh == null || terrainMesh.getAmountOfBlockFaces() == 0) {
                return;
            }

            int lodLevel = terrainManager.computeLodLevel(terrainManager.getCenterRegionX(), terrainManager
                    .getCenterRegionY(), terrainManager
                    .getCenterRegionZ(), terrainRegion.getRegionX(), terrainRegion.getRegionY(), terrainRegion.getRegionZ());

            if (terrainMesh.getLodLevel() != lodLevel) {
                //TODO: Recompute
                return;
            }
            MeshWithBounds mesh = terrainMesh.getOrGenerateMeshFromFaces(terrainManager.getWorld(), terrainRegion);
            if (mesh == null) {
                return;
            }

            mesh.render(camera);
        });
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        //world.getTerrainManager().getTerrainGraph().renderTerrain(camera, world, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance);
        renderBenchmark.endSection();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
    }

    @Override
    public void debugText(DebugScreen debugScreen) {

    }
}
