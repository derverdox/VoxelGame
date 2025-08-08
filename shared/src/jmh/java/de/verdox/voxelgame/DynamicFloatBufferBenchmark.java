package de.verdox.voxelgame;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmarks für DynamicFloatBuffer und TerrainMeshBuffer.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class DynamicFloatBufferBenchmark {

    /**
     * State für DynamicFloatBuffer-Benchmarks.
     */
    @State(Scope.Thread)
    public static class DynamicState {
        @Param({"1000", "10000", "100000", "1000000"})
        public int initialCapacity;

        public float[] sampleData;
        public SIMDDynamicFloatBuffer dfb;

        @org.openjdk.jmh.annotations.Setup(org.openjdk.jmh.annotations.Level.Invocation)
        public void setup() {
            dfb = new SIMDDynamicFloatBuffer(initialCapacity);
            sampleData = new float[initialCapacity];
            for (int i = 0; i < initialCapacity; i++) {
                sampleData[i] = i * 0.5f;
            }
        }
    }

    @Benchmark
    public void dyn_append(DynamicState st) {
        for (float v : st.sampleData) st.dfb.append(v);
    }

    @Benchmark
    public void dyn_set(DynamicState st) {
        for (int i = 0; i < st.sampleData.length; i++) st.dfb.set(i, st.sampleData[i]);
    }

    @Benchmark
    public void dyn_remove(DynamicState st) {
        for (float v : st.sampleData) st.dfb.append(v);
        for (int i = 0; i < st.sampleData.length; i += 2) st.dfb.remove(i);
    }

    @Benchmark
    public void dyn_insert(DynamicState st) {
        for (float v : st.sampleData) st.dfb.insert(0, v);
    }

    // ------------------------------------------------------------------------
    // 2) Benchmark für SIMD-Set: Buffer ist voll mit sampleData
    // ------------------------------------------------------------------------
    @State(Scope.Thread)
    public static class SetState {
        @Param({ "1000", "10000", "100000", "1000000" })
        public int initialCapacity;

        public float[] sampleData;
        public SIMDDynamicFloatBuffer dfb;

        /** Bei jeder Invocation neu: Buffer vorbefüllt, damit setSIMD überschreiben kann */
        @Setup(Level.Invocation)
        public void setup() {
            dfb = new SIMDDynamicFloatBuffer(initialCapacity);
            sampleData = new float[initialCapacity];
            for (int i = 0; i < sampleData.length; i++) {
                sampleData[i] = i * 0.5f;
                dfb.append(sampleData[i]);    // Buffer mit originalen Werten füllen
            }
        }
    }

    @Benchmark
    public void simd_set(SetState st) {
        st.dfb.set(0, st.sampleData);
    }

    /**
     * State für insertSIMD:
     * - initialCapacity variiert
     * - sampleData gefüllt mit i*0.5f
     * - bei jeder Invocation ein **leerer** Buffer
     */
    @State(Scope.Thread)
    public static class InsertState {
        @Param({ "1000", "10000", "100000", "1000000" })
        public int initialCapacity;

        public float[] sampleData;
        public SIMDDynamicFloatBuffer dfb;

        @Setup(Level.Invocation)
        public void setup() {
            dfb = new SIMDDynamicFloatBuffer(initialCapacity);
            sampleData = new float[initialCapacity];
            for (int i = 0; i < sampleData.length; i++) {
                sampleData[i] = i * 0.5f;
            }
        }
    }

    /**
     * Misst die Zeit, um sampleData per SIMD am Anfang (pos=0) einzufügen.
     */
    @Benchmark
    public void simd_insert(InsertState st) {
        st.dfb.insert(0, st.sampleData);
    }
}
