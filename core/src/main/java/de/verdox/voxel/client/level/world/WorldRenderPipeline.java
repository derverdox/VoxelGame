package de.verdox.voxel.client.level.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.region.strategy.CameraCenteredRegionStrategy;
import de.verdox.voxel.client.level.mesh.region.strategy.ChunkRenderRegionStrategy;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.level.chunk.ChunkBase;

import java.util.ArrayList;
import java.util.List;

public abstract class WorldRenderPipeline implements DebuggableOnScreen {
    private int centerChunkX, centerChunkY, centerChunkZ;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private int visibleRegionsCount = 0;
    private long endFilter;
    private long endRegionBuild;

    public final void renderWorld(Camera camera, ClientWorld world, ModelBatch batch, Environment environment) {
        int currentChunkX = ChunkBase.chunkX(world, (int) camera.position.x);
        int currentChunkY = ChunkBase.chunkY(world, (int) camera.position.y);
        int currentChunkZ = ChunkBase.chunkZ(world, (int) camera.position.z);

        if (centerChunkX != currentChunkX || centerChunkY != currentChunkY || centerChunkZ != currentChunkZ) {
            long start = System.currentTimeMillis();
            onChangeChunk(world, currentChunkX, currentChunkY, currentChunkZ);
            System.out.println("Chunk center change took: " + (System.currentTimeMillis() - start) + "ms");
            centerChunkX = currentChunkX;
            centerChunkY = currentChunkY;
            centerChunkZ = currentChunkZ;
        }

        long startFilter = System.currentTimeMillis();
        List<CameraCenteredRegionStrategy.RenderRegion> visibleRegions = new ArrayList<>(world.getRenderRegionStrategy().getAmountOfRegions());

        ChunkRenderRegionStrategy.RenderRegion centerRegion = world.getRenderRegionStrategy().getRegionOfChunk(currentChunkX, currentChunkY, currentChunkZ);
        if (centerRegion != null) {
            visibleRegions.add(centerRegion);
        }

        world.getRenderRegionStrategy().filterRegionsInFrustum(camera, world, visibleRegions);
        endFilter = System.currentTimeMillis() - startFilter;


        visibleRegionsCount = visibleRegions.size();

/*        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setProjectionMatrix(camera.combined);*/

        long startRegionBuild = System.currentTimeMillis();
        int REBUILD_PER_FRAME = 150;

        for (int i = 0; i < visibleRegions.size(); i++) {
            CameraCenteredRegionStrategy.RenderRegion region = visibleRegions.get(i);
            //renderRegionBoundsForDebugging(world, shapeRenderer, region);

            region.render(camera, batch, REBUILD_PER_FRAME > 0);

            if (region.isDirty()) {
                REBUILD_PER_FRAME--;
            }
        }
/*        shapeRenderer.end();*/
        endRegionBuild = System.currentTimeMillis() - startRegionBuild;
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
        debugScreen.addDebugTextLine("Region rebuild avg: " + (CameraCenteredRegionStrategy.sumTime / CameraCenteredRegionStrategy.samples) + "ms");
        debugScreen.addDebugTextLine("Visible Regions: " + visibleRegionsCount);
        debugScreen.addDebugTextLine("Filter time: " + endFilter + "ms ");
        debugScreen.addDebugTextLine("Region batch time: " + endRegionBuild + "ms ");
    }
}
