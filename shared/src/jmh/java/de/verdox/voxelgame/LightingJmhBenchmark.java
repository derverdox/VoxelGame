package de.verdox.voxelgame;

import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.lighting.ChunkLightEngine;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
public class LightingJmhBenchmark {
    /**
     * Messen der Skylight-Berechnung.
     */
    @Benchmark
    public void benchComputeSkylight(LightBenchmarkState state) {
        computeSkylight(state.chunk, state.above);
    }

    /**
     * Messen der RGB-Blocklight-Berechnung.
     */
    @Benchmark
    public void benchComputeColoredLight(LightBenchmarkState state) {

        computeColoredLight(state.chunk, state.above);
    }

    // Deine Methoden hier referenzieren:
    private void computeSkylight(LightAccessor accessor, LightAccessor above) {
        // ... implementiert wie zuvor ...
        computeSkylight(accessor, above);
    }
    private void computeColoredLight(LightAccessor accessor, LightAccessor above) {
        // ... implementiert wie zuvor ...
    }
}
