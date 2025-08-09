package de.verdox.voxel.client.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class ThroughputBenchmark {
    private final String name;

    private final LongAdder totalProcessingTimeNs = new LongAdder(); // Summe der Item-Dauern
    private final LongAdder processesDone         = new LongAdder();
    private final AtomicLong lastTookNs           = new AtomicLong();

    private final long startedAtNs = System.nanoTime();

    // Optional: bekannte Anzahl Worker/Threads für Effizienz
    private final AtomicInteger workers = new AtomicInteger(0);

    public ThroughputBenchmark(String name) {
        this.name = name;
    }

    /** Optional: Anzahl Worker/Threads für Effizienz-Berechnung setzen. */
    public ThroughputBenchmark withWorkers(int workerCount) {
        this.workers.set(Math.max(0, workerCount));
        return this;
    }

    /** durationNanos = Dauer eines verarbeiteten Items in Nanosekunden (gemessen in eurer Pipeline) */
    public void add(long durationNanos) {
        totalProcessingTimeNs.add(durationNanos);
        lastTookNs.set(durationNanos);
        processesDone.increment();
    }

    public long getProcessed()                 { return processesDone.sum(); }
    public long getTotalProcessingTimeNs()     { return totalProcessingTimeNs.sum(); }
    public long getLastTookMs()                { return TimeUnit.NANOSECONDS.toMillis(lastTookNs.get()); }
    public int  getWorkers()                   { return workers.get(); }

    public String format() {
        long processed = getProcessed();

        double wallSec = (System.nanoTime() - startedAtNs) / 1_000_000_000.0;
        double cpuSec  = getTotalProcessingTimeNs()       / 1_000_000_000.0;

        double tputWall = wallSec > 0 ? processed / wallSec : 0.0;  // Items / s (Echtzeit)
        double tputCpu  = cpuSec  > 0 ? processed / cpuSec  : 0.0;  // Items / s (aufsummierte Arbeitszeit)

        // Effektive Parallelität (≈ „wie viele Arbeitssekunden verbrennen wir pro Wandsekunde?“)
        double parallelism = (wallSec > 0) ? (cpuSec / wallSec) : 0.0;

        // Effizienz relativ zur Thread-Anzahl (falls bekannt)
        int w = getWorkers();
        double efficiency = (w > 0) ? (parallelism / w) : Double.NaN; // 1.0 == perfekt, <1 schlechter, >1 oversubscribed/messfehler

        String effPart = (w > 0)
                ? String.format(Locale.US, "Parallelism: %.2fx, Efficiency: %.0f%% of %d",
                parallelism, Math.max(0.0, Math.min(1.0, efficiency)) * 100.0, w)
                : String.format(Locale.US, "Parallelism: %.2fx", parallelism);

        return String.format(Locale.US,
                "%s: %,d, Throughput(wall): %.1f %s/s, Throughput(cpu): %.1f %s/s, %s, Last: %dms",
                name, processed, tputWall, name, tputCpu, name, effPart, getLastTookMs());
    }
}