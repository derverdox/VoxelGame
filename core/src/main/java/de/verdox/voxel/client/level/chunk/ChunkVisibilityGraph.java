package de.verdox.voxel.client.level.chunk;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.chunk.MeshMaster;
import de.verdox.voxel.client.level.mesh.region.ChunkRenderRegionManager;
import de.verdox.voxel.client.util.ChunkMeshUtil;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.BitPackingUtil;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Deprecated
public class ChunkVisibilityGraph {
    @Getter
    private final ClientWorld world;
    private final ChunkNode[] availableNodes = new ChunkNode[BitPackingUtil.calculateRadialBitPackingArraySize(ClientBase.clientSettings.horizontalViewDistance + 10, ClientBase.clientSettings.horizontalViewDistance + 10, ClientBase.clientSettings.verticalViewDistance + 10)];
    private final Set<Long> emptyChunkKeys = new HashSet<>();
    private final Set<Long> nonEmptyChunkKeys = new HashSet<>();


    private final ScheduledExecutorService bg = Executors.newSingleThreadScheduledExecutor();
    @Getter
    private final AtomicReference<List<ClientChunk>> visibleRef = new AtomicReference<>(Collections.emptyList());
    private List<ClientChunk> backBuffer = new ArrayList<>();
    @Getter
    private final ChunkRenderRegionManager chunkRenderRegionManager = new ChunkRenderRegionManager(this);

