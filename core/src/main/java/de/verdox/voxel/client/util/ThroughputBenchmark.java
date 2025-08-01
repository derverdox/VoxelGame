package de.verdox.voxel.client.util;

import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class ThroughputBenchmark {
    private final AtomicLong totalProcessingTimeNs = new AtomicLong();
    private final AtomicLong processesDone = new AtomicLong();
    private final AtomicLong lastTook = new AtomicLong();
    private final String name;

    public ThroughputBenchmark(String name) {
        this.name = name;
    }

    public void add(long durationNanos) {
        totalProcessingTimeNs.addAndGet(durationNanos);
        lastTook.set(TimeUnit.NANOSECONDS.toMillis(durationNanos));
        processesDone.addAndGet(1);
    }

    public String format() {
        long processed = processesDone.get();
        double totalSec = getTotalProcessingTimeNs().get() / 1_000_000_000.0;
        double throughput = (totalSec > 0) ? processed / totalSec : 0;

        return String.format(
                name + ": %d, Throughput: %.1f " + name + "/s, Last: %dms",
                processed, throughput, getLastTook().get()
        );
    }
}
