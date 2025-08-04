package de.verdox.voxel.shared.util.buffer;

public class BufferComparison {
    public static void main(String[] args) {
        final int size = 1_000_000;
        final int runs = 100; // Anzahl der Messläufe pro Operation

        // Preset-Daten vorbereiten
        float[] data = new float[size];
        for (int i = 0; i < size; i++) {
            data[i] = i * 0.5f;
        }
        float[] newDataShort = new float[size / 4];
        for (int i = 0; i < newDataShort.length; i++) {
            newDataShort[i] = i * 0.25f;
        }
        float[] newDataLong = new float[size / 2];
        for (int i = 0; i < newDataLong.length; i++) {
            newDataLong[i] = i * 0.75f;
        }

        System.out.println("=== SIMD Buffer ===");
        benchmarkAll(new SIMDDynamicFloatBuffer(size, true), data, newDataShort, newDataLong, runs);

        System.out.println("=== Plain Buffer ===");
        benchmarkAll(new PlainDynamicFloatBuffer(size, true), data, newDataShort, newDataLong, runs);
    }

    private static void benchmarkAll(DynamicFloatBuffer bufPrototype,
                                     float[] data,
                                     float[] newDataShort,
                                     float[] newDataLong,
                                     int runs) {
        int size = data.length;

        // 1) append(float)
        {
            long sum = 0;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                long t0 = System.nanoTime();
                buf.append(1);
                sum += System.nanoTime() - t0;
            }
            print("append", sum, runs);
        }

        // 2) set(int, float)
        {
            long sum = 0;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                for (float v : data) buf.append(v);
                long t0 = System.nanoTime();
                buf.set(5, 1);
                sum += System.nanoTime() - t0;
            }
            print("set single", sum, runs);
        }

        // 3) fill(int, int, float)
        {
            long sum = 0;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                long t0 = System.nanoTime();
                buf.fill(0, size, 1.23f);
                sum += System.nanoTime() - t0;
            }
            print("fill", sum, runs);
        }

        // 4) remove(int)
        {
            long sum = 0;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                for (float v : data) buf.append(v);
                long t0 = System.nanoTime();
                // entferne jeden vierten Eintrag
                buf.remove(15);
                sum += System.nanoTime() - t0;
            }
            print("remove", sum, runs);
        }

        // 5) insert(int, float)
        {
            long sum = 0;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                long t0 = System.nanoTime();
                buf.insert(0, 1);
                sum += System.nanoTime() - t0;
            }
            print("insert single", sum, runs);
        }

        // 6) set(int, float[])
        {
            long sum = 0;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                for (float v : data) buf.append(v);
                long t0 = System.nanoTime();
                buf.set(0, data);
                sum += System.nanoTime() - t0;
            }
            print("set array", sum, runs);
        }

        // 7) insert(int, float[])
        {
            long sum = 0;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                long t0 = System.nanoTime();
                buf.insert(0, data);
                sum += System.nanoTime() - t0;
            }
            print("insert array", sum, runs);
        }

        // 8) update – shrink
        {
            long sum = 0;
            int start = size / 4;
            int end = start + (newDataShort.length * 2);
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                for (float v : data) buf.append(v);
                long t0 = System.nanoTime();
                buf.update(start, end, newDataShort);
                sum += System.nanoTime() - t0;
            }
            print("update shrink", sum, runs);
        }

        // 9) update – expand
        {
            long sum = 0;
            int start = size / 4;
            int end = start + newDataShort.length;
            for (int r = 0; r < runs; r++) {
                DynamicFloatBuffer buf = newInstance(bufPrototype, size);
                for (float v : data) buf.append(v);
                long t0 = System.nanoTime();
                buf.update(start, end, newDataLong);
                sum += System.nanoTime() - t0;
            }
            print("update expand", sum, runs);
        }

        System.out.println();
    }

    private static DynamicFloatBuffer newInstance(DynamicFloatBuffer prototype, int capacity) {
        if (prototype instanceof PlainDynamicFloatBuffer) {
            return new PlainDynamicFloatBuffer(capacity, true);
        } else {
            return new SIMDDynamicFloatBuffer(capacity, true); // SIMD-Variante
        }
    }

    private static void print(String op, long totalNano, int runs) {
        double avgMs = totalNano / runs;
        System.out.printf("%-15s : %8.2f ns%n", op, avgMs);
    }
}
