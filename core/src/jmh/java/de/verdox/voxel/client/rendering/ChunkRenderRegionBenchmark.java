package de.verdox.voxel.client.rendering;

import de.verdox.voxel.client.level.mesh.region.CameraCenteredRegionStrategy;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ChunkRenderRegionBenchmark {

    private CameraCenteredRegionStrategy master;
    private int chunkSizeX, chunkSizeY, chunkSizeZ;
    private int centerX, centerY, centerZ;
    private int viewDistX, viewDistY, viewDistZ;

    @Setup(Level.Trial)
    public void setup() {
        master = new CameraCenteredRegionStrategy(null, 32, 32, 32);
        chunkSizeX = chunkSizeY = chunkSizeZ = 16;
        centerX = centerY = centerZ = 0;
        viewDistX = viewDistY = viewDistZ = 32;
    }

    @Benchmark
    public void measureRebuildRegions() {
        master.rebuildRegions(
            chunkSizeX, chunkSizeY, chunkSizeZ,
            centerX, centerY, centerZ
        );
    }
}
