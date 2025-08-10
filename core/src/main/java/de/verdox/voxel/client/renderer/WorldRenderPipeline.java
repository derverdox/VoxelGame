package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.*;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.mesh.TerrainManager;
import de.verdox.voxel.client.renderer.shader.Shaders;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Benchmark;
import de.verdox.voxel.shared.util.TerrainRenderStats;
import lombok.Getter;

public class WorldRenderPipeline implements DebuggableOnScreen {
    @Getter
    private int centerChunkX, centerChunkY, centerChunkZ;
    private int amountFacesDrawn;
    private TerrainRenderStats terrainRenderStats = new TerrainRenderStats();


    public WorldRenderPipeline() {
        for (Texture texture : TextureAtlasManager.getInstance().getBlockTextureAtlas().getTextures()) {
            texture.bind();
            Gdx.gl.glTexParameteri(texture.glTarget, GL30.GL_TEXTURE_MAX_LEVEL, 4);
            Gdx.gl.glGenerateMipmap(texture.glTarget);
            texture.setFilter(Texture.TextureFilter.MipMapNearestNearest, Texture.TextureFilter.Nearest);
        }
    }

    public final void renderWorld(Camera camera, TerrainManager terrainManager, Benchmark renderBenchmark) {
        terrainRenderStats.reset();
        terrainRenderStats.maxDrawableChunks = ClientBase.clientSettings.horizontalViewDistance * ClientBase.clientSettings.verticalViewDistance * ClientBase.clientSettings.horizontalViewDistance;
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClearDepthf(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        int counter = 0;
        for (Texture texture : TextureAtlasManager.getInstance().getBlockTextureAtlas().getTextures()) {
            texture.bind(counter++);
        }

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
        amountFacesDrawn = terrainManager.getTerrainRenderGraph().renderTerrain(camera, terrainManager.getWorld(), ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance, terrainRenderStats);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        renderBenchmark.endSection();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Shaders.resetCurrentShader();
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        for (String printToLine : terrainRenderStats.printToLines()) {
            debugScreen.addDebugTextLine(printToLine);
        }
    }
}
