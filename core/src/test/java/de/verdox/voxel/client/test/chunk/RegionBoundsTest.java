package de.verdox.voxel.client.test.chunk;

import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.RegionBounds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class RegionBoundsTest {
    private static final RegionBounds bounds = new RegionBounds(4, 4, 4);

    public static Stream<RegionOffsetEntry> provideMinMaxEntries() {
        return Stream.of(
                new RegionOffsetEntry(
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0
                ),
                new RegionOffsetEntry(
                        -1, -1, -1,
                        -1, -1, -1,
                        3, 3, 3
                )
        );
    }

    public record RegionOffsetEntry(
            int chunkX, int chunkY, int chunkZ,
            int regionX, int regionY, int regionZ,
            int offsetX, int offsetY, int offsetZ
    ) {
    }

    @ParameterizedTest
    @MethodSource("provideMinMaxEntries")
    public void testRegionCoordinates(RegionOffsetEntry entry) {
        Assertions.assertEquals(entry.regionX, bounds.getRegionX(entry.chunkX));
        Assertions.assertEquals(entry.regionY, bounds.getRegionY(entry.chunkY));
        Assertions.assertEquals(entry.regionZ, bounds.getRegionZ(entry.chunkZ));
    }

    @ParameterizedTest
    @MethodSource("provideMinMaxEntries")
    public void testRegionKeyDirect(RegionOffsetEntry entry) {
        long actual = bounds.getRegionKeyFromChunk(entry.chunkX, entry.chunkY, entry.chunkZ);
        long expected = Chunk.computeChunkKey(entry.regionX, entry.regionY, entry.regionZ);
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("provideMinMaxEntries")
    public void testRegionKeyInDirect(RegionOffsetEntry entry) {
        long actual = bounds.getRegionKeyFromChunk(Chunk.computeChunkKey(entry.chunkX, entry.chunkY, entry.chunkZ));
        long expected = Chunk.computeChunkKey(entry.regionX, entry.regionY, entry.regionZ);
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("provideMinMaxEntries")
    public void testRegionOffsets(RegionOffsetEntry entry) {
        Assertions.assertEquals(entry.offsetX, bounds.getOffsetX(entry.chunkX));
        Assertions.assertEquals(entry.offsetY, bounds.getOffsetY(entry.chunkY));
        Assertions.assertEquals(entry.offsetZ, bounds.getOffsetZ(entry.chunkZ));
    }
}
