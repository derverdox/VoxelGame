package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import lombok.Getter;
import lombok.Setter;

public class TerrainRegion implements LightAccessor {
    private final TerrainManager terrainManager;
    @Getter
    private final int regionX, regionY, regionZ;
    private final ClientChunk[] chunksInRegion;
    private final ClientChunk[] chunkHeighMap;
    private final ClientChunk[] chunkDepthMap;
    private int count;
    private short airRegions;
    private final TerrainRegion[] neighbors;
    @Getter
    private int sideOcclusionMask;
    @Getter
    @Setter
    private TerrainMesh terrainMesh;

    public TerrainRegion(TerrainManager terrainManager, int regionX, int regionY, int regionZ) {
        this.terrainManager = terrainManager;
        this.regionX = regionX;
        this.regionY = regionY;
        this.regionZ = regionZ;
        this.chunksInRegion = new ClientChunk[getBounds().regionSizeX() * getBounds().regionSizeY() * getBounds().regionSizeZ()];
        this.chunkHeighMap = new ClientChunk[getBounds().regionSizeX() * getBounds().regionSizeZ()];
        this.chunkDepthMap = new ClientChunk[getBounds().regionSizeX() * getBounds().regionSizeZ()];
        this.neighbors = new TerrainRegion[Direction.values().length];
    }

    public RegionBounds getBounds() {
        return terrainManager.getBounds();
    }

    public void disposeMesh() {
        getTerrainMesh().dispose();
        setTerrainMesh(null);
    }

    public TerrainMesh getOrCreateMesh() {
        if (terrainMesh == null) {
            this.terrainMesh = new TerrainMesh();
        }
        return this.terrainMesh;
    }

    public long getRegionKey() {
        return ChunkBase.computeChunkKey(regionX, regionY, regionZ);
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

        if (chunk.getWorld().hasNeighborsToAllSides(chunk)) {
            terrainManager.getMeshService().createChunkMesh(this, chunk);
        } else {
            for (int i = 0; i < Direction.values().length; i++) {
                Direction direction = Direction.values()[i];
                ClientChunk neighbor = chunk.getWorld().getChunkNeighborNow(chunk, direction);
                if (neighbor == null) {
                    continue;
                }
                TerrainRegion neighborRegion = terrainManager.getRegionOfChunk(neighbor.getChunkX(), neighbor.getChunkY(), neighbor.getChunkZ());
                terrainManager.getMeshService().createChunkMesh(neighborRegion, neighbor);
            }
        }


        computeRegionSideMask(chunk.getWorld());

        TerrainRegion highest = terrainManager.getHighestRegion(regionX, regionZ);
        TerrainRegion lowest = terrainManager.getLowestRegion(regionX, regionZ);

        if (highest != null && lowest != null) {
            int calcSteps = highest.getRegionY() - lowest.getRegionY();
            terrainManager.getLightEngine()
                          .scheduleSkylightUpdateInSlice(terrainManager.getWorld(), regionX, regionZ, highest, calcSteps, (x, y, z) -> {
                          });
        }
    }

    public void removeChunk(ClientChunk chunk) {
        checkIfChunkInRegion(chunk);
        int idx = getIndexInRegion(chunk);
        if (chunksInRegion[idx] != null) {
            count++;
        }
        chunksInRegion[idx] = null;
        terrainManager.getMeshService().removeChunkMesh(this, chunk);

        if (chunk.isEmpty()) {
            airRegions -= 1;
        }
        computeRegionSideMask(chunk.getWorld());
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
        computeRegionSideMask(chunk.getWorld());
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
        return this.getBounds().regionSizeX() * terrainManager.getWorld().getChunkSizeX();
    }

    @Override
    public int sizeY() {
        return this.getBounds().regionSizeY() * terrainManager.getWorld().getChunkSizeY();
    }

