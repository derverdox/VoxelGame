package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.Gdx;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.lighting.ChunkLightEngine;
import de.verdox.voxel.shared.util.Direction;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.function.LongFunction;

public class TerrainManager {
    @Getter
    private final TerrainMeshPipeline meshPipeline;
    @Getter
    private final TerrainMeshStorage meshStorage;
    @Getter
    private final TerrainGraph terrainGraph;
    @Getter
    private final ClientWorld world;
    @Getter
    private final ChunkLightEngine lightEngine;
    private final Long2ObjectMap<TerrainRegion> terrainRegions = new Long2ObjectOpenHashMap<>();
    private final Long2IntMap highestRegions = new Long2IntOpenHashMap();
    private final Long2IntMap lowestRegions = new Long2IntOpenHashMap();

    public TerrainManager(ClientWorld world, ChunkMeshCalculator chunkMeshCalculator, int regionSizeX, int regionSizeY, int regionSizeZ) {
        this.world = world;
        this.meshPipeline = new TerrainMeshPipeline(world, chunkMeshCalculator, regionSizeX, regionSizeY, regionSizeZ);
        this.meshStorage = new TerrainMeshStorage(world, this, regionSizeX, regionSizeY, regionSizeZ);
        this.terrainGraph = new TerrainGraph(this, this.meshPipeline.getRegionBounds(), this.meshStorage, world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ());
        this.lightEngine = new ChunkLightEngine(this.meshPipeline.getRegionBounds());
        Gdx.app.log("Terrain Manager", "Initialized terrain manager with size [" + regionSizeX + ", " + regionSizeY + ", " + regionSizeZ + "]");
    }

    public void addChunk(ClientChunk chunk) {
        var bounds = this.meshPipeline.getRegionBounds();
        int regionX = bounds.getRegionX(chunk.getChunkX());
        int regionY = bounds.getRegionY(chunk.getChunkY());
        int regionZ = bounds.getRegionZ(chunk.getChunkZ());
        long regionKey = ChunkBase.computeChunkKey(regionX, regionY, regionZ);
        if(!chunk.isEmpty()) {
            terrainGraph.addRegion(regionX, regionY, regionZ);
        }

        TerrainRegion terrainRegion;
        if (!terrainRegions.containsKey(regionKey)) {
            TerrainRegion newRegion = new TerrainRegion(this, regionX, regionY, regionZ, bounds);

            for (int i = 0; i < Direction.values().length; i++) {
                Direction dir = Direction.values()[i];
                long neighborKey = ChunkBase.computeChunkKey(regionX + dir.getOffsetX(), regionY + dir.getOffsetY(), regionZ + dir.getOffsetZ());

                if (!terrainRegions.containsKey(neighborKey)) {
                    terrainRegions.put(neighborKey, new TerrainRegion(this, regionX + dir.getOffsetX(), regionY + dir.getOffsetY(), regionZ + dir.getOffsetZ(), bounds));
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

        long heightKey = ChunkBase.computeChunkKey(regionX, 0, regionZ);
        if (!highestRegions.containsKey(heightKey) || regionY > highestRegions.get(heightKey)) {
            highestRegions.put(heightKey, regionY);
        }

        if (!lowestRegions.containsKey(heightKey) || regionY < lowestRegions.get(heightKey)) {
            lowestRegions.put(heightKey, regionY);
        }
    }

    public void removeChunk(ClientChunk chunk) {
        var bounds = this.meshPipeline.getRegionBounds();
        int regionX = bounds.getRegionX(chunk.getChunkX());
        int regionY = bounds.getRegionY(chunk.getChunkY());
        int regionZ = bounds.getRegionZ(chunk.getChunkZ());
        long regionKey = ChunkBase.computeChunkKey(regionX, regionY, regionZ);

        TerrainRegion terrainRegion = terrainRegions.get(regionKey);
        if (terrainRegion != null) {
            terrainRegion.removeChunk(chunk);
            if (terrainRegion.isEmpty()) {
                terrainRegions.remove(regionKey);

                for (int i = 0; i < Direction.values().length; i++) {
                    Direction dir = Direction.values()[i];
                    long neighborKey = ChunkBase.computeChunkKey(regionX + dir.getOffsetX(), regionY + dir.getOffsetY(), regionZ + dir.getOffsetZ());

                    if (terrainRegions.containsKey(neighborKey)) {
                        TerrainRegion neighborRegion = terrainRegions.get(neighborKey);
                        terrainRegion.unLinkNeighbor(dir, neighborRegion);
                    }
                }

                terrainGraph.removeRegion(regionX, regionY, regionZ);
            }
        }

        long heightKey = ChunkBase.computeChunkKey(regionX, 0, regionZ);
        if (highestRegions.containsKey(heightKey) && regionY > highestRegions.get(heightKey)) {
            highestRegions.put(heightKey, regionY - 1);
        }

        if (lowestRegions.containsKey(heightKey) && regionY < lowestRegions.get(heightKey)) {
            lowestRegions.put(heightKey, regionY + 1);
        }
    }

    public void afterChunkUpdate(ClientChunk chunk, boolean wasEmptyBefore) {
        var bounds = this.meshPipeline.getRegionBounds();

        int regionX = bounds.getRegionX(chunk.getChunkX());
        int regionY = bounds.getRegionY(chunk.getChunkY());
        int regionZ = bounds.getRegionZ(chunk.getChunkZ());
        long regionKey = ChunkBase.computeChunkKey(regionX, regionY, regionZ);

        TerrainRegion terrainRegion = terrainRegions.get(regionKey);
        if (terrainRegion != null) {
            terrainRegion.updateChunk(chunk, wasEmptyBefore);
        }
    }

    public void updateMesh(TerrainRegion terrainRegion, boolean updateNeighbors) {
        this.meshStorage.recalculateMesh(terrainRegion.getRegionX(), terrainRegion.getRegionY(), terrainRegion.getRegionZ(), terrainRegion, true);
        if (!updateNeighbors) return;
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            TerrainRegion neighbor = terrainRegion.getNeighbor(direction);
            if (neighbor == null) {
                continue;
            }

            this.meshStorage.recalculateMesh(neighbor.getRegionX() + direction.getOffsetX(), neighbor.getRegionY() + direction.getOffsetY(), neighbor.getRegionZ() + direction.getOffsetZ(), neighbor, true);
        }
    }

    public TerrainRegion getRegion(int regionX, int regionY, int regionZ) {
        long regionKey = ChunkBase.computeChunkKey(regionX, regionY, regionZ);
        return terrainRegions.get(regionKey);
    }

    public TerrainRegion getHighestRegion(int regionX, int regionZ) {
        long heightKey = ChunkBase.computeChunkKey(regionX, 0, regionZ);
        return getRegion(regionX, highestRegions.getOrDefault(heightKey, 0), regionZ);
    }

    public TerrainRegion getLowestRegion(int regionX, int regionZ) {
        long heightKey = ChunkBase.computeChunkKey(regionX, 0, regionZ);
        return getRegion(regionX, lowestRegions.getOrDefault(heightKey, 0), regionZ);
    }
}
