package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.util.*;

public class TerrainGraph {
    private final Long2ObjectOpenHashMap<RegionNode> regions = new Long2ObjectOpenHashMap<>();

    private final Map<PlaneKey, NavigableSet<Integer>> xyIndex = new HashMap<>();
    private final Map<PlaneKey, NavigableSet<Integer>> xzIndex = new HashMap<>();
    private final Map<PlaneKey, NavigableSet<Integer>> yzIndex = new HashMap<>();
    private final TerrainManager terrainManager;
    private final RegionBounds bounds;
    private final TerrainMeshStorage storage;
    private final int chunkSizeX;
    private final int chunkSizeY;
    private final int chunkSizeZ;

    public TerrainGraph(TerrainManager terrainManager, RegionBounds bounds, TerrainMeshStorage storage, int chunkSizeX, int chunkSizeY, int chunkSizeZ) {
        this.terrainManager = terrainManager;
        this.bounds = bounds;
        this.storage = storage;
        this.chunkSizeX = chunkSizeX;
        this.chunkSizeY = chunkSizeY;
        this.chunkSizeZ = chunkSizeZ;
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
        private int visibleChunksInRegion;

        public RegionNode(int x, int y, int z) {
            this.pos = ChunkBase.computeChunkKey(x, y, z);
            this.boundingBox = createBoundingBox(x, y, z);
        }

        public boolean hasLinkedNeighbors() {
            return nextXPos != null || nextXNeg != null || nextYPos != null || nextYNeg != null || nextZPos != null || nextZNeg != null;
        }

        public void incrementVisibleChunks() {
            this.visibleChunksInRegion += 1;
        }

        public boolean decrementVisibleChunks() {
            this.visibleChunksInRegion -= 1;
            if (this.visibleChunksInRegion == 0) {
                removeRegion(ChunkBase.unpackChunkX(pos), ChunkBase.unpackChunkY(pos), ChunkBase.unpackChunkZ(pos));
                return true;
            }
            return false;
        }

        private BoundingBox createBoundingBox(int x, int y, int z) {
            int minChunkX = bounds.getMinChunkX(x);
            int minChunkY = bounds.getMinChunkY(y);
            int minChunkZ = bounds.getMinChunkZ(z);

            int maxChunkX = bounds.getMaxChunkX(x);
            int maxChunkY = bounds.getMaxChunkY(y);
            int maxChunkZ = bounds.getMaxChunkZ(z);
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
                ", visibleChunksInRegion=" + visibleChunksInRegion +
                '}';
        }
    }

    private record PlaneKey(int k1, int k2) {
    }

