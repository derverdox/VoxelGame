package de.verdox.voxel.client.renderer.graph;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.TerrainManager;
import de.verdox.voxel.client.renderer.classic.TerrainMesh;
import de.verdox.voxel.client.level.mesh.TerrainRegion;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.TerrainRenderStats;
import de.verdox.voxel.shared.util.ThreadUtil;
import de.verdox.voxel.shared.util.datastructure.LongQueue;
import it.unimi.dsi.fastutil.longs.*;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NaiveTerrainRenderGraph implements TerrainRenderGraph {
    private final Long2ObjectOpenHashMap<RegionNode> regions = new Long2ObjectOpenHashMap<>();

    private final LongSet visited = new LongOpenHashSet();
    private final LongQueue queue = new LongQueue();

    private final Map<PlaneKey, NavigableSet<Integer>> xyIndex = new HashMap<>();
    private final Map<PlaneKey, NavigableSet<Integer>> xzIndex = new HashMap<>();
    private final Map<PlaneKey, NavigableSet<Integer>> yzIndex = new HashMap<>();
    private final TerrainManager terrainManager;
    private final int chunkSizeX;
    private final int chunkSizeY;
    private final int chunkSizeZ;
    private final Executor service = Executors.newSingleThreadExecutor(ThreadUtil.createFactoryForName("Terrain Graph Calculation Thread", true));

    public NaiveTerrainRenderGraph(TerrainManager terrainManager, int chunkSizeX, int chunkSizeY, int chunkSizeZ) {
        this.terrainManager = terrainManager;
        this.chunkSizeX = chunkSizeX;
        this.chunkSizeY = chunkSizeY;
        this.chunkSizeZ = chunkSizeZ;
    }

    @Override
    public void addRegion(int x, int y, int z) {
        addRegionInternal(x, y, z);
    }

    @Override
    public void removeRegion(int x, int y, int z) {
        long pos = Chunk.computeChunkKey(x, y, z);
        RegionNode old = regions.remove(pos);
        if (old == null) return;

        // Unlink along Z
        NavigableSet<Integer> zSet = xyIndex.get(new PlaneKey(x, y));
        zSet.remove(z);
        if (old.nextZPos != null) old.nextZPos.nextZNeg = old.nextZNeg;
        if (old.nextZNeg != null) old.nextZNeg.nextZPos = old.nextZPos;
        if (zSet.isEmpty()) xyIndex.remove(new PlaneKey(x, y));

        // Unlink along Y
        NavigableSet<Integer> ySet = xzIndex.get(new PlaneKey(x, z));
        ySet.remove(y);
        if (old.nextYPos != null) old.nextYPos.nextYNeg = old.nextYNeg;
        if (old.nextYNeg != null) old.nextYNeg.nextYPos = old.nextYPos;
        if (ySet.isEmpty()) xzIndex.remove(new PlaneKey(x, z));

        // Unlink along X
        NavigableSet<Integer> xSet = yzIndex.get(new PlaneKey(y, z));
        xSet.remove(x);
        if (old.nextXPos != null) old.nextXPos.nextXNeg = old.nextXNeg;
        if (old.nextXNeg != null) old.nextXNeg.nextXPos = old.nextXPos;
        if (xSet.isEmpty()) yzIndex.remove(new PlaneKey(y, z));
    }

    @Override
    public synchronized int renderTerrain(Camera camera, ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ, TerrainRenderStats renderStats) {
        int amountFacesRendered = 0;
        visited.clear();
        queue.clear();

        int startChunkX = Chunk.chunkX(chunkSizeX, (int) camera.position.x);
        int startChunkY = Chunk.chunkY(chunkSizeY, (int) camera.position.y);
        int startChunkZ = Chunk.chunkZ(chunkSizeZ, (int) camera.position.z);

        int regionX = terrainManager.getBounds().getRegionX(startChunkX);
        int regionY = terrainManager.getBounds().getRegionY(startChunkY);
        int regionZ = terrainManager.getBounds().getRegionZ(startChunkZ);
        long regionKey = Chunk.computeChunkKey(regionX, regionY, regionZ);

        RegionNode startNode = regions.get(regionKey);
        boolean removeCameraNodeAfter = false;
        if (startNode == null) {
            removeCameraNodeAfter = true;
            startNode = addRegionInternal(regionX, regionY, regionZ);
            regionKey = startNode.pos;
        }

        visited.add(regionKey);
        queue.enqueue(regionKey);

        while (!queue.isEmpty()) {
            long key = queue.dequeue();
            RegionNode node = regions.get(key);
            if (node == null) {
                continue;
            }

            int nx = Chunk.unpackChunkX(key);
            int ny = Chunk.unpackChunkY(key);
            int nz = Chunk.unpackChunkZ(key);

            TerrainRegion terrainRegion = terrainManager.getRegion(nx, ny, nz);

            if (terrainRegion == null) {
                continue;
            }

            if (!startNode.equals(node)) {
                if (Math.abs(startChunkX - nx) > (viewDistanceX) ||
                        Math.abs(startChunkY - ny) > (viewDistanceY) ||
                        Math.abs(startChunkZ - nz) > (viewDistanceZ)) {
                    continue;
                }
            }
            if (!camera.frustum.boundsInFrustum(node.boundingBox)) {
                continue;
            }

            TerrainMesh terrainMesh = terrainRegion.getTerrainMesh();

            if (terrainMesh != null && terrainMesh.getAmountOfBlockFaces() != 0 /*&& terrainMesh.isComplete()*/) {

                int lodLevel = terrainManager.computeLodLevel(regionX, regionY, regionZ, nx, ny, nz);
                if (terrainMesh.getLodLevel() != lodLevel) {
                    terrainManager.updateMesh(terrainRegion, false, lodLevel);
                    continue;
                }

                var mesh = terrainMesh.getOrGenerateMeshFromFaces(world, terrainRegion);
                if (mesh == null) {
                    continue;
                }
                amountFacesRendered += terrainRegion.getRenderedFaces();
                mesh.render(camera);
                terrainMesh.count(renderStats);
                renderStats.drawnChunks += terrainRegion.getRenderedChunks();
            }


            if (!node.hasLinkedNeighbors()) {
                continue;
            }

            if (node.nextXPos != null && visited.add(node.nextXPos.pos)) {
                if (!ClientBase.clientSettings.useOcclusionCulling || (terrainRegion.getSideOcclusionMask() & (1 << Direction.EAST.getId())) == 0) {
                    queue.enqueue(node.nextXPos.pos);
                }
            }

            if (node.nextXNeg != null && visited.add(node.nextXNeg.pos)) {
                if (!ClientBase.clientSettings.useOcclusionCulling || (terrainRegion.getSideOcclusionMask() & (1 << Direction.WEST.getId())) == 0) {
                    queue.enqueue(node.nextXNeg.pos);
                }
            }

            if (node.nextYPos != null && visited.add(node.nextYPos.pos)) {
                if (!ClientBase.clientSettings.useOcclusionCulling || (terrainRegion.getSideOcclusionMask() & (1 << Direction.UP.getId())) == 0) {
                    queue.enqueue(node.nextYPos.pos);
                }
            }

            if (node.nextYNeg != null && visited.add(node.nextYNeg.pos)) {
                if (!ClientBase.clientSettings.useOcclusionCulling || (terrainRegion.getSideOcclusionMask() & (1 << Direction.DOWN.getId())) == 0) {
                    queue.enqueue(node.nextYNeg.pos);
                }
            }

            if (node.nextZPos != null && visited.add(node.nextZPos.pos)) {
                if (!ClientBase.clientSettings.useOcclusionCulling || (terrainRegion.getSideOcclusionMask() & (1 << Direction.SOUTH.getId())) == 0) {
                    queue.enqueue(node.nextZPos.pos);
                }
            }


            if (node.nextZNeg != null && visited.add(node.nextZNeg.pos)) {
                if (!ClientBase.clientSettings.useOcclusionCulling || (terrainRegion.getSideOcclusionMask() & (1 << Direction.NORTH.getId())) == 0) {
                    queue.enqueue(node.nextZNeg.pos);
                }
            }
        }

        if (removeCameraNodeAfter) {
            removeRegion(regionX, regionY, regionZ);
        }
        return amountFacesRendered;
    }

    private synchronized RegionNode addRegionInternal(int x, int y, int z) {
        long pos = Chunk.computeChunkKey(x, y, z);
        if (regions.containsKey(pos)) return regions.get(pos);
        RegionNode newRegionNode = new RegionNode(x, y, z);
        // XY-plane index for Z
        NavigableSet<Integer> zSet = xyIndex.computeIfAbsent(new PlaneKey(x, y), k -> new TreeSet<>());
        Integer zAbove = zSet.higher(z);
        Integer zBelow = zSet.lower(z);

        // link along Z
        if (zAbove != null) {
            RegionNode above = regions.get(Chunk.computeChunkKey(x, y, zAbove));
            newRegionNode.nextZPos = above;
            above.nextZNeg = newRegionNode;
        }
        if (zBelow != null) {
            RegionNode below = regions.get(Chunk.computeChunkKey(x, y, zBelow));
            newRegionNode.nextZNeg = below;
            below.nextZPos = newRegionNode;
        }
        zSet.add(z);

        // XZ-plane index for Y
        NavigableSet<Integer> ySet = xzIndex.computeIfAbsent(new PlaneKey(x, z), k -> new TreeSet<>());
        Integer yAbove = ySet.higher(y);
        Integer yBelow = ySet.lower(y);
        if (yAbove != null) {
            RegionNode nYPos = regions.get(Chunk.computeChunkKey(x, yAbove, z));
            newRegionNode.nextYPos = nYPos;
            nYPos.nextYNeg = newRegionNode;
        }
        if (yBelow != null) {
            RegionNode nYNeg = regions.get(Chunk.computeChunkKey(x, yBelow, z));
            newRegionNode.nextYNeg = nYNeg;
            nYNeg.nextYPos = newRegionNode;
        }
        ySet.add(y);

        // YZ-plane index for X
        NavigableSet<Integer> xSet = yzIndex.computeIfAbsent(new PlaneKey(y, z), k -> new TreeSet<>());
        Integer xAbove = xSet.higher(x);
        Integer xBelow = xSet.lower(x);
        if (xAbove != null) {
            RegionNode nXPos = regions.get(Chunk.computeChunkKey(xAbove, y, z));
            newRegionNode.nextXPos = nXPos;
            nXPos.nextXNeg = newRegionNode;
        }
        if (xBelow != null) {
            RegionNode nXNeg = regions.get(Chunk.computeChunkKey(xBelow, y, z));
            newRegionNode.nextXNeg = nXNeg;
            nXNeg.nextXPos = newRegionNode;
        }
        xSet.add(x);

        // finally insert
        regions.put(pos, newRegionNode);

        return newRegionNode;
    }

    /**
     * Represents a stored chunk with 6 neighbor pointers.
     */
    public class RegionNode {
        public final long pos;
        public RegionNode nextXPos, nextXNeg;
        public RegionNode nextYPos, nextYNeg;
        public RegionNode nextZPos, nextZNeg;
        public BoundingBox boundingBox;

        public RegionNode(int x, int y, int z) {
            this.pos = Chunk.computeChunkKey(x, y, z);
            this.boundingBox = createBoundingBox(x, y, z);
        }

        public boolean hasLinkedNeighbors() {
            return nextXPos != null || nextXNeg != null || nextYPos != null || nextYNeg != null || nextZPos != null || nextZNeg != null;
        }

        private BoundingBox createBoundingBox(int x, int y, int z) {
            int minChunkX = terrainManager.getBounds().getMinChunkX(x);
            int minChunkY = terrainManager.getBounds().getMinChunkY(y);
            int minChunkZ = terrainManager.getBounds().getMinChunkZ(z);

            int maxChunkX = terrainManager.getBounds().getMaxChunkX(x);
            int maxChunkY = terrainManager.getBounds().getMaxChunkY(y);
            int maxChunkZ = terrainManager.getBounds().getMaxChunkZ(z);
            return new BoundingBox().set(
                    new Vector3(
                            minChunkX * chunkSizeX,
                            minChunkY * chunkSizeY,
                            minChunkZ * chunkSizeZ
                    ),
                    new Vector3(
                            maxChunkX * chunkSizeX + chunkSizeX - 1,
                            maxChunkY * chunkSizeY + chunkSizeY - 1,
                            maxChunkZ * chunkSizeZ + chunkSizeZ - 1
                    )
            );
        }

        @Override
        public String toString() {
            return "RegionNode{" +
                    "nextZNeg=" + (nextZNeg != null) +
                    ", nextZPos=" + (nextZPos != null) +
                    ", nextYNeg=" + (nextYNeg != null) +
                    ", nextYPos=" + (nextYPos != null) +
                    ", nextXNeg=" + (nextXNeg != null) +
                    ", nextXPos=" + (nextXPos != null) +
                    ", pos=" + pos +
                    '}';
        }
    }

    private record PlaneKey(int k1, int k2) {
    }
}
