package de.verdox.voxel.client.renderer.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.chunk.calculation.BitOcclusionBasedChunkMeshCalculator;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import lombok.Getter;

@Getter
@Deprecated
public class WorldRenderer implements DebuggableOnScreen {
    private int amountFaces;
    private int chunksInFrustum;
    private int chunkIterationsX;
    private int chunkIterationsY;
    private int chunkIterationsZ;

    private long renderTime;
    private World lastRenderedWorld;
    private int currentChunkX;
    private int currentChunkY;
    private int currentChunkZ;

    private int startX = 0;
    private int startY = 0;
    private int startZ = 0;
    private int endX = 0;
    private int endY = 0;
    private int endZ = 0;

    private final GLProfiler worldRendererProfiler;

    public WorldRenderer() {
        worldRendererProfiler = new GLProfiler(Gdx.graphics);
        worldRendererProfiler.enable();
    }

    public void renderWorld(Camera camera, ClientWorld world, int centerX, int centerY, int centerZ, ModelBatch batch, Environment environment) {
        long start = System.currentTimeMillis();
        try {

            amountFaces = 0;
            chunksInFrustum = 0;

            chunkIterationsX = 0;
            chunkIterationsY = 0;
            chunkIterationsZ = 0;

            lastRenderedWorld = null;

            currentChunkX = ChunkBase.chunkX(world, centerX);
            currentChunkY = ChunkBase.chunkY(world, centerY);
            currentChunkZ = ChunkBase.chunkZ(world, centerZ);


            //naiveChunkRendering(camera, world, batch, environment);
            //graphRendering(camera, world, batch, environment);
            //regionBasedRendering(camera, world, batch, environment);
            //greedyMeshRendering(camera, world, batch, environment);
            //chunkGraphRendering(camera, world, chunkMeshGraph, batch, environment);

            lastRenderedWorld = world;

        } finally {
            renderTime = System.currentTimeMillis() - start;
            worldRendererProfiler.reset();
        }
    }

    /*private void regionBasedRendering(Camera camera, ClientWorld world, ModelBatch batch, Environment environment) {
        world.getChunkVisibilityGraph().getChunkRenderRegionManager().rebuildDirtyCaches(camera);
        world.getChunkVisibilityGraph().getChunkRenderRegionManager().renderVisibleRegions(camera, batch);
    }

    private void graphRendering(Camera camera, ClientWorld world, ModelBatch batch, Environment environment) {
        //List<ClientChunk> chunks = world.getChunkVisibilityGraph().computeVisibleChunks(camera.frustum, camera.position);
        List<ClientChunk> chunks = world.getChunkVisibilityGraph().getVisibleRef().get();

        for (int i = 0; i < chunks.size(); i++) {
            ClientChunk visibleChunk = chunks.get(i);
            ChunkMesh chunkMesh = meshMaster.getChunkMeshIfAvailable(visibleChunk.getChunkX(), visibleChunk.getChunkY(), visibleChunk.getChunkZ());
            if (chunkMesh == null) {
                continue;
            }

            var builtMesh = chunkMesh.getOrGenerateMeshFromFaces(TextureAtlasManager.getInstance().getBlockTextureAtlas(), world, visibleChunk.getChunkX(), visibleChunk.getChunkY(), visibleChunk.getChunkZ());
            if (builtMesh != null) {
                if (chunkMesh.getAmountOfBlockFaces() == 0) {
                    continue;
                }
                var boundingBoxInWorld = builtMesh.bounds();
                if (!ClientBase.clientSettings.useFrustumCulling || camera.frustum.boundsInFrustum(boundingBoxInWorld)) {
                    batch.render(builtMesh.instance(), environment);
                    amountFaces += chunkMesh.getAmountOfBlockFaces();
                    chunksInFrustum++;
                }
            }
        }
    }*/

/*    private void naiveChunkRendering(Camera camera, World world, ModelBatch batch, Environment environment) {
        ChunkRenderingBounds renderBounds = ChunkRenderingBounds.computeChunkLoopBounds(camera, world);

        startX = renderBounds.startX;
        startY = renderBounds.startY;
        startZ = renderBounds.startZ;

        endX = renderBounds.endX;
        endY = renderBounds.endY;
        endZ = renderBounds.endZ;

        chunkIterationsX = endX - startX;
        chunkIterationsY = endY - startY;
        chunkIterationsZ = endZ - startZ;


        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {

                int sy = startY;
                int ey = endY;

                for (int y = sy; y <= ey; y++) {
                    ChunkMesh chunkMesh = meshMaster.getChunkMeshIfAvailable(x, y, z);
                    if (chunkMesh == null) {
                        continue;
                    }

                    var builtMesh = chunkMesh.getOrGenerateMeshFromFaces(TextureAtlasManager.getInstance()
                        .getBlockTextureAtlas());
                    if (builtMesh != null) {
                        var boundingBoxInWorld = builtMesh.setPos(world.getChunkSizeX() * x, world.getChunkSizeY() * y, world.getChunkSizeZ() * z);

                        if (!ClientBase.clientSettings.useFrustumCulling || camera.frustum.boundsInFrustum(boundingBoxInWorld)) {
                            batch.render(builtMesh.instance(), environment);
                            amountFaces += chunkMesh.getAmountOfBlockFaces();
                            renderedChunks++;
                            chunksInFrustum++;
                        }
                    }
                }
            }
        }
    }*/

    public static Environment createEnvironment(World world) {
        Environment environment = new Environment();
        environment.set(new ColorAttribute(
            ColorAttribute.AmbientLight,
            1f, 1f, 1f, 1f
        ));
        return environment;
    }

    @Override
    public void debugText(DebugScreen debugScreen) {

    }
}
