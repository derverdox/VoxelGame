package de.verdox.voxel.client.level.mesh.terrain.graph;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.level.mesh.terrain.TerrainManager;
import de.verdox.voxel.client.level.mesh.terrain.TerrainRegion;
import de.verdox.voxel.shared.level.chunk.Chunk;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.List;

public class OctreeTerrainGraph {
    private final TerrainManager terrainManager;
    private final byte maxDepth;
    private final byte maxRegionsPerNode;
    private final OctreeNode root;

    public OctreeTerrainGraph(TerrainManager terrainManager, int maxDepth, int maxRegionsPerNode, int viewDistanceX, int viewDistanceY, int viewDistanceZ) {
        this.terrainManager = terrainManager;
        this.maxDepth = (byte) maxDepth;
        this.maxRegionsPerNode = (byte) maxRegionsPerNode;

        int spanX = terrainManager.getWorld().getChunkSizeX() * viewDistanceX;
        int spanY = terrainManager.getWorld().getChunkSizeY() * viewDistanceY;
        int spanZ = terrainManager.getWorld().getChunkSizeZ() * viewDistanceZ;

        int regionsInX = viewDistanceX / terrainManager.getBounds().regionSizeX();
        int regionsInY = viewDistanceY / terrainManager.getBounds().regionSizeY();
        int regionsInZ = viewDistanceZ / terrainManager.getBounds().regionSizeZ();

        this.root = new OctreeNode(-spanX, -spanY, -spanZ, spanX, spanY, spanZ, (byte) 0);

        for (int rX = -regionsInX; rX < regionsInX; rX++) {
            for (int rY = -regionsInY; rY < regionsInY; rY++) {
                for (int rZ = -regionsInZ; rZ < regionsInZ; rZ++) {
                    this.root.insert(rX, rY, rZ);
                }
            }
        }
    }

    private int chunkSizeX() {
        return terrainManager.getWorld().getChunkSizeX();
    }

    private int chunkSizeY() {
        return terrainManager.getWorld().getChunkSizeY();
    }

    private int chunkSizeZ() {
        return terrainManager.getWorld().getChunkSizeZ();
    }