    @Override
    public int sizeZ() {
        return this.getBounds().regionSizeZ() * terrainManager.getWorld().getChunkSizeZ();
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
        return chunk.getChunkLightData()
                    .getBlockRed((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public byte getBlockLightBlue(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getChunkLightData()
                    .getBlockBlue((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public byte getBlockLightGreen(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getChunkLightData()
                    .getBlockGreen((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public void setBlockLight(int localX, int localY, int localZ, byte red, byte green, byte blue) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return;
        }
        chunk.getChunkLightData()
             .setBlockLight((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ), red, green, blue);
    }

    @Override
    public byte getSkyLight(int localX, int localY, int localZ) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getChunkLightData()
                    .getSkyLight((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ));
    }

    @Override
    public void setSkyLight(int localX, int localY, int localZ, byte light) {
        ChunkBase<?> chunk = getChunkInRegion(localX, localY, localZ);
        if (chunk == null) {
            return;
        }
        chunk.getChunkLightData()
             .setSkyLight((byte) chunk.localX(localX), (byte) chunk.localY(localY), (byte) chunk.localZ(localZ), light);
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
        return chunk.getHeightmap().get(chunk.localX(localX), chunk.localZ(localZ));
    }

    @Override
    public boolean isAirRegion() {
        return airRegions == chunksInRegion.length;
    }

    @Override
    public TerrainRegion getNeighbor(Direction direction) {
        return neighbors[direction.getId()];
    }

    @Override
    public TerrainRegion getRelative(int x, int y, int z) {
        return terrainManager.getRegion(regionX + x, regionY + y, regionZ + z);
    }

    void linkNeighbor(Direction direction, TerrainRegion neighbor) {
        neighbors[direction.getId()] = neighbor;
        neighbor.neighbors[direction.getOpposite().getId()] = this;
    }

    void unLinkNeighbor(Direction direction, TerrainRegion neighbor) {
        neighbors[direction.getId()] = null;
        neighbor.neighbors[direction.getOpposite().getId()] = null;
    }

    public void checkIfChunkInRegion(ClientChunk chunk) {
        int regionX = getBounds().getRegionX(chunk.getChunkX());
        int regionY = getBounds().getRegionY(chunk.getChunkY());
        int regionZ = getBounds().getRegionZ(chunk.getChunkZ());

        if (regionX != this.regionX || regionY != this.regionY || regionZ != this.regionZ) {
            throw new IllegalArgumentException("The chunk " + chunk + " does not belong to this region.");
        }
    }

    private int getIndexInRegion(ClientChunk chunk) {
        return getIndexInRegion(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    private int getIndexInRegion(int chunkX, int chunkY, int chunkZ) {
        return getBounds().getIndexInRegion(chunkX, chunkY, chunkZ);
    }

    private int getIndexInHeightMap(int chunkX, int chunkZ) {
        int xIndex = chunkX - getBounds().getMinChunkX(regionX);
        int zIndex = chunkZ - getBounds().getMinChunkZ(regionZ);

        int sizeX = getBounds().regionSizeX();
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

        int chunkX = this.getBounds().getMinChunkX(regionX) + shiftX;
        int chunkY = this.getBounds().getMinChunkY(regionY) + shiftY;
        int chunkZ = this.getBounds().getMinChunkZ(regionZ) + shiftZ;

        int idx = getIndexInRegion(chunkX, chunkY, chunkZ);
        return chunksInRegion[idx];
    }

    private ChunkBase<?> getChunkFromHeightMaps(int regionCordX, int regionCordZ, boolean getLowest) {
        int shiftX = Math.floorDiv(regionCordX, terrainManager.getWorld().getChunkSizeX());
        int shiftZ = Math.floorDiv(regionCordZ, terrainManager.getWorld().getChunkSizeZ());

        int chunkX = this.getBounds().getMinChunkX(regionX) + shiftX;
        int chunkZ = this.getBounds().getMinChunkZ(regionZ) + shiftZ;

        int idx = getIndexInHeightMap(chunkX, chunkZ);
        if (getLowest) {
            return chunkDepthMap[idx];
        }
        return chunkHeighMap[idx];
    }

    private void computeRegionSideMask(World<?> chunkProvider) {
        sideOcclusionMask = 0;


        if (isRegionFaceFullyOpaque(
                getBounds().getMinChunkX(regionX), getBounds().getMinChunkY(regionY), getBounds().getMaxChunkY(regionY), getBounds().getMinChunkZ(regionZ), getBounds().getMaxChunkZ(regionZ),
                Direction.WEST, chunkProvider))
            sideOcclusionMask |= 1 << Direction.WEST.getId();

        // X-Pos Face (EAST) an maxChunkX
        if (isRegionFaceFullyOpaque(
                getBounds().getMaxChunkX(regionX), getBounds().getMinChunkY(regionY), getBounds().getMaxChunkY(regionY), getBounds().getMinChunkZ(regionZ), getBounds().getMaxChunkZ(regionZ),
                Direction.EAST, chunkProvider))
            sideOcclusionMask |= 1 << Direction.EAST.getId();

        // Y-Neg Face (DOWN) an minChunkY
        if (isRegionFaceFullyOpaque(
                getBounds().getMinChunkY(regionY), getBounds().getMinChunkX(regionX), getBounds().getMaxChunkX(regionX), getBounds().getMinChunkZ(regionZ), getBounds().getMaxChunkZ(regionZ),
                Direction.DOWN, chunkProvider, true, false))
            sideOcclusionMask |= 1 << Direction.DOWN.getId();

        // Y-Pos Face (UP) an maxChunkY
        if (isRegionFaceFullyOpaque(
                getBounds().getMaxChunkY(regionY), getBounds().getMinChunkX(regionX), getBounds().getMaxChunkX(regionX), getBounds().getMinChunkZ(regionZ), getBounds().getMaxChunkZ(regionZ),
                Direction.UP, chunkProvider, true, false))
            sideOcclusionMask |= 1 << Direction.UP.getId();

        // Z-Neg Face (NORTH) an minChunkZ
        if (isRegionFaceFullyOpaque(
                getBounds().getMinChunkZ(regionZ), getBounds().getMinChunkX(regionX), getBounds().getMaxChunkX(regionX), getBounds().getMinChunkY(regionY), getBounds().getMaxChunkY(regionY),
                Direction.NORTH, chunkProvider, false, true))
            sideOcclusionMask |= 1 << Direction.NORTH.getId();

        // Z-Pos Face (SOUTH) an maxChunkZ
        if (isRegionFaceFullyOpaque(
                getBounds().getMaxChunkZ(regionZ), getBounds().getMinChunkX(regionX), getBounds().getMaxChunkX(regionX), getBounds().getMinChunkY(regionY), getBounds().getMaxChunkY(regionY),
                Direction.SOUTH, chunkProvider, false, true))
            sideOcclusionMask |= 1 << Direction.SOUTH.getId();
    }

    /**
     * Prüft, ob **alle** Chunks auf der angegebenen Face-Ebene diese Face voll occluden.
     *
     * @param faceCoord   der konstante Chunk-Index auf der Face (z.B. minChunkX für WEST)
     * @param rangeAStart Start des ersten freien Variablen-Ranges (y oder x)
     * @param rangeAEnd   Ende des ersten Ranges (inklusive)
     * @param rangeBStart Start des zweiten Ranges (z oder y)
     * @param rangeBEnd   Ende des zweiten Ranges
     * @param faceDir     in welche Richtung das Face zeigt
     * @param provider    zum Nachladen der Chunks
     * @param swapAB      tausche A- mit B-Koordinate (für Y-Faces)
     * @param isZFace     true, wenn die Face-Ebene in Z liegt (für NORTH/SOUTH)
     */
    private boolean isRegionFaceFullyOpaque(int faceCoord,
                                            int rangeAStart, int rangeAEnd,
                                            int rangeBStart, int rangeBEnd,
                                            Direction faceDir,
                                            World<?> provider,
                                            boolean swapAB,
                                            boolean isZFace) {
        for (int a = rangeAStart; a <= rangeAEnd; a++) {
            for (int b = rangeBStart; b <= rangeBEnd; b++) {
                int cx = swapAB ? a : faceCoord;
                int cy = swapAB ? faceCoord : (isZFace ? b : a);
                int cz = isZFace ? faceCoord : b;

                ClientChunk chunk = (ClientChunk) provider.getChunkNow(cx, cy, cz);
                if (chunk == null) return false;
                OccupancyMask mask = chunk.getChunkOccupancyMask();
                if ((mask.getSideMask() & (1L << faceDir.getId())) == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    // Überladene Variante für X-Faces
    private boolean isRegionFaceFullyOpaque(int faceX,
                                            int yStart, int yEnd,
                                            int zStart, int zEnd,
                                            Direction faceDir,
                                            World<?> provider) {
        return isRegionFaceFullyOpaque(faceX, yStart, yEnd, zStart, zEnd, faceDir, provider, false, false);
    }
}
