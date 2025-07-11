package de.verdox.voxel.shared.util;

import java.util.concurrent.TimeUnit;

public class Benchmark {
    public static void nanos(Runnable runnable, int tries, boolean printToConsole) {
        long start = System.nanoTime();

        for (int i = 0; i < tries; i++) {
            runnable.run();
        }
        long end = System.nanoTime() - start;
        end /= tries;

        if (!printToConsole) {
            return;
        }

        if (TimeUnit.NANOSECONDS.toMillis(end) > 0) {
            System.out.println("Took " + TimeUnit.NANOSECONDS.toMillis(end) + " ms");
        } else {
            System.out.println("Took " + end + " ns");
        }
    }
}
