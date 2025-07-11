package de.verdox.voxel.client.level.mesh.region;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.math.Frustum;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ChunkVisibilityGraph;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.chunk.ChunkMesh;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Deprecated
public class ChunkRenderRegionManager {

    private final List<RegionDefinition> regionDefs = new ArrayList<>();
    @Getter
    private final Map<RegionKey, ChunkRenderRegion> regions = new HashMap<>();
    private final Map<RegionKey, ChunkRenderRegion> dirtyRegions = new ConcurrentHashMap<>();
    private final ChunkVisibilityGraph chunkVisibilityGraph;
    private final RegionComputingQueue regionComputingQueue = new RegionComputingQueue();

    public ChunkRenderRegionManager(ChunkVisibilityGraph chunkVisibilityGraph) {
        this.chunkVisibilityGraph = chunkVisibilityGraph;
        regionDefs.add(new RegionDefinition(8, 8, 8));
        regionDefs.add(new RegionDefinition(16, 16, 16));
        regionDefs.add(new RegionDefinition(32, 32, 32));
        regionDefs.add(new RegionDefinition(64, 64, 64));
    }

    /**
     * Call when the player's chunk position changes to update active regions.
     */
    public void updateRegionsAround(ClientWorld clientWorld, int playerChunkX, int playerChunkY, int playerChunkZ) {
        Set<RegionKey> needed = new HashSet<>();
        for (RegionDefinition def : regionDefs) {

            int radiusX = (ClientBase.clientSettings.horizontalViewDistance + def.sizeX - 1) / def.sizeX;
            int radiusY = (ClientBase.clientSettings.horizontalViewDistance + def.sizeY - 1) / def.sizeY;
            int radiusZ = (ClientBase.clientSettings.horizontalViewDistance + def.sizeZ - 1) / def.sizeZ;

            for (int dx = -radiusX; dx <= radiusX; dx++) {
                for (int dy = -radiusY; dy <= radiusY; dy++) {
                    for (int dz = -radiusZ; dz <= radiusZ; dz++) {

                        int regionX = playerChunkX + dx * def.sizeX;
                        int regionY = playerChunkY + dy * def.sizeY;
                        int regionZ = playerChunkZ + dz * def.sizeZ;

                        RegionKey key = RegionKey.of(def, regionX, regionY, regionZ);
                        needed.add(key);
                        regions.computeIfAbsent(key, k -> new ChunkRenderRegion(clientWorld, def.sizeX, def.sizeY, def.sizeZ, k.minX, k.minY, k.minZ));
                        regions.get(key).setDirty(true);
                        dirtyRegions.put(key, regions.get(key));
                    }
                }
            }
        }
        regions.keySet().removeIf(key -> !needed.contains(key));
        dirtyRegions.keySet().removeIf(key -> !needed.contains(key));
    }

    private void rebuildDirtyRegion(ChunkRenderRegion region, Camera camera) {
        ClientWorld world = chunkVisibilityGraph.getWorld();
        if (!region.isDirty()) {
            return;
        }

        ModelCache cache = region.getCache();
        cache.begin(camera);

        TextureAtlas textureAtlas = TextureAtlasManager.getInstance().getBlockTextureAtlas();

        for (int rX = region.getMinChunkX(); rX < region.getMinChunkX() + region.getSizeX(); rX++) {
            for (int rY = region.getMinChunkY(); rY < region.getMinChunkY() + region.getSizeY(); rY++) {
                for (int rZ = region.getMinChunkZ(); rZ < region.getMinChunkZ() + region.getSizeZ(); rZ++) {
                    if (!chunkVisibilityGraph.isInsideViewDistance(rX, rY, rZ)) {
                        continue;
                    }
                    if (!region.contains(rX, rY, rZ)) {
                        continue;
                    }

                    ChunkVisibilityGraph.ChunkNode chunkNode = chunkVisibilityGraph.getNode(rX, rY, rZ);
                    if (chunkNode == null) {
                        continue;
                    }

                    ClientChunk visibleChunk = chunkNode.getChunk();

                    ChunkMesh chunkMesh = ClientBase.clientRenderer.getWorldRenderer().getMeshMaster()
                                                                   .getChunkMeshIfAvailable(visibleChunk.getChunkX(), visibleChunk.getChunkY(), visibleChunk.getChunkZ());
                    if (chunkMesh == null) {
                        continue;
                    }

                    MeshWithBounds meshWithBounds = chunkMesh.getOrGenerateMeshFromFaces(textureAtlas, visibleChunk.getWorld(), visibleChunk.getChunkX(), visibleChunk.getChunkY(), visibleChunk.getChunkZ());
                    if (meshWithBounds == null) {
                        continue;
                    }
                    var boundingBoxInWorld = meshWithBounds.bounds();
                    if (!ClientBase.clientSettings.useFrustumCulling || camera.frustum.boundsInFrustum(boundingBoxInWorld)) {
                        cache.add(meshWithBounds.instance());
                    }
                }
            }
        }
        cache.end();
        region.setDirty(false);
    }

