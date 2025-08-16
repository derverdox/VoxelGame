package de.verdox.voxel.client.renderer.terrain.regions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.level.TerrainManager;
import de.verdox.voxel.client.renderer.ClientRenderer;
import de.verdox.voxel.client.renderer.mesh.chunk.ChunkMeshCalculator;
import de.verdox.voxel.client.renderer.terrain.regions.mesh.RegionMeshCalculator;
import de.verdox.voxel.client.renderer.terrain.regions.graph.OctreeRegionBasedTerrainRenderGraph;
import de.verdox.voxel.client.renderer.terrain.regions.graph.RegionBasedTerrainRenderGraph;
import de.verdox.voxel.shared.util.TerrainRenderStats;
import de.verdox.voxel.shared.util.lod.LODUtil;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.lighting.ChunkLightEngine;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

public class RegionalizedTerrainManager implements TerrainManager {
    @Getter
    private final RegionBasedTerrainRenderGraph regionBasedTerrainRenderGraph;
    @Getter
    private final TerrainMeshService meshService;
    @Getter
    private final ClientWorld world;
    @Getter
    private final ChunkLightEngine lightEngine;
    private final Long2ObjectMap<TerrainRegion> terrainRegions = new Long2ObjectOpenHashMap<>();
    private final Long2IntMap highestRegions = new Long2IntOpenHashMap();
    private final Long2IntMap lowestRegions = new Long2IntOpenHashMap();

    @Getter
    private final RegionBounds bounds;

    @Getter
    private int centerRegionX, centerRegionY, centerRegionZ;

    @Getter
    private int centerChunkX, centerChunkY, centerChunkZ;

    public RegionalizedTerrainManager(ClientWorld world, RegionMeshCalculator regionMeshCalculator, ChunkMeshCalculator chunkMeshCalculator, int regionSizeX, int regionSizeY, int regionSizeZ) {
        Gdx.app.log("Terrain Manager", "Initialized terrain manager with size [" + regionSizeX + ", " + regionSizeY + ", " + regionSizeZ + "]");
        this.world = world;
        this.bounds = new RegionBounds(regionSizeX, regionSizeY, regionSizeZ);
        this.meshService = new TerrainMeshService(this, regionMeshCalculator, chunkMeshCalculator);

        this.regionBasedTerrainRenderGraph = new OctreeRegionBasedTerrainRenderGraph(this, 4, 8, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance);
        this.lightEngine = new ChunkLightEngine();
    }

    @Override
    public void setCameraChunk(int chunkX, int chunkY, int chunkZ) {
        centerChunkX = chunkX;
        centerChunkY = chunkY;
        centerChunkZ = chunkZ;

        centerRegionX = bounds.getRegionX(chunkX);
        centerRegionY = bounds.getRegionY(chunkY);
        centerRegionZ = bounds.getRegionZ(chunkZ);
    }

    @Override
    public TerrainChunk getChunkNow(int chunkX, int chunkY, int chunkZ) {
        int regionX = bounds.getRegionX(chunkX);
        int regionY = bounds.getRegionY(chunkY);
        int regionZ = bounds.getRegionZ(chunkZ);

        TerrainRegion terrainRegion = getRegion(regionX, regionY, regionZ);
        if (terrainRegion == null) {
            return null;
        }
        return terrainRegion.getTerrainChunk(chunkX, chunkY, chunkZ);
    }

    @Override
    public void addChunk(Chunk chunk) {
        int regionX = bounds.getRegionX(chunk.getChunkX());
        int regionY = bounds.getRegionY(chunk.getChunkY());
        int regionZ = bounds.getRegionZ(chunk.getChunkZ());

        long regionKey = Chunk.computeChunkKey(regionX, regionY, regionZ);
        regionBasedTerrainRenderGraph.addRegion(regionX, regionY, regionZ);

        TerrainRegion terrainRegion;
        if (!terrainRegions.containsKey(regionKey)) {
            TerrainRegion newRegion = new TerrainRegion(this, regionX, regionY, regionZ);

            for (int i = 0; i < Direction.values().length; i++) {
                Direction dir = Direction.values()[i];
                long neighborKey = Chunk.computeChunkKey(regionX + dir.getOffsetX(), regionY + dir.getOffsetY(), regionZ + dir.getOffsetZ());

                if (!terrainRegions.containsKey(neighborKey)) {
                    terrainRegions.put(neighborKey, new TerrainRegion(this, regionX + dir.getOffsetX(), regionY + dir.getOffsetY(), regionZ + dir.getOffsetZ()));
                }
                TerrainRegion neighborRegion = terrainRegions.get(neighborKey);
                newRegion.linkNeighbor(dir, neighborRegion);
            }

            terrainRegions.put(regionKey, newRegion);
            terrainRegion = newRegion;
        } else {
            terrainRegion = terrainRegions.get(regionKey);
        }
        terrainRegion.addChunk(chunk);

        long heightKey = Chunk.computeChunkKey(regionX, 0, regionZ);
        if (!highestRegions.containsKey(heightKey) || regionY > highestRegions.get(heightKey)) {
            highestRegions.put(heightKey, regionY);
        }

        if (!lowestRegions.containsKey(heightKey) || regionY < lowestRegions.get(heightKey)) {
            lowestRegions.put(heightKey, regionY);
        }
    }

