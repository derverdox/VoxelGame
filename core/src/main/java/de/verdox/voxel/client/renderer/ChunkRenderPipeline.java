package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ChunkVisibilityGraph;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.region.ChunkRenderRegion;
import de.verdox.voxel.client.level.mesh.region.ChunkRenderRegionManager;

import java.util.List;

@Deprecated
public interface ChunkRenderPipeline {

    /**
     * Executed when the player camera chunk changes
     */
    void onChunkCenterChange(ChunkRenderRegionManager renderRegionManager, ChunkVisibilityGraph visibilityGraph, int newCenterChunkX, int newCenterChunkY, int newCenterChunkZ);

    /**
     * Step 1
     */
    void computeVisibleRegions(ClientWorld world, Camera camera, ChunkRenderRegionManager renderRegionManager, List<ChunkRenderRegion> result);

    /**
     * Step 2
     */
    void computeVisibleChunksInRegion(ClientWorld world, Camera camera, ChunkRenderRegion chunkRenderRegion, ChunkVisibilityGraph visibilityGraph, List<ClientChunk> result);
}
