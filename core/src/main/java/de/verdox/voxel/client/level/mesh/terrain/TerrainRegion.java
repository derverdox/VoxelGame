package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import lombok.Getter;

public class TerrainRegion implements LightAccessor {
    private final TerrainManager terrainManager;
    @Getter
    private final int regionX, regionY, regionZ;
    private final RegionBounds bounds;
    private final ClientChunk[] chunksInRegion;
    private final ClientChunk[] chunkHeighMap;
    private final ClientChunk[] chunkDepthMap;
    private int count;
    private short airRegions;
    private final TerrainRegion[] neighbors;

    public TerrainRegion(TerrainManager terrainManager, int regionX, int regionY, int regionZ, RegionBounds bounds) {
        this.terrainManager = terrainManager;
        this.regionX = regionX;
        this.regionY = regionY;
        this.regionZ = regionZ;
        this.bounds = bounds;
        this.chunksInRegion = new ClientChunk[bounds.regionSizeX() * bounds.regionSizeY() * bounds.regionSizeZ()];
        this.chunkHeighMap = new ClientChunk[bounds.regionSizeX() * bounds.regionSizeZ()];
        this.chunkDepthMap = new ClientChunk[bounds.regionSizeX() * bounds.regionSizeZ()];
        this.neighbors = new TerrainRegion[Direction.values().length];
    }

    void linkNeighbor(Direction direction, TerrainRegion neighbor) {
        neighbors[direction.getId()] = neighbor;
        neighbor.neighbors[direction.getOpposite().getId()] = this;
    }

    void unLinkNeighbor(Direction direction, TerrainRegion neighbor) {
        neighbors[direction.getId()] = null;
        neighbor.neighbors[direction.getOpposite().getId()] = null;
    }

    public void addChunk(ClientChunk chunk) {
        checkIfChunkInRegion(chunk);
        int idx = getIndexInRegion(chunk);
        int heightIdx = getIndexInHeightMap(chunk.getChunkX(), chunk.getChunkZ());

        addToHeightMaps(chunk, heightIdx);

        if (chunksInRegion[idx] == null) {
            count++;
        }
        chunksInRegion[idx] = chunk;

        if (chunk.isEmpty()) {
            airRegions += 1;
        }

        if(isComplete()) {
            terrainManager.updateMesh(regionX, regionY, regionZ, true);
        }

        TerrainRegion highest = terrainManager.getHighestRegion(regionX, regionZ);
        TerrainRegion lowest = terrainManager.getLowestRegion(regionX, regionZ);

        if (highest != null && lowest != null) {
            int calcSteps = highest.getRegionY() - lowest.getRegionY();
            terrainManager.getLightEngine().scheduleSkylightUpdateInSlice(terrainManager.getWorld(), regionX, regionZ, highest, calcSteps, (x, y, z) -> {});
        }
    }

    public void removeChunk(ClientChunk chunk) {
        checkIfChunkInRegion(chunk);
        int idx = getIndexInRegion(chunk);
        if (chunksInRegion[idx] != null) {
            count++;
        }
        chunksInRegion[idx] = null;
        terrainManager.updateMesh(regionX, regionY, regionZ, true);

        if (chunk.isEmpty()) {
            airRegions -= 1;
        }
    }

    public boolean isComplete() {
        return count == chunksInRegion.length;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void updateChunk(ClientChunk chunk, boolean wasEmptyBefore) {
        checkIfChunkInRegion(chunk);

        if (wasEmptyBefore && !chunk.isEmpty()) {
            airRegions -= 1;
        } else if (!wasEmptyBefore && chunk.isEmpty()) {
            airRegions += 1;
        }
    }

    @Override
    public String toString() {
        return "TerrainRegion{" +
            "regionX=" + regionX +
            ", regionY=" + regionY +
            ", regionZ=" + regionZ +
            ", count=" + count +
            '}';
    }

    @Override
    public int sizeX() {
        return this.bounds.regionSizeX() * terrainManager.getWorld().getChunkSizeX();
    }

    @Override
    public int sizeY() {
        return this.bounds.regionSizeY() * terrainManager.getWorld().getChunkSizeY();
    }

    @Override
    public int sizeZ() {
        return this.bounds.regionSizeZ() * terrainManager.getWorld().getChunkSizeZ();
    }

    @Override
    public boolean isOpaque(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return false;
        }
        return chunk.getBlockAt(chunk.localX(localX), chunk.localY(localY), chunk.localZ(localZ)).isOpaque();
    }

    @Override
    public byte getEmissionRed(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getBlockAt(chunk.localX(localX), chunk.localY(localY), chunk.localZ(localZ)).getEmissionRed();
    }

    @Override
    public byte getEmissionBlue(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getBlockAt(chunk.localX(localX), chunk.localY(localY), chunk.localZ(localZ)).getEmissionBlue();
    }

    @Override
    public byte getEmissionGreen(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getBlockAt(chunk.localX(localX), chunk.localY(localY), chunk.localZ(localZ)).getEmissionGreen();
    }