    /**
     * Rebuild caches for dirty regions (should be called off the render-thread if possible).
     */
    public void rebuildDirtyCaches(Camera camera) {
        if (dirtyRegions.isEmpty()) {
            return;
        }
        ClientWorld world = chunkVisibilityGraph.getWorld();

        for (ChunkRenderRegion region : dirtyRegions.values()) {
            if (!region.isDirty()) {
                continue;
            }
            rebuildDirtyRegion(region, camera);
        }
        dirtyRegions.clear();
    }

    /**
     * Render visible regions (on render-thread).
     */
    public void renderVisibleRegions(Camera camera, ModelBatch modelBatch) {
        Frustum frustum = camera.frustum;
        for (ChunkRenderRegion region : regions.values()) {
            if (frustum.boundsInFrustum(region.getBounds())) {
                modelBatch.render(region.getCache());
            }
        }
    }

    /**
     * Mark regions containing this chunk dirty on load/unload/block-update.
     */
    public void markRegionsDirtyForChunk(int cx, int cy, int cz) {
        for (var entry : regions.entrySet()) {
            if (entry.getValue().contains(cx, cy, cz)) {
                entry.getValue().setDirty(true);
                dirtyRegions.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void chunkLoaded(ClientChunk chunk) {
        markRegionsDirtyForChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    public void chunkUnloaded(ClientChunk chunk) {
        markRegionsDirtyForChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    public void blockUpdateInChunk(ClientChunk chunk) {
        markRegionsDirtyForChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    public record RegionDefinition(int sizeX, int sizeY, int sizeZ) {
    }

    /**
     * Unique key for a region based on its origin and definition.
     */
    private record RegionKey(RegionDefinition def, int minX, int minY, int minZ) {

        static RegionKey of(RegionDefinition def, int cx, int cy, int cz) {
            int minX = (cx >= 0 ? (cx / def.sizeX) : ((cx - def.sizeX + 1) / def.sizeX)) * def.sizeX;
            int minY = (cy >= 0 ? (cy / def.sizeY) : ((cy - def.sizeY + 1) / def.sizeY)) * def.sizeY;
            int minZ = (cz >= 0 ? (cz / def.sizeZ) : ((cz - def.sizeZ + 1) / def.sizeZ)) * def.sizeZ;
            return new RegionKey(def, minX, minY, minZ);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegionKey)) return false;
            RegionKey k = (RegionKey) o;
            return def.equals(k.def) && minX == k.minX && minY == k.minY && minZ == k.minZ;
        }

    }

    private static class RegionComputingQueue {
        private final ExecutorService executor = Executors.newFixedThreadPool(4);
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();

        void enqueue(Runnable task) {
            pending.add(task);
            scheduleNext();
        }

        private void scheduleNext() {
            if (running.compareAndSet(false, true)) {
                executor.submit(this::runAll);
            }
        }

        private void runAll() {
            try {
                Runnable next;
                while ((next = pending.poll()) != null) {
                    next.run();
                }
            } finally {
                running.set(false);
                if (!pending.isEmpty()) {
                    scheduleNext();
                }
            }
        }

    }
}