    @Override
    public void removeChunk(Chunk chunk) {
        int regionX = bounds.getRegionX(chunk.getChunkX());
        int regionY = bounds.getRegionY(chunk.getChunkY());
        int regionZ = bounds.getRegionZ(chunk.getChunkZ());
        long regionKey = Chunk.computeChunkKey(regionX, regionY, regionZ);

        TerrainRegion terrainRegion = terrainRegions.get(regionKey);
        if (terrainRegion != null) {
            terrainRegion.removeChunk(chunk);
            if (terrainRegion.isEmpty()) {
                terrainRegions.remove(regionKey);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction dir = Direction.values()[i];
                    long neighborKey = Chunk.computeChunkKey(regionX + dir.getOffsetX(), regionY + dir.getOffsetY(), regionZ + dir.getOffsetZ());

                    if (terrainRegions.containsKey(neighborKey)) {
                        TerrainRegion neighborRegion = terrainRegions.get(neighborKey);
                        terrainRegion.unLinkNeighbor(dir, neighborRegion);
                    }
                }

                regionBasedTerrainRenderGraph.removeRegion(regionX, regionY, regionZ);
                if (terrainRegion.getTerrainMesh() != null) {
                    terrainRegion.disposeMesh();
                }
            }
        }

        long heightKey = Chunk.computeChunkKey(regionX, 0, regionZ);
        if (highestRegions.containsKey(heightKey) && regionY > highestRegions.get(heightKey)) {
            highestRegions.put(heightKey, regionY - 1);
        }

        if (lowestRegions.containsKey(heightKey) && regionY < lowestRegions.get(heightKey)) {
            lowestRegions.put(heightKey, regionY + 1);
        }
    }

    @Override
    public void afterChunkUpdate(Chunk chunk, boolean wasEmptyBefore) {
        int regionX = bounds.getRegionX(chunk.getChunkX());
        int regionY = bounds.getRegionY(chunk.getChunkY());
        int regionZ = bounds.getRegionZ(chunk.getChunkZ());
        long regionKey = Chunk.computeChunkKey(regionX, regionY, regionZ);

        TerrainRegion terrainRegion = terrainRegions.get(regionKey);
        if (terrainRegion != null) {
            terrainRegion.updateChunk(chunk, wasEmptyBefore);
        }
    }

    @Override
    public int renderTerrain(Camera camera, ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ, TerrainRenderStats renderStats) {
        return regionBasedTerrainRenderGraph.renderTerrain(camera, world, viewDistanceX, viewDistanceY, viewDistanceZ, renderStats);
    }

    public TerrainRegion getRegionOfChunk(int chunkX, int chunkY, int chunkZ) {
        long regionKey = bounds.getRegionKeyFromChunk(chunkX, chunkY, chunkZ);
        return terrainRegions.get(regionKey);
    }

    public TerrainRegion getRegion(int regionX, int regionY, int regionZ) {
        long regionKey = Chunk.computeChunkKey(regionX, regionY, regionZ);
        return terrainRegions.get(regionKey);
    }

    public TerrainRegion getHighestRegion(int regionX, int regionZ) {
        long heightKey = Chunk.computeChunkKey(regionX, 0, regionZ);
        return getRegion(regionX, highestRegions.getOrDefault(heightKey, 0), regionZ);
    }

    public TerrainRegion getLowestRegion(int regionX, int regionZ) {
        long heightKey = Chunk.computeChunkKey(regionX, 0, regionZ);
        return getRegion(regionX, lowestRegions.getOrDefault(heightKey, 0), regionZ);
    }

    public int computeLodLevel(
            int centerRegionX, int centerRegionY, int centerRegionZ,
            int targetRegionX, int targetRegionY, int targetRegionZ
    ) {
        return LODUtil.computeLodLevel(
                getBounds(),
                ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance,
                getWorld().getChunkSizeX(),
                getWorld().getChunkSizeY(),
                getWorld().getChunkSizeZ(),
                centerRegionX, centerRegionY, centerRegionZ,
                targetRegionX, targetRegionY, targetRegionZ,
                LODUtil.getMaxLod(getWorld())
        );
    }
}