    public ChunkVisibilityGraph(ClientWorld world) {
        this.world = world;
        world.getLoadedChunks().forEach(this::chunkLoaded);

        bg.scheduleWithFixedDelay(() -> {
            Camera camera = ClientBase.clientRenderer.getCamera();
            backBuffer = computeVisibleChunks(camera.frustum, new Vector3(camera.position));
            visibleRef.set(new ArrayList<>(backBuffer));
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private ChunkNode addNode(ClientChunk clientChunk) {
        ChunkNode node = new ChunkNode(clientChunk);
        availableNodes[idx(clientChunk.getChunkX(), clientChunk.getChunkY(), clientChunk.getChunkZ())] = node;
        return node;
    }

    private ChunkNode removeNode(int chunkX, int chunkY, int chunkZ) {
        int idx = idx(chunkX, chunkY, chunkZ);
        ChunkNode node = availableNodes[idx];
        availableNodes[idx] = null;
        return node;
    }

    public boolean isInsideViewDistance(int chunkX, int chunkY, int chunkZ) {
        return BitPackingUtil.isRadialValidIndex(chunkX, chunkY, chunkZ, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance);
    }

    public ChunkNode getNode(int chunkX, int chunkY, int chunkZ) {
        return availableNodes[idx(chunkX, chunkY, chunkZ)];
    }

    public ChunkNode getNode(ClientChunk clientChunk) {
        return getNode(clientChunk.getChunkX(), clientChunk.getChunkY(), clientChunk.getChunkZ());
    }

    private int idx(int chunkX, int chunkY, int chunkZ) {
        return BitPackingUtil.getRadialBitPackingArrayIndex(chunkX, chunkY, chunkZ, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance);
    }


    public void chunkLoaded(ClientChunk chunk) {
        ChunkNode node = addNode(chunk);
        linkNeighbors(node);
        rebuildAround(node);

        recalculateChunkMeshIfAllowed(chunk);

        for (int i = 0; i < node.neighbors.length; i++) {
            ChunkNode nei = node.neighbors[i];
            if (nei != null) {
                linkNeighbors(nei);
                rebuildAround(nei);
            }
        }
        chunkRenderRegionManager.chunkLoaded(chunk);
    }

    private void recalculateChunkMeshIfAllowed(ClientChunk chunk) {
        if (!ChunkMeshUtil.isAllowedToGenerateMesh(
            chunk,
            world.getRenderRegionStrategy().getCenterChunkX(), world.getRenderRegionStrategy().getCenterChunkY(), world.getRenderRegionStrategy().getCenterChunkZ(),
            ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance)
        ) {
            return;
        }
        MeshMaster meshMaster = ClientBase.clientRenderer
            .getWorldRenderer()
            .getMeshMaster();
        meshMaster.recalculateMesh(chunk);
    }


    public void chunkUnloaded(ClientChunk chunk) {
        long key = ChunkBase.computeChunkKey(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
        // remove from all maps/sets
        ChunkNode node = removeNode(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
        emptyChunkKeys.remove(key);
        nonEmptyChunkKeys.remove(key);
        if (node == null) return;

        // unlink from neighbors

        for (int i = 0; i < Direction.values().length; i++) {
            ChunkNode nei = node.neighbors[i];
            if (nei != null) {
                nei.neighbors[i ^ 1] = null;
            }
        }
        chunkRenderRegionManager.chunkUnloaded(chunk);
    }

    public void blockUpdateInChunk(ClientChunk chunk) {
        long key = ChunkBase.computeChunkKey(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
        ChunkNode node = getNode(chunk);
        if (node == null) return;

        // update side‐occlusion mask
        int newSideMask = chunk.getOccupancyMask().getSideOcclusionMask();
        node.sideMask = newSideMask;

        // update empty/nonEmpty sets
        boolean nowEmpty = chunk.getOccupancyMask().isChunkEmpty();
        if (nowEmpty) {
            nonEmptyChunkKeys.remove(key);
            emptyChunkKeys.add(key);
        } else {
            emptyChunkKeys.remove(key);
            nonEmptyChunkKeys.add(key);
        }
        chunkRenderRegionManager.blockUpdateInChunk(chunk);
    }

    /**
     * Returns all chunks visible from the camera.
     */
    private final Set<ChunkNode> visited = new HashSet<>();
    private final Deque<ChunkNode> queue = new ArrayDeque<>();
    private final List<ClientChunk> visible = new ArrayList<>(2048);

    public List<ClientChunk> computeVisibleChunks(Frustum frustum, Vector3 position) {
        visited.clear();
        queue.clear();
        visible.clear();

        int horizontalRadius = ClientBase.clientSettings.horizontalViewDistance / 2;
        int verticalRadius = ClientBase.clientSettings.verticalViewDistance / 2;

        int chunkX = ChunkBase.chunkX(world, (int) position.x);
        int chunkY = ChunkBase.chunkY(world, (int) position.y);
        int chunkZ = ChunkBase.chunkZ(world, (int) position.z);

        ClientChunk camChunk = world.getChunkForPosition(position);
        if (camChunk == null) return visible;

        ChunkNode start = getNode(camChunk);
        if (start == null) return visible;

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            ChunkNode cur = queue.poll();
            boolean isStart = (cur == start);
            // frustum culling
            if (!isStart && !frustum.boundsInFrustum(cur.chunk.getBoundingBox())) continue;


            for (int i = 0; i < Direction.values().length; i++) {
                ChunkNode nei = cur.neighbors[i];
                if (nei == null || visited.contains(nei)) continue;
                // side occlusion test
                //if ((cur.sideMask & (1 << dir)) != 0) continue;
                visited.add(nei);
                queue.add(nei);

                ClientChunk clientChunk = nei.chunk;
                int dx = clientChunk.getChunkX() - chunkX;
                int dy = clientChunk.getChunkY() - chunkY;
                int dz = clientChunk.getChunkZ() - chunkZ;
                if (Math.abs(dx) > horizontalRadius
                    || Math.abs(dy) > verticalRadius
                    || Math.abs(dz) > horizontalRadius) {
                    world.removeChunk(clientChunk);
                    continue;
                }
                visible.add(clientChunk);
            }
        }
        return visible;
    }

    private void linkNeighbors(ChunkNode node) {
        ClientChunk chunk = node.chunk;

        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];
            ClientChunk c = world.getChunk(
                chunk.getChunkX() + direction.getOffsetX(), chunk.getChunkY() + direction.getOffsetY(), chunk.getChunkZ() + direction.getOffsetZ()
            );
            if (c != null) {
                ChunkNode nei = getNode(c);
                node.neighbors[i] = nei;
                if (nei != null) {
                    nei.neighbors[i ^ 1] = node;
                    nei.sideMask = nei.chunk.getOccupancyMask().getSideOcclusionMask();
                }
            } else {
                node.neighbors[i] = null;
            }
        }
        node.sideMask = node.chunk.getOccupancyMask().getSideOcclusionMask();
    }

    private void rebuildAround(ChunkNode node) {
        Set<ClientChunk> toRebuild = new HashSet<>();
        toRebuild.add(node.chunk);
        for (ChunkNode nei : node.neighbors) {
            if (nei != null) {
                toRebuild.add(nei.chunk);
            }
        }
        for (ClientChunk c : toRebuild) {
            recalculateChunkMeshIfAllowed(c);
        }
    }


    @Getter
    public static class ChunkNode {
        final ClientChunk chunk;
        final ChunkNode[] neighbors = new ChunkNode[6];
        int sideMask;

        ChunkNode(ClientChunk chunk) {
            this.chunk = chunk;
            this.sideMask = chunk.getOccupancyMask().getSideOcclusionMask();
        }
    }
}