    @Override
    public byte getBlockLightRed(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getChunkLightData().getBlockRed((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public byte getBlockLightBlue(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getChunkLightData().getBlockBlue((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public byte getBlockLightGreen(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getChunkLightData().getBlockGreen((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public void setBlockLight(int localX, int localY, int localZ, byte red, byte green, byte blue) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return;
        }
        chunk.getChunkLightData().setBlockLight((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ), red, green, blue);
    }

    @Override
    public byte getSkyLight(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getChunkLightData().getSkyLight((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public void setSkyLight(int localX, int localY, int localZ, byte light) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return;
        }
        chunk.getChunkLightData().setSkyLight((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ), light);
    }

    @Override
    public byte getWorldSkyLight() {
        return 15;
    }

    @Override
    public boolean isInBounds(int localX, int localY, int localZ) {
        return
            localX >= 0 && localX < sizeX() &&
                localY >= 0 && localY < sizeY() &&
                localZ >= 0 && localZ < sizeZ();
    }

    @Override
    public int getHighestNonAirBlockAt(int localX, int localZ) {
        ChunkBase<?> chunk = getChunkFromHeightMaps(localX, localZ, true);
        if (chunk == null || chunk.isEmpty()) {
            return -1;
        }
        return chunk.getHeightmap()[chunk.localX(localX)][chunk.localZ(localZ)];
    }

    @Override
    public boolean isAirRegion() {
        return airRegions == chunksInRegion.length;
    }

    @Override
    public LightAccessor getNeighbor(Direction direction) {
        return neighbors[direction.getId()];
    }

    @Override
    public LightAccessor getRelative(int x, int y, int z) {
        return terrainManager.getRegion(regionX + x, regionY + y, regionZ + z);
    }

    private void checkIfChunkInRegion(ClientChunk chunk) {
        int regionX = bounds.getRegionX(chunk.getChunkX());
        int regionY = bounds.getRegionY(chunk.getChunkY());
        int regionZ = bounds.getRegionZ(chunk.getChunkZ());

        if (regionX != this.regionX || regionY != this.regionY || regionZ != this.regionZ) {
            throw new IllegalArgumentException("The chunk " + chunk + " does not belong to this region.");
        }
    }

    private int getIndexInRegion(ClientChunk chunk) {
        return getIndexInRegion(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    private int getIndexInRegion(int chunkX, int chunkY, int chunkZ) {
        int xIndex = chunkX - bounds.getMinChunkX(regionX);
        int yIndex = chunkY - bounds.getMinChunkY(regionY);
        int zIndex = chunkZ - bounds.getMinChunkZ(regionZ);

        int sizeX = bounds.regionSizeX();
        int sizeZ = bounds.regionSizeZ();

        return xIndex
            + zIndex * sizeX
            + yIndex * (sizeX * sizeZ);
    }

    private int getIndexInHeightMap(int chunkX, int chunkZ) {
        int xIndex = chunkX - bounds.getMinChunkX(regionX);
        int zIndex = chunkZ - bounds.getMinChunkZ(regionZ);

        int sizeX = bounds.regionSizeX();
        return xIndex + zIndex * sizeX;
    }


    private void addToHeightMaps(ClientChunk chunk, int heightIdx) {
        if (chunkHeighMap[heightIdx] == null) {
            chunkHeighMap[heightIdx] = chunk;
        } else if (!chunk.isEmpty()) {
            ClientChunk highestChunk = chunkHeighMap[heightIdx];

            if (highestChunk.isEmpty() || chunk.getChunkY() > highestChunk.getChunkY()) {
                chunkHeighMap[heightIdx] = chunk;
            }
        }

        if (chunkDepthMap[heightIdx] == null) {
            chunkDepthMap[heightIdx] = chunk;
        } else if (!chunk.isEmpty()) {
            ClientChunk lowestChunk = chunkDepthMap[heightIdx];

            if (lowestChunk.isEmpty() || chunk.getChunkY() < lowestChunk.getChunkY()) {
                chunkDepthMap[heightIdx] = chunk;
            }
        }
    }

    private ChunkBase<?> getChunkInRegion(int regionCordX, int regionCordY, int regionCordZ) {
        int shiftX = Math.floorDiv(regionCordX, terrainManager.getWorld().getChunkSizeX());
        int shiftY = Math.floorDiv(regionCordY, terrainManager.getWorld().getChunkSizeY());
        int shiftZ = Math.floorDiv(regionCordZ, terrainManager.getWorld().getChunkSizeZ());

        int chunkX = this.bounds.getMinChunkX(regionX) + shiftX;
        int chunkY = this.bounds.getMinChunkY(regionY) + shiftY;
        int chunkZ = this.bounds.getMinChunkZ(regionZ) + shiftZ;

        int idx = getIndexInRegion(chunkX, chunkY, chunkZ);
        return chunksInRegion[idx];
    }

    private ChunkBase<?> getChunkFromHeightMaps(int regionCordX, int regionCordZ, boolean getLowest) {
        int shiftX = Math.floorDiv(regionCordX, terrainManager.getWorld().getChunkSizeX());
        int shiftZ = Math.floorDiv(regionCordZ, terrainManager.getWorld().getChunkSizeZ());

        int chunkX = this.bounds.getMinChunkX(regionX) + shiftX;
        int chunkZ = this.bounds.getMinChunkZ(regionZ) + shiftZ;

        int idx = getIndexInHeightMap(chunkX, chunkZ);
        if (getLowest) {
            return chunkDepthMap[idx];
        }
        return chunkHeighMap[idx];
    }
}
