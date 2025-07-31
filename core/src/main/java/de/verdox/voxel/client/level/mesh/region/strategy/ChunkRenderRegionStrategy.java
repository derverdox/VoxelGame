package de.verdox.voxel.client.level.mesh.region.strategy;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public interface ChunkRenderRegionStrategy {
    void changeViewDistance(int viewDistanceX, int viewDistanceY, int viewDistanceZ);

    void chunkLoad(ClientChunk chunk);

    void chunkUnload(ClientChunk chunk);

    default void markDirty(ClientChunk chunk) {
        RenderRegion renderRegion = getRegionOfChunk(chunk);
        if (renderRegion == null) {
            return;
        }
        renderRegion.setDirty(true);

    }

    int getAmountOfRegions();

    default RenderRegion getRegionOfChunk(ClientChunk chunk) {
        return getRegionOfChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    RenderRegion getRegionOfChunk(int chunkX, int chunkY, int chunkZ);

    void onChangeCameraCenter(int chunkSizeX, int chunkSizeY, int chunkSizeZ, int centerChunkX, int centerChunkY, int centerChunkZ);

    int getCenterChunkX();

    int getCenterChunkY();

    int getCenterChunkZ();

    int getMinViewChunkX();

    int getMinViewChunkY();

    int getMinViewChunkZ();

    int getMaxViewChunkX();

    int getMaxViewChunkY();

    int getMaxViewChunkZ();

    ClientWorld getWorld();

    void filterRegionsInFrustum(Camera camera, ClientWorld world, List<CameraCenteredRegionStrategy.RenderRegion> result);


    class RenderRegion {
        private final ModelCache modelCache = new ModelCache();
        @Getter
        private final BoundingBox boundingBox = new BoundingBox();
        private final ChunkRenderRegionStrategy strategy;
        @Getter
        @Setter
        private boolean dirty;

        @Getter
        private int minChunkX;
        @Getter
        private int minChunkY;
        @Getter
        private int minChunkZ;
        @Getter
        private int maxChunkX;
        @Getter
        private int maxChunkY;
        @Getter
        private int maxChunkZ;
        @Getter
        private int level;

        public RenderRegion(ChunkRenderRegionStrategy chunkRenderRegionStrategy) {
            this.strategy = chunkRenderRegionStrategy;
            this.dirty = true;
        }

        public void rebuildRegionGeometry(int chunkSizeX, int chunkSizeY, int chunkSizeZ, int minChunkX, int minChunkY, int minChunkZ, int maxChunkX, int maxChunkY, int maxChunkZ, int level) {
            this.minChunkX = minChunkX;
            this.minChunkY = minChunkY;
            this.minChunkZ = minChunkZ;

            this.maxChunkX = maxChunkX;
            this.maxChunkY = maxChunkY;
            this.maxChunkZ = maxChunkZ;
            this.level = level;

            float minBlockX = minChunkX * (float) chunkSizeX;
            float minBlockY = minChunkY * (float) chunkSizeY;
            float minBlockZ = minChunkZ * (float) chunkSizeZ;

            float maxBlockX = (maxChunkX + 1) * (float) chunkSizeX;
            float maxBlockY = (maxChunkY + 1) * (float) chunkSizeY;
            float maxBlockZ = (maxChunkZ + 1) * (float) chunkSizeZ;

            boundingBox.min.set(minBlockX, minBlockY, minBlockZ);
            boundingBox.max.set(maxBlockX, maxBlockY, maxBlockZ);
            boundingBox.update();
            this.dirty = true;
        }

        // Called in render thread
        public void render(Camera camera, ModelBatch batch, boolean allowBuild) {
            if (dirty && allowBuild) {
                long start = System.currentTimeMillis();
                try {
                    if (level <= 1) {
                        MeshWithBounds meshWithBounds = getChunkMeshAt(minChunkX, minChunkY, minChunkZ);
                        if (meshWithBounds == null) return;
                        meshWithBounds.render(camera, batch);
                        //this.dirty = false;
                    } else {
                        naiveRegionBuilding(camera);
                    }
                } finally {
                    long end = System.currentTimeMillis() - start;
                    CameraCenteredRegionStrategy.sumTime += end;
                    CameraCenteredRegionStrategy.samples++;
                    this.dirty = false;
                }
            }
            if (level <= 1) {
                MeshWithBounds meshWithBounds = getChunkMeshAt(minChunkX, minChunkY, minChunkZ);
                if (meshWithBounds == null) return;
                meshWithBounds.render(camera, batch);
            } else {
                batch.render(modelCache);
            }
        }

        private boolean naiveRegionBuilding(Camera camera) {
            int counter = 0;

            modelCache.begin(camera);
            int xMinBound = Math.max(this.minChunkX, this.strategy.getMinViewChunkX());
            int yMinBound = Math.max(this.minChunkY, this.strategy.getMinViewChunkY());
            int zMinBound = Math.max(this.minChunkZ, this.strategy.getMinViewChunkZ());

            int xMaxBound = Math.min(this.maxChunkX, this.strategy.getMaxViewChunkX());
            int yMaxBound = Math.min(this.maxChunkY, this.strategy.getMaxViewChunkY());
            int zMaxBound = Math.min(this.maxChunkZ, this.strategy.getMaxViewChunkZ());

            //TODO: Also cap between the chunk that is the highest on world heightmap and world depth map.

            //System.out.println("[" + xMinBound + " - " + xMaxBound + "]" + "[" + yMinBound + " - " + yMaxBound + "]" + "[" + zMinBound + " - " + zMaxBound + "]");

            for (int chunkX = xMinBound; chunkX <= xMaxBound; chunkX++) {
                for (int chunkY = yMinBound; chunkY <= yMaxBound; chunkY++) {
                    for (int chunkZ = zMinBound; chunkZ <= zMaxBound; chunkZ++) {
                        MeshWithBounds meshWithBounds = getChunkMeshAt(chunkX, chunkY, chunkZ);
                        if (meshWithBounds == null) {
                            continue;
                        }
                        meshWithBounds.addToModelCache(modelCache);
                        counter++;
                    }
                }
            }

            modelCache.end();
            return true;
        }

        private final Set<ClientChunk> visited = new HashSet<>();
        private final Deque<ClientChunk> queue = new ArrayDeque<>();
        private final List<ClientChunk> visible = new ArrayList<>(512);

        private void bsfRegionBuilding(Camera camera) {
            int horizontalRadius = ClientBase.clientSettings.horizontalViewDistance;
            int verticalRadius = ClientBase.clientSettings.verticalViewDistance;

            int startChunkX = Math.min(Math.max(strategy.getCenterChunkX(), minChunkX), maxChunkX);
            int startChunkY = Math.min(Math.max(strategy.getCenterChunkY(), minChunkY), maxChunkY);
            int startChunkZ = Math.min(Math.max(strategy.getCenterChunkZ(), minChunkZ), maxChunkZ);
            ClientChunk start = strategy.getWorld().getChunk(startChunkX, startChunkY, startChunkZ);
            if (start == null) return;

            visited.add(start);
            queue.add(start);

            while (!queue.isEmpty()) {
                ClientChunk current = queue.poll();
                boolean isStart = (current.equals(start));

                // frustum culling
                //if (!isStart && !camera.frustum.boundsInFrustum(current.getBoundingBox())) continue;

                int dx = strategy.getCenterChunkX() - current.getChunkX();
                int dy = strategy.getCenterChunkY() - current.getChunkY();
                int dz = strategy.getCenterChunkZ() - current.getChunkZ();
                if (Math.abs(dx) > horizontalRadius
                    || Math.abs(dy) > verticalRadius
                    || Math.abs(dz) > horizontalRadius) {
                    continue;
                }

                MeshWithBounds meshWithBounds = getChunkMeshAt(current.getChunkX(), current.getChunkY(), current.getChunkZ());
                if (meshWithBounds != null) {
                    meshWithBounds.addToModelCache(modelCache);
                }

                // Is inside view distance?

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction direction = Direction.values()[i];
                    ClientChunk neighbor = strategy.getWorld().getChunk(current.getChunkX() + direction.getOffsetX(), current.getChunkY() + direction.getOffsetY(), current.getChunkZ() + direction.getOffsetZ());
                    if (neighbor == null || visited.contains(neighbor)) continue;

                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        private MeshWithBounds getChunkMeshAt(int chunkX, int chunkY, int chunkZ) {
/*            ChunkMesh chunkMesh = ClientBase.clientRenderer.getWorldRenderer().getMeshMaster().getChunkMeshIfAvailable(chunkX, chunkY, chunkZ);
            if (chunkMesh == null) {
                return null;
            }
            return chunkMesh.getOrGenerateMeshFromFaces(TextureAtlasManager.getInstance().getBlockTextureAtlas(), strategy.getWorld(), chunkX, chunkY, chunkZ);
        */
            return null;
        }
    }
}