    public RegionNode addRegion(ClientChunk chunk) {
        return addRegion(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    public RegionNode addRegionByChunkCoords(int chunkX, int chunkY, int chunkZ) {
        int regionX = bounds.getRegionX(chunkX);
        int regionY = bounds.getRegionY(chunkY);
        int regionZ = bounds.getRegionZ(chunkZ);
        return addRegion(regionX, regionY, regionZ);
    }

    public RegionNode getRegion(ClientChunk chunk) {
        return getRegion(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    public RegionNode getRegionByChunkCoords(int chunkX, int chunkY, int chunkZ) {
        int regionX = bounds.getRegionX(chunkX);
        int regionY = bounds.getRegionY(chunkY);
        int regionZ = bounds.getRegionZ(chunkZ);
        return getRegion(regionX, regionY, regionZ);
    }

    /**
     * Adds a chunk at (x,y,z). Updates neighbor pointers in O(log n).
     */
    public RegionNode addRegion(int x, int y, int z) {
        long pos = ChunkBase.computeChunkKey(x, y, z);
        if (regions.containsKey(pos)) return regions.get(pos);
        RegionNode newRegionNode = new RegionNode(x, y, z);
        // XY-plane index for Z
        NavigableSet<Integer> zSet = xyIndex.computeIfAbsent(new PlaneKey(x, y), k -> new TreeSet<>());
        Integer zAbove = zSet.higher(z);
        Integer zBelow = zSet.lower(z);

        // link along Z
        if (zAbove != null) {
            RegionNode above = regions.get(ChunkBase.computeChunkKey(x, y, zAbove));
            newRegionNode.nextZPos = above;
            above.nextZNeg = newRegionNode;
        }
        if (zBelow != null) {
            RegionNode below = regions.get(ChunkBase.computeChunkKey(x, y, zBelow));
            newRegionNode.nextZNeg = below;
            below.nextZPos = newRegionNode;
        }
        zSet.add(z);

        // XZ-plane index for Y
        NavigableSet<Integer> ySet = xzIndex.computeIfAbsent(new PlaneKey(x, z), k -> new TreeSet<>());
        Integer yAbove = ySet.higher(y);
        Integer yBelow = ySet.lower(y);
        if (yAbove != null) {
            RegionNode nYPos = regions.get(ChunkBase.computeChunkKey(x, yAbove, z));
            newRegionNode.nextYPos = nYPos;
            nYPos.nextYNeg = newRegionNode;
        }
        if (yBelow != null) {
            RegionNode nYNeg = regions.get(ChunkBase.computeChunkKey(x, yBelow, z));
            newRegionNode.nextYNeg = nYNeg;
            nYNeg.nextYPos = newRegionNode;
        }
        ySet.add(y);

        // YZ-plane index for X
        NavigableSet<Integer> xSet = yzIndex.computeIfAbsent(new PlaneKey(y, z), k -> new TreeSet<>());
        Integer xAbove = xSet.higher(x);
        Integer xBelow = xSet.lower(x);
        if (xAbove != null) {
            RegionNode nXPos = regions.get(ChunkBase.computeChunkKey(xAbove, y, z));
            newRegionNode.nextXPos = nXPos;
            nXPos.nextXNeg = newRegionNode;
        }
        if (xBelow != null) {
            RegionNode nXNeg = regions.get(ChunkBase.computeChunkKey(xBelow, y, z));
            newRegionNode.nextXNeg = nXNeg;
            nXNeg.nextXPos = newRegionNode;
        }
        xSet.add(x);

        // finally insert
        regions.put(pos, newRegionNode);

        return newRegionNode;
    }

    /**
     * Removes chunk at (x,y,z). Updates neighbor pointers.
     */
    public void removeRegion(int x, int y, int z) {
        long pos = ChunkBase.computeChunkKey(x, y, z);
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

    public RegionNode getRegion(int x, int y, int z) {
        return regions.get(ChunkBase.computeChunkKey(x, y, z));
    }

    /**
     * Findet die Region, die an (x,y,z) liegt oder – falls dort keine existiert – die nächste vorhandene Region.
     *
     * @param x Ziel-X
     * @param y Ziel-Y
     * @param z Ziel-Z
     * @return entweder der exakt an (x,y,z) liegende Knoten oder der Knoten mit dem kleinsten euklidischen Abstand;
     * null, falls überhaupt keine Region gespeichert ist.
     */
    private RegionNode findNearestRegionInFrustum(Camera camera, int x, int y, int z, int maxStepsX, int maxStepsY, int maxStepsZ) {
        long key = ChunkBase.computeChunkKey(x, y, z);
        RegionNode exact = regions.get(key);
        if (exact != null) {
            return exact;
        }

        Set<Long> visited = new HashSet<>();
        Queue<Long> toVisit = new ArrayDeque<>();
        visited.add(key);
        toVisit.add(key);

        while (!toVisit.isEmpty()) {
            long nextToCheck = toVisit.poll();
            int nx = ChunkBase.unpackChunkX(nextToCheck);
            int ny = ChunkBase.unpackChunkY(nextToCheck);
            int nz = ChunkBase.unpackChunkZ(nextToCheck);

            RegionNode node = regions.get(nextToCheck);
            if (node != null && camera.frustum.boundsInFrustum(node.boundingBox)) {
                return node;
            }
            visited.add(nextToCheck);

            for (int i = 0; i < Direction.values().length; i++) {
                Direction direction = Direction.values()[i];

                int relX = nx + direction.getOffsetX();
                int relY = ny + direction.getOffsetY();
                int relZ = nz + direction.getOffsetZ();

                // Check if in view distance
                if (
                    relX > x + maxStepsX || relX < x - maxStepsX ||
                        relY > y + maxStepsY || relY < y - maxStepsY ||
                        relZ > z + maxStepsZ || relZ < z - maxStepsZ
                ) {
                    continue;
                }

                long neighborKey = ChunkBase.computeChunkKey(relX, relY, relZ);
                if (visited.contains(neighborKey)) {
                    continue;
                }
                toVisit.add(neighborKey);
            }
        }
        return null;
    }

    private final Set<Long> visited = new HashSet<>();
    private final Deque<Long> queue = new ArrayDeque<>();

    private final LongList foundRegions = new LongArrayList();
    private int lastStartChunkX = Integer.MIN_VALUE;
    private int lastStartChunkY = Integer.MIN_VALUE;
    private int lastStartChunkZ = Integer.MIN_VALUE;

    public int bsfRenderVisibleRegions(Camera camera, ClientWorld world, ModelBatch batch, int viewDistanceX, int viewDistanceY, int viewDistanceZ) {
        visited.clear();
        queue.clear();
        foundRegions.clear();

        int amountOfRenderedBlockFaces = 0;

        int startChunkX = ChunkBase.chunkX(chunkSizeX, (int) camera.position.x);
        int startChunkY = ChunkBase.chunkY(chunkSizeY, (int) camera.position.y);
        int startChunkZ = ChunkBase.chunkZ(chunkSizeZ, (int) camera.position.z);

        this.lastStartChunkX = startChunkX;
        this.lastStartChunkY = startChunkY;
        this.lastStartChunkZ = startChunkZ;

        int regionX = bounds.getRegionX(startChunkX);
        int regionY = bounds.getRegionY(startChunkY);
        int regionZ = bounds.getRegionZ(startChunkZ);
        long regionKey = ChunkBase.computeChunkKey(regionX, regionY, regionZ);

        RegionNode startNode = regions.get(regionKey);
        boolean removeCameraNodeAfter = false;
        if (startNode == null) {
            removeCameraNodeAfter = true;
            startNode = addRegion(regionX, regionY, regionZ);
            regionKey = startNode.pos;
        }

        visited.add(regionKey);
        queue.add(regionKey);

        while (!queue.isEmpty()) {
            long key = queue.poll();
            RegionNode node = regions.get(key);
            if (node == null) {
                continue;
            }

            int nx = ChunkBase.unpackChunkX(key);
            int ny = ChunkBase.unpackChunkY(key);
            int nz = ChunkBase.unpackChunkZ(key);

            if (!startNode.equals(node)) {
                if (Math.abs(startChunkX - nx) > (viewDistanceX) ||
                    Math.abs(startChunkY - ny) > (viewDistanceY) ||
                    Math.abs(startChunkZ - nz) > (viewDistanceZ)) {
                    continue;
                }
            }
            if (camera.frustum.boundsInFrustum(node.boundingBox)) {
                TerrainMesh terrainMesh = world.getTerrainManager().getMeshStorage().getRegionMeshIfAvailable(nx, ny, nz);

                if (terrainMesh != null && terrainMesh.getAmountOfBlockFaces() != 0 && terrainMesh.isComplete()) {

                    TerrainRegion terrainRegion = terrainManager.getRegion(nx, ny, nz);
                    if (terrainRegion != null) {
                        var mesh = terrainMesh.getOrGenerateMeshFromFaces(TextureAtlasManager.getInstance().getBlockTextureAtlas(), world, nx, ny, nz, terrainRegion);
                        mesh.render(camera, batch);

                        amountOfRenderedBlockFaces += terrainMesh.getAmountOfBlockFaces();
                    }
                }
            }
            foundRegions.add(ChunkBase.computeChunkKey(nx, ny, nz));

            if (!node.hasLinkedNeighbors()) {
                continue;
            }


            for (RegionNode neigh : new RegionNode[]{
                node.nextXPos, node.nextXNeg,
                node.nextYPos, node.nextYNeg,
                node.nextZPos, node.nextZNeg}) {
                if (neigh != null && visited.add(neigh.pos)) {
                    queue.add(neigh.pos);
                }
            }
        }

        if (removeCameraNodeAfter) {
            removeRegion(regionX, regionY, regionZ);
        }
        return amountOfRenderedBlockFaces;
    }


}
