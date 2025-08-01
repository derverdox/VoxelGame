package de.verdox.voxel.shared.util;

import de.verdox.voxel.shared.level.chunk.ChunkBase;

public record RegionBounds(int regionSizeX, int regionSizeY, int regionSizeZ) {



    public int getOffsetX(int chunkX) {
        int regionX = getRegionX(chunkX);
        int minChunkX = getMinChunkX(regionX);

        return chunkX - minChunkX;
    }

    public int getOffsetY(int chunkY) {
        int regionY = getRegionY(chunkY);
        int minChunkY = getMinChunkY(regionY);

        return chunkY - minChunkY;
    }

    public int getOffsetZ(int chunkZ) {
        int regionZ = getRegionZ(chunkZ);
        int minChunkZ = getMinChunkZ(regionZ);

        return chunkZ - minChunkZ;
    }

    public int getRegionX(int chunkX) {
        return Math.floorDiv(chunkX, regionSizeX);
    }

    public int getMinChunkX(int regionX) {
        return regionX * regionSizeX;
    }

    public int getMaxChunkX(int regionX) {
        return getMinChunkX(regionX) + regionSizeX - 1;
    }

    public int getRegionY(int chunkY) {
        return Math.floorDiv(chunkY, regionSizeY);
    }

    public int getMinChunkY(int regionY) {
        return regionY * regionSizeY;
    }

    public int getMaxChunkY(int regionY) {
        return getMinChunkY(regionY) + regionSizeY - 1;
    }

    public int getRegionZ(int chunkZ) {
        return Math.floorDiv(chunkZ, regionSizeZ);
    }

    public int getMinChunkZ(int regionZ) {
        return regionZ * regionSizeZ;
    }

    public int getMaxChunkZ(int regionZ) {
        return getMinChunkZ(regionZ) + regionSizeZ - 1;
    }

    public long getRegionKey(int regionX, int regionY, int regionZ) {
        return ChunkBase.computeChunkKey(regionX, regionY, regionZ);
    }

    public long getRegionKeyFromChunk(long chunkKey) {
        int chunkX = ChunkBase.unpackChunkX(chunkKey);
        int chunkY = ChunkBase.unpackChunkY(chunkKey);
        int chunkZ = ChunkBase.unpackChunkZ(chunkKey);
        return getRegionKeyFromChunk(chunkX, chunkY, chunkZ);
    }

    public long getRegionKeyFromChunk(int chunkX, int chunkY, int chunkZ) {
        return ChunkBase.computeChunkKey(getRegionX(chunkX), getRegionY(chunkY), getRegionZ(chunkZ));
    }

    public int getIndexInRegion(int chunkX, int chunkY, int chunkZ) {
        int xIndex = chunkX - getMinChunkX(getRegionX(chunkX));
        int yIndex = chunkY - getMinChunkY(getRegionY(chunkY));
        int zIndex = chunkZ - getMinChunkZ(getRegionZ(chunkZ));

        int sizeX = regionSizeX();
        int sizeZ = regionSizeZ();

        return xIndex
                + zIndex * sizeX
                + yIndex * (sizeX * sizeZ);
    }

    /**
     * Calculate the minimum block X coordinate for a given region.
     * @param regionX region index in X
     * @param chunkSizeX size of one chunk in blocks along X
     * @return global minimum block X coordinate of the region
     */
    public int getMinBlockX(int regionX, int chunkSizeX) {
        return getMinChunkX(regionX) * chunkSizeX;
    }

    /**
     * Calculate the maximum block X coordinate for a given region.
     * @param regionX region index in X
     * @param chunkSizeX size of one chunk in blocks along X
     * @return global maximum block X coordinate of the region
     */
    public int getMaxBlockX(int regionX, int chunkSizeX) {
        return getMaxChunkX(regionX) * chunkSizeX + (chunkSizeX - 1);
    }

    /**
     * Calculate the minimum block Y coordinate for a given region.
     * @param regionY region index in Y
     * @param chunkSizeY size of one chunk in blocks along Y
     * @return global minimum block Y coordinate of the region
     */
    public int getMinBlockY(int regionY, int chunkSizeY) {
        return getMinChunkY(regionY) * chunkSizeY;
    }

    /**
     * Calculate the maximum block Y coordinate for a given region.
     * @param regionY region index in Y
     * @param chunkSizeY size of one chunk in blocks along Y
     * @return global maximum block Y coordinate of the region
     */
    public int getMaxBlockY(int regionY, int chunkSizeY) {
        return getMaxChunkY(regionY) * chunkSizeY + (chunkSizeY - 1);
    }

    /**
     * Calculate the minimum block Z coordinate for a given region.
     * @param regionZ region index in Z
     * @param chunkSizeZ size of one chunk in blocks along Z
     * @return global minimum block Z coordinate of the region
     */
    public int getMinBlockZ(int regionZ, int chunkSizeZ) {
        return getMinChunkZ(regionZ) * chunkSizeZ;
    }

    /**
     * Calculate the maximum block Z coordinate for a given region.
     * @param regionZ region index in Z
     * @param chunkSizeZ size of one chunk in blocks along Z
     * @return global maximum block Z coordinate of the region
     */
    public int getMaxBlockZ(int regionZ, int chunkSizeZ) {
        return getMaxChunkZ(regionZ) * chunkSizeZ + (chunkSizeZ - 1);
    }
}
