package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ChunkVisibilityGraph;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.region.ChunkRenderRegion;
import de.verdox.voxel.client.level.mesh.region.ChunkRenderRegionManager;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Direction;

import java.util.*;

@Deprecated
public class ChunkRenderPipelineImpl implements ChunkRenderPipeline {
    @Override
    public void onChunkCenterChange(ChunkRenderRegionManager renderRegionManager, ChunkVisibilityGraph visibilityGraph, int newCenterChunkX, int newCenterChunkY, int newCenterChunkZ) {

    }

    @Override
    public void computeVisibleRegions(ClientWorld world, Camera camera, ChunkRenderRegionManager renderRegionManager, List<ChunkRenderRegion> visibleRegions) {
        for (ChunkRenderRegion value : renderRegionManager.getRegions().values()) {
            if (camera.frustum.boundsInFrustum(value.getBounds())) {
                visibleRegions.add(value);
            }
        }
    }

    @Override
    public void computeVisibleChunksInRegion(ClientWorld world, Camera camera, ChunkRenderRegion chunkRenderRegion, ChunkVisibilityGraph visibilityGraph, List<ClientChunk> result) {
        Set<ChunkVisibilityGraph.ChunkNode> visited = new HashSet<>();
        Deque<ChunkVisibilityGraph.ChunkNode> queue = new ArrayDeque<>();

        int horizontalRadius = ClientBase.clientSettings.horizontalViewDistance / 2;
        int verticalRadius = ClientBase.clientSettings.verticalViewDistance / 2;

        int chunkX = ChunkBase.chunkX(world, (int) camera.position.x);
        int chunkY = ChunkBase.chunkY(world, (int) camera.position.y);
        int chunkZ = ChunkBase.chunkZ(world, (int) camera.position.z);

        ClientChunk camChunk = world.getChunkForPosition(camera.position);
        if (camChunk == null) return;

        ChunkVisibilityGraph.ChunkNode start = visibilityGraph.getNode(camChunk);
        if (start == null) return;

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            ChunkVisibilityGraph.ChunkNode cur = queue.poll();
            boolean isStart = (cur == start);
            // frustum culling
            if (!isStart && (chunkRenderRegion.contains(cur.getChunk().getChunkX(), cur.getChunk().getChunkY(), cur.getChunk().getChunkZ()) || !camera.frustum.boundsInFrustum(cur.getChunk().getBoundingBox()))) {
                continue;
            }


            for (int i = 0; i < Direction.values().length; i++) {
                ChunkVisibilityGraph.ChunkNode nei = cur.getNeighbors()[i];
                if (nei == null || visited.contains(nei)) continue;
                // side occlusion test
                //if ((cur.sideMask & (1 << dir)) != 0) continue;
                visited.add(nei);
                queue.add(nei);

                ClientChunk clientChunk = nei.getChunk();
                int dx = clientChunk.getChunkX() - chunkX;
                int dy = clientChunk.getChunkY() - chunkY;
                int dz = clientChunk.getChunkZ() - chunkZ;
                if (Math.abs(dx) > horizontalRadius
                    || Math.abs(dy) > verticalRadius
                    || Math.abs(dz) > horizontalRadius) {
                    world.removeChunk(clientChunk);
                    continue;
                }
                result.add(clientChunk);
            }
        }
    }


    private void iterateThroughChunkRegions(ChunkRenderRegion renderRegion, int centerChunkX, int centerChunkY, int centerChunkZ) {
        int regionRadiusX = (ClientBase.clientSettings.horizontalViewDistance + renderRegion.getSizeX() - 1) / renderRegion.getSizeX();
        int regionRadiusY = (ClientBase.clientSettings.horizontalViewDistance + renderRegion.getSizeY() - 1) / renderRegion.getSizeY();
        int regionRadiusZ = (ClientBase.clientSettings.horizontalViewDistance + renderRegion.getSizeZ() - 1) / renderRegion.getSizeZ();

        int playerRadiusX = centerChunkX + (ClientBase.clientSettings.horizontalViewDistance / 2);
        int playerRadiusY = centerChunkY + (ClientBase.clientSettings.verticalViewDistance / 2);
        int playerRadiusZ = centerChunkZ + (ClientBase.clientSettings.horizontalViewDistance / 2);
    }
}
