package de.verdox.voxel.client.test.chunk;

import de.verdox.voxel.client.util.ChunkRenderRegionUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ChunkRenderRegionTest {
    public record MinMaxChunkRegionTestEntry(
        int regionX, int regionY, int regionZ,
        int minChunkX, int minChunkY, int minChunkZ,
        int maxChunkX, int maxChunkY, int maxChunkZ
    ) {
    }

    public record FindRegionForChunkTestEntry(
        int chunkX, int chunkY, int chunkZ,
        int regionX, int regionY, int regionZ
    ) {
    }

    public record TestDistanceCoverage(int regionLevel, int cubeSideSizeOfRegion, int minDistanceToCenterChunk,
                                       int maxDistanceToCenterChunk) {
    }

    private final static List<MinMaxChunkRegionTestEntry> minMaxTestEntries = new ArrayList<>();
    private final static List<FindRegionForChunkTestEntry> fingRegionsForChunkTestEntries = new ArrayList<>();
    private final static List<TestDistanceCoverage> distanceCoverageEntries = new ArrayList<>();

    static {
        minMaxTestEntries.add(new MinMaxChunkRegionTestEntry(0, 0, 0, 0, 0, 0, 0, 0, 0));
        for (int x = -1; x <= 1; x += 1) {
            for (int y = -1; y <= 1; y += 1) {
                for (int z = -1; z <= 1; z += 1) {
                    minMaxTestEntries.add(new MinMaxChunkRegionTestEntry(x, y, z, x, y, z, x, y, z));
                }
            }
        }
        minMaxTestEntries.add(new MinMaxChunkRegionTestEntry(
                2, 0, 0,
                2, -1, -1,
                4, 1, 1
            )
        );
        minMaxTestEntries.add(new MinMaxChunkRegionTestEntry(
                3, 0, 0,
                5, -4, -4,
                13, 4, 4
            )
        );

        fingRegionsForChunkTestEntries.add(new FindRegionForChunkTestEntry(
                3, 1, -2,
                2, 0, -1
            )
        );

        fingRegionsForChunkTestEntries.add(new FindRegionForChunkTestEntry(
                1, 2, -1,
                0, 2, 0
            )
        );

        fingRegionsForChunkTestEntries.add(new FindRegionForChunkTestEntry(
                1, -2, -1,
                0, -2, 0
            )
        );

        fingRegionsForChunkTestEntries.add(new FindRegionForChunkTestEntry(
                7, 2, -2,
                3, 0, 0
            )
        );

        distanceCoverageEntries.add(new TestDistanceCoverage(
                0, 1, 0, 0
            )
        );
        distanceCoverageEntries.add(new TestDistanceCoverage(
                1, 1, 1, 1
            )
        );
        distanceCoverageEntries.add(new TestDistanceCoverage(
                2, 3, 2, 4
            )
        );
        distanceCoverageEntries.add(new TestDistanceCoverage(
                3, 9, 5, 13
            )
        );
    }

    public static Stream<MinMaxChunkRegionTestEntry> provideMinMaxEntries() {
        return minMaxTestEntries.stream();
    }

    public static Stream<FindRegionForChunkTestEntry> provideFinRegionEntries() {
        return fingRegionsForChunkTestEntries.stream();
    }

    public static Stream<TestDistanceCoverage> distanceCoverageEntries() {
        return distanceCoverageEntries.stream();
    }


    @Test
    public void testRegionSizeForLevel() {
        Assertions.assertEquals(27, ChunkRenderRegionUtil.getRegionSizeForLevel(4));
        Assertions.assertEquals(9, ChunkRenderRegionUtil.getRegionSizeForLevel(3));
        Assertions.assertEquals(3, ChunkRenderRegionUtil.getRegionSizeForLevel(2));
        Assertions.assertEquals(1, ChunkRenderRegionUtil.getRegionSizeForLevel(1));
        Assertions.assertEquals(1, ChunkRenderRegionUtil.getRegionSizeForLevel(0));
    }

    @ParameterizedTest
    @MethodSource("provideMinMaxEntries")
    public void testMinmaxChunkForRegion(MinMaxChunkRegionTestEntry entry) {
        int maxLevel = Math.max(Math.abs(entry.regionX), Math.max(Math.abs(entry.regionY), Math.abs(entry.regionZ)));
        int sizeOfCube = ChunkRenderRegionUtil.getRegionSizeForLevel(maxLevel);

        int minChunkX = ChunkRenderRegionUtil.computeMinChunkForRegion(0, entry.regionX, maxLevel);
        int minChunkY = ChunkRenderRegionUtil.computeMinChunkForRegion(0, entry.regionY, maxLevel);
        int minChunkZ = ChunkRenderRegionUtil.computeMinChunkForRegion(0, entry.regionZ, maxLevel);

        int maxChunkX = minChunkX + sizeOfCube - 1;
        int maxChunkY = minChunkY + sizeOfCube - 1;
        int maxChunkZ = minChunkZ + sizeOfCube - 1;

        int[] expectedMinChunks = new int[]{entry.minChunkX, entry.minChunkY, entry.minChunkZ};
        int[] actualMinChunks = new int[]{minChunkX, minChunkY, minChunkZ};
        Assertions.assertArrayEquals(expectedMinChunks, actualMinChunks, "Min chunk does not match for region [" + entry.regionX + ", " + entry.regionY + ", " + entry.regionZ + "]. Expected " + Arrays.toString(expectedMinChunks) + " but was " + Arrays.toString(actualMinChunks));

        int[] expectedMaxChunks = new int[]{entry.maxChunkX, entry.maxChunkY, entry.maxChunkZ};
        int[] actualMaxChunks = new int[]{maxChunkX, maxChunkY, maxChunkZ};
        Assertions.assertArrayEquals(expectedMaxChunks, actualMaxChunks, "Max chunk does not match for region [" + entry.regionX + ", " + entry.regionY + ", " + entry.regionZ + "]. Expected " + Arrays.toString(expectedMaxChunks) + " but was " + Arrays.toString(actualMaxChunks));
    }

    @ParameterizedTest
    @MethodSource("provideFinRegionEntries")
    public void testFindRegionForChunk(FindRegionForChunkTestEntry entry) {
        int[] expectedRegion = new int[]{entry.regionX, entry.regionY, entry.regionZ};
        int[] actualRegion = new int[3];
        ChunkRenderRegionUtil.findRegion(0, 0, 0, entry.chunkX, entry.chunkY, entry.chunkZ, actualRegion);

        Assertions.assertArrayEquals(expectedRegion, actualRegion, "Calculated region does not match for chunk [" + entry.chunkX + ", " + entry.chunkY + ", " + entry.chunkZ + "] . Expected " + Arrays.toString(expectedRegion) + " but was " + Arrays.toString(actualRegion));
    }

    @ParameterizedTest
    @MethodSource("distanceCoverageEntries")
    public void testDistanceCoverages(TestDistanceCoverage entry) {
        Assertions.assertEquals(entry.maxDistanceToCenterChunk, ChunkRenderRegionUtil.getDistanceCoveredInChunksAtLevel(entry.regionLevel), "Region " + entry.regionLevel + " produced wrong results");

        for (int d = entry.maxDistanceToCenterChunk; d >= entry.minDistanceToCenterChunk; d--) {
            Assertions.assertEquals(entry.regionLevel, ChunkRenderRegionUtil.getRegionsCoveredOnDistanceToCenter(d), "Distance to center chunk " + d + " returns wrong region (" + entry.minDistanceToCenterChunk + ", " + entry.maxDistanceToCenterChunk + ")");
        }
    }
}
