package de.verdox.voxelgame;

import de.verdox.voxel.test.light.DummyLightAccessor;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Benchmark)
public class LightBenchmarkState {
    @Param({"8","16", "32", "64"})        // verschiedene Chunk-Größen testen
    public int chunkSize;

    public DummyLightAccessor chunk;
    public DummyLightAccessor above;
    public long seed;

    @Setup(Level.Invocation)
    public void setUp() {
        // Erstelle Accessor für Sky- und Blocklicht
        chunk = new DummyLightAccessor(chunkSize, chunkSize, chunkSize, (byte)15);
        above = new DummyLightAccessor(chunkSize, chunkSize, chunkSize, (byte)15);

        // Fülle “above” mit vollem Sky-Light
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                above.setSkyLight((byte)x, (byte)0, (byte)z, (byte)15);
            }
        }

        // zufällige Opaqueness (z.B. 30% blockende Blöcke)
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int x = 0; x < chunkSize; x++) {
            for (int y = 0; y < chunkSize; y++) {
                for (int z = 0; z < chunkSize; z++) {
                    boolean opa = rnd.nextDouble() < 0.3;
                    chunk.setOpaque((byte)x, (byte)y, (byte)z, opa);
                }
            }
        }
    }
}