    /**
     * Frustum-Query: Frustum ins lokalen Koordinaten testen und Callback mit Welt-Box
     */
    public void queryVisibleRegions(Camera camera, FrustumCallback frustumCallback) {
        int chunkX = Chunk.chunkX(terrainManager.getWorld(), (int) camera.position.x);
        int chunkY = Chunk.chunkY(terrainManager.getWorld(), (int) camera.position.y);
        int chunkZ = Chunk.chunkZ(terrainManager.getWorld(), (int) camera.position.z);

        int worldRegionX = terrainManager.getBounds().getRegionX(chunkX);
        int worldRegionY = terrainManager.getBounds().getRegionY(chunkY);
        int worldRegionZ = terrainManager.getBounds().getRegionZ(chunkZ);

        int minBlockX = terrainManager.getBounds().getMinBlockX(worldRegionX, chunkSizeX());
        int minBlockY = terrainManager.getBounds().getMinBlockY(worldRegionY, chunkSizeY());
        int minBlockZ = terrainManager.getBounds().getMinBlockZ(worldRegionZ, chunkSizeZ());
        int maxBlockX = terrainManager.getBounds().getMaxBlockX(worldRegionX, chunkSizeX());
        int maxBlockY = terrainManager.getBounds().getMaxBlockY(worldRegionY, chunkSizeY());
        int maxBlockZ = terrainManager.getBounds().getMaxBlockZ(worldRegionZ, chunkSizeZ());

        root.queryFrustum(camera, frustumCallback, worldRegionX, worldRegionY, worldRegionZ, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
    }

    private class OctreeNode {
        private int minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ;
        private final byte depth;
        private List<Long> regionsThisHeight;
        private OctreeNode[] children;

        public OctreeNode(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, byte depth) {
            this.minBlockX = minBlockX;
            this.minBlockY = minBlockY;
            this.minBlockZ = minBlockZ;
            this.maxBlockX = maxBlockX;
            this.maxBlockY = maxBlockY;
            this.maxBlockZ = maxBlockZ;
            this.depth = depth;
            this.regionsThisHeight = new LongArrayList();
        }

        void insert(int relativeRegionX, int relativeRegionY, int relativeRegionZ) {
            if (children != null) {
                int i = getChildIndexByBounds(relativeRegionX, relativeRegionY, relativeRegionZ);
                if (i >= 0) {
                    children[i].insert(relativeRegionX, relativeRegionY, relativeRegionZ);
                    return;
                }
            }

            this.regionsThisHeight.add(getRelativeKey(relativeRegionX, relativeRegionY, relativeRegionZ));
            if (regionsThisHeight.size() < maxRegionsPerNode || depth > maxDepth) {
                return;
            }

            subDivide();
            var copy = new LongArrayList(regionsThisHeight);
            regionsThisHeight.clear();
            for (int i = 0; i < copy.size(); i++) {
                long regionKey = copy.getLong(i);
                insert(extractRelativeRegionX(regionKey), extractRelativeRegionY(regionKey), extractRelativeRegionZ(regionKey));
            }
        }

        private void queryFrustum(
                Camera camera,
                FrustumCallback frustumCallback,
                int cameraRegionXWorld, int cameraRegionYWorld, int cameraRegionZWorld,
                int minBlockXCameraWorld, int minBlockYCameraWorld, int minBlockZCameraWorld,
                int maxBlockXCameraWorld, int maxBlockYCameraWorld, int maxBlockZCameraWorld
        ) {
            if (!isInFrustum(
                    camera,
                    minBlockXCameraWorld, minBlockYCameraWorld, minBlockZCameraWorld,
                    maxBlockXCameraWorld, maxBlockYCameraWorld, maxBlockZCameraWorld,
                    this.minBlockX, this.minBlockY, this.minBlockZ,
                    this.maxBlockX, this.maxBlockY, this.maxBlockZ)
            ) {
                return;
            }

            for (int i = 0; i < this.regionsThisHeight.size(); i++) {
                long relativeRegionKey = this.regionsThisHeight.get(i);
                int relativeRegionX = extractRelativeRegionX(relativeRegionKey);
                int relativeRegionY = extractRelativeRegionY(relativeRegionKey);
                int relativeRegionZ = extractRelativeRegionZ(relativeRegionKey);

                int minBlockX = terrainManager.getBounds().getMinBlockX(relativeRegionX, chunkSizeX());
                int minBlockY = terrainManager.getBounds().getMinBlockY(relativeRegionY, chunkSizeY());
                int minBlockZ = terrainManager.getBounds().getMinBlockZ(relativeRegionZ, chunkSizeZ());

                int maxBlockX = terrainManager.getBounds().getMaxBlockX(relativeRegionX, chunkSizeX());
                int maxBlockY = terrainManager.getBounds().getMaxBlockY(relativeRegionY, chunkSizeY());
                int maxBlockZ = terrainManager.getBounds().getMaxBlockZ(relativeRegionZ, chunkSizeZ());

                if (isInFrustum(
                        camera,
                        minBlockXCameraWorld, minBlockYCameraWorld, minBlockZCameraWorld,
                        maxBlockXCameraWorld, maxBlockYCameraWorld, maxBlockZCameraWorld,
                        minBlockX, minBlockY, minBlockZ,
                        maxBlockX, maxBlockY, maxBlockZ)
                ) {
                    TerrainRegion terrainRegion = terrainManager.getRegion(relativeRegionX + cameraRegionXWorld, relativeRegionY + cameraRegionYWorld, relativeRegionZ + cameraRegionZWorld);
                    if (terrainRegion != null) {
                        frustumCallback.onSeeRegion(terrainRegion);
                    }
                }
            }

            // 3. Traversiere Kinder-Knoten rekursiv
            if (children != null) {
                for (OctreeNode child : children) {
                    child.queryFrustum(camera, frustumCallback, cameraRegionXWorld, cameraRegionYWorld, cameraRegionZWorld, minBlockXCameraWorld, minBlockYCameraWorld, minBlockZCameraWorld, maxBlockXCameraWorld, maxBlockYCameraWorld, maxBlockZCameraWorld);
                }
            }
        }

        private boolean isInFrustum(
                Camera camera,
                int minBlockXCameraWorld, int minBlockYCameraWorld, int minBlockZCameraWorld,
                int maxBlockXCameraWorld, int maxBlockYCameraWorld, int maxBlockZCameraWorld,
                int minBlockX, int minBlockY, int minBlockZ,
                int maxBlockX, int maxBlockY, int maxBlockZ
        ) {
            int minBlockXWorld = minBlockXCameraWorld + minBlockX;
            int minBlockYWorld = minBlockYCameraWorld + minBlockY;
            int minBlockZWorld = minBlockZCameraWorld + minBlockZ;

            int maxBlockXWorld = maxBlockXCameraWorld + maxBlockX;
            int maxBlockYWorld = maxBlockYCameraWorld + maxBlockY;
            int maxBlockZWorld = maxBlockZCameraWorld + maxBlockZ;

            int centerBlockXWorld = minBlockXWorld + (maxBlockXWorld - minBlockXWorld) / 2;
            int centerBlockYWorld = minBlockYWorld + (maxBlockYWorld - minBlockYWorld) / 2;
            int centerBlockZWorld = minBlockZWorld + (maxBlockZWorld - minBlockZWorld) / 2;

            int widthHalf = (maxBlockXWorld - minBlockXWorld) / 2;
            int heightHalf = (maxBlockYWorld - minBlockYWorld) / 2;
            int depthHalf = (maxBlockZWorld - minBlockZWorld) / 2;

            return camera.frustum.boundsInFrustum(centerBlockXWorld, centerBlockYWorld, centerBlockZWorld, widthHalf, heightHalf, depthHalf);
        }

        private void subDivide() {
            children = new OctreeNode[8];

            int centerX = minBlockX + (maxBlockX - minBlockX) / 2;
            int centerY = minBlockY + (maxBlockY - minBlockY) / 2;
            int centerZ = minBlockZ + (maxBlockZ - minBlockZ) / 2;

            for (int i = 0; i < 8; i++) {

                int minX = (i & 1) == 0 ? this.minBlockX : centerX;
                int minY = (i & 2) == 0 ? this.minBlockY : centerY;
                int minZ = (i & 4) == 0 ? this.minBlockZ : centerZ;

                int maxX = (i & 1) == 0 ? centerX : this.maxBlockX;
                int maxY = (i & 2) == 0 ? centerY : this.maxBlockY;
                int maxZ = (i & 4) == 0 ? centerZ : this.maxBlockZ;

                children[i] = new OctreeNode(minX, minY, minZ, maxX, maxY, maxZ, (byte) (depth + 1));
            }
        }

        private int getChildIndexByBounds(int relativeRegionX, int relativeRegionY, int relativeRegionZ) {
            for (int i = 0; i < 8; i++) {
                OctreeNode node = children[i];
                if (node.contains(relativeRegionX, relativeRegionY, relativeRegionZ)) {
                    return i;
                }
            }
            return -1;
        }

        private boolean contains(int relativeRegionX, int relativeRegionY, int relativeRegionZ) {

            int minX = terrainManager.getBounds().getMinChunkX(relativeRegionX) * terrainManager.getWorld()
                                                                                                .getChunkSizeX();
            int minY = terrainManager.getBounds().getMinChunkY(relativeRegionY) * terrainManager.getWorld()
                                                                                                .getChunkSizeY();
            int minZ = terrainManager.getBounds().getMinChunkZ(relativeRegionZ) * terrainManager.getWorld()
                                                                                                .getChunkSizeZ();

            int maxX = terrainManager.getBounds().getMaxChunkX(relativeRegionX) * terrainManager.getWorld()
                                                                                                .getChunkSizeX();
            int maxY = terrainManager.getBounds().getMaxChunkY(relativeRegionY) * terrainManager.getWorld()
                                                                                                .getChunkSizeY();
            int maxZ = terrainManager.getBounds().getMaxChunkZ(relativeRegionZ) * terrainManager.getWorld()
                                                                                                .getChunkSizeZ();

            return this.minBlockX <= minX && this.maxBlockX >= maxX && this.minBlockY <= minY && this.maxBlockY >= maxY && this.minBlockZ <= minZ && this.maxBlockZ >= maxZ;
        }

        private long getRelativeKey(int relativeRegionX, int relativeRegionY, int relativeRegionZ) {
            int relativeToCenterX = relativeRegionX - terrainManager.getCenterRegionX();
            int relativeToCenterY = relativeRegionY - terrainManager.getCenterRegionY();
            int relativeToCenterZ = relativeRegionZ - terrainManager.getCenterRegionZ();

            return terrainManager.getBounds().getRegionKey(relativeToCenterX, relativeToCenterY, relativeToCenterZ);
        }

        private int extractRelativeRegionX(long relativeRegionKey) {
            return Chunk.unpackChunkX(relativeRegionKey);
        }

        private int extractRelativeRegionY(long relativeRegionKey) {
            return Chunk.unpackChunkY(relativeRegionKey);
        }

        private int extractRelativeRegionZ(long relativeRegionKey) {
            return Chunk.unpackChunkZ(relativeRegionKey);
        }
    }

    public interface FrustumCallback {
        void onSeeRegion(TerrainRegion terrainRegion);
    }
}
