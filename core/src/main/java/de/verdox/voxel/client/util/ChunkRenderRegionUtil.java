package de.verdox.voxel.client.util;

public class ChunkRenderRegionUtil {
    private static final int REGION_SIZE_GROWTH_FACTOR = 3;

    public static int countRegionsForChunkDistanceOnAxis(int distance) {
        if (distance <= 0) return 0;
        double x = Math.log(2.0 * distance + 1) / Math.log(REGION_SIZE_GROWTH_FACTOR);
        return Math.max(0, (int) Math.ceil(x) - 1) + 1;
    }

    public static void findRegion(int centerX, int centerY, int centerZ, int chunkX, int chunkY, int chunkZ, int[] result) {
        int deltaX = chunkX - centerX;
        int deltaY = chunkY - centerY;
        int deltaZ = chunkZ - centerZ;

        if (deltaX == 0 && deltaY == 0 && deltaZ == 0) {
            result[0] = 0;
            result[1] = 0;
            result[2] = 0;
            return;
        }

        int passedRegionsByDistanceToCenterX = getRegionsCoveredOnDistanceToCenter(deltaX);
        int passedRegionsByDistanceToCenterY = getRegionsCoveredOnDistanceToCenter(deltaY);
        int passedRegionsByDistanceToCenterZ = getRegionsCoveredOnDistanceToCenter(deltaZ);

        // Find primary axis. We traverse the region tree first by its greatest step size. Then we are inside the large region -> The coordinate system is shifted.
        int[] passes = {passedRegionsByDistanceToCenterX, passedRegionsByDistanceToCenterY, passedRegionsByDistanceToCenterZ};
        int primaryAxis = 0;
        for (int i = 1; i < passes.length; i++) {
            if (Math.abs(passes[i]) > Math.abs(passes[primaryAxis])) {
                primaryAxis = i;
            }
        }

        result[primaryAxis] = passes[primaryAxis];
        for (int otherAxis = primaryAxis - 2; otherAxis <= primaryAxis + 2; otherAxis++) {
            if (otherAxis == primaryAxis || otherAxis < 0 || otherAxis >= result.length) {
                continue;
            }
            // In the largest regions coordinate system we calculate the coordinate of the region in the other two axes which are <=
            result[otherAxis] = passes[otherAxis] / result[primaryAxis];
        }
    }

    public static int getDistanceCoveredInChunksAtLevel(int level) {
        return (int) ((Math.pow(3, level) + 1) / 2) - 1;
    }

    /**
     * Inverse zu coveredDistance(level) = (3^level + 1)/2.
     * Liefert den Level, für den coveredDistance(level) == coveredDist gilt.
     * Wir nutzen hier die geschlossene Formel 3^level = 2*n - 1
     * und berechnen level = log_3(2*n-1).
     */
    public static int getRegionsCoveredOnDistanceToCenter(int coveredDist) {
        if (coveredDist == 0) {
            return 0;
        }
        int result = (int) Math.ceil((Math.log((2 * Math.abs(coveredDist)) + 1) / Math.log(3)));
        return coveredDist < 0 ? result * -1 : result;
    }

    /**
     * Summe der Größen der Regionen 0..(k-1):
     * level=0 → 0
     * level=1 → size(0)=1
     * level≥2 → 1 + 1 + 3 + … + 3^(k-2) = (3^(k-1)+1)/2
     */
    public static int coveredBeforeLevel(int k) {
        if (k <= 0) {
            return 0;
        } else if (k == 1) {
            return 1;
        } else {
            return ((int) Math.pow(3, k - 1) + 1) / 2;
        }
    }

    /**
     * Größe des Würfels auf Level k:
     * level=0 → 1
     * level=1 → 1
     * level=2 → 3
     * level=3 → 9
     * …
     */
    public static int getRegionSizeForLevel(int level) {
        int k = Math.abs(level);
        if (k <= 1) {
            return 1;
        }
        return (int) Math.pow(3, k - 1);
    }

    /**
     * Rechnet für eine Achse den minimalen Chunk‐Index einer Region
     * mit gegebenem regionIndex (k kann negativ sein für die “linke” Seite).
     */
    public static int computeMinChunkForRegion(int center, int regionIndex, int maxLevel) {
        // This is the center
        if (maxLevel == 0) {
            return center;
        } else if (maxLevel == 1) {
            return center + regionIndex;
        }

        int size = getRegionSizeForLevel(maxLevel);
        int prev = coveredBeforeLevel(maxLevel);

        if (regionIndex == 0) {
            // zentrierter Würfel: Min = center - (size-1)/2
            return center - (size - 1) / 2;
        } else if (regionIndex > 0) {
            // positive Seite: ab dem „inneren Offset“ prev
            return center + prev;
        } else {
            // negative Seite: symmetrisch zur positiven Seite
            return center - (prev + size - 1);
        }
    }
}
