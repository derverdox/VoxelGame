package de.verdox.voxel.shared.util.lod;

import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.util.RegionBounds;

public class LODUtil {
    private static final double LOG_2 = Math.log(2);

    public static int getLodScale(int lodLevel) {
        return 1 << lodLevel;
    }

    public static int getMaxLod(World world) {
        int minSize = Math.min(world.getChunkSizeX(), Math.min(world.getChunkSizeY(), world.getChunkSizeZ()));
        return (int) Math.floor(Math.log(minSize) / Math.log(2));
    }

    public static int computeLodLevel(
            RegionBounds bounds,
            int viewDistanceChunksX, int viewDistanceChunksY, int viewDistanceChunksZ,
            int chunkSizeX, int chunkSizeY, int chunkSizeZ,
            int centerRegionX, int centerRegionY, int centerRegionZ,
            int targetRegionX, int targetRegionY, int targetRegionZ,
            int maxLOD
    ) {
        int regionSizeX = bounds.regionSizeX() * chunkSizeX;
        int regionSizeY = bounds.regionSizeY() * chunkSizeY;
        int regionSizeZ = bounds.regionSizeZ() * chunkSizeZ;

        int dx = (targetRegionX - centerRegionX) * regionSizeX;
        int dy = (targetRegionY - centerRegionY) * regionSizeY;
        int dz = (targetRegionZ - centerRegionZ) * regionSizeZ;

        int distanceSquared = dx * dx + dy * dy + dz * dz;

        int baseDistanceX = viewDistanceChunksX * chunkSizeX / maxLOD;
        int baseDistanceY = viewDistanceChunksY * chunkSizeY / maxLOD;
        int baseDistanceZ = viewDistanceChunksZ * chunkSizeZ / maxLOD;

        int baseDistance = Math.max(baseDistanceX, Math.max(baseDistanceY, baseDistanceZ));

        if (distanceSquared < 1)
            return 0;

        double lodExact = 0.5 * Math.log(distanceSquared / (double) (baseDistance * baseDistance)) / LOG_2;
        int lod = (int) Math.floor(lodExact);

        //return MathUtils.clamp(lod, 0, maxLOD);
        return 0;
    }
}
