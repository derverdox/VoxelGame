package de.verdox.voxel.shared.util;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hierarchischer Benchmark-Profiler mit gleitendem Durchschnitt der letzten X Messwerte.
 */
public class Benchmark {

    private static final boolean ENABLE_BENCHMARK = true;

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("00.00%");

    private final Section root;
    private final Deque<Section> stack;
    private final int windowSize;

    /**
     * @param windowSize Anzahl der letzten Messwerte für den gleitenden Durchschnitt
     */
    public Benchmark(int windowSize) {
        this.windowSize = windowSize;
        this.root = new Section("ROOT", null, windowSize);
        this.stack = new ArrayDeque<>();
        this.stack.push(root);
    }

    /**
     * Startet das Root-Benchmark neu und leert alle vorherigen Daten.
     */
    public void start() {
        if (!ENABLE_BENCHMARK) {
            return;
        }
        root.clear();
        root.startTime = System.nanoTime();
        stack.clear();
        stack.push(root);
    }

    /**
     * Beginnt eine neue Sektion mit dem gegebenen Namen.
     */
    public void startSection(String name) {
        if (!ENABLE_BENCHMARK) {
            return;
        }
        Section parent = stack.peek();
        Section sec = parent.getOrCreateChild(name, windowSize);
        sec.startTime = System.nanoTime();
        stack.push(sec);
    }

    /**
     * Beendet die aktuellste Sektion und zeichnet die Dauer auf.
     */
    public void endSection() {
        if (!ENABLE_BENCHMARK) {
            return;
        }
        Section sec = stack.pop();
        sec.endTime = System.nanoTime();
        sec.duration = sec.endTime - sec.startTime;
        sec.recordDuration(sec.duration);
    }

    /**
     * Beendet alle offenen Sektionen (inkl. Root) und zeichnet auch deren Dauer auf.
     */
    public void end() {
        if (!ENABLE_BENCHMARK) {
            return;
        }
        while (stack.size() > 1) {
            endSection();
        }
        root.endTime = System.nanoTime();
        root.duration = root.endTime - root.startTime;
        root.recordDuration(root.duration);
    }

    /**
     * Liefert den Bericht als Liste von Zeilen, mit gleitenden Durchschnitten.
     */
    public List<String> printToLines(String title) {
        if (!ENABLE_BENCHMARK) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        long totalAvg = root.getAverage();
        lines.add(title + ": avg " + formatTime(totalAvg) + " (über letzte " + windowSize + ")");
        for (Section sec : root.children.values()) {
            printSection(sec, 1, totalAvg, lines);
        }
        return lines;
    }

    private void printSection(Section sec, int depth, long totalAvg, List<String> lines) {
        if (!ENABLE_BENCHMARK) {
            return;
        }
        String indent = "    ".repeat(depth);
        long avg = sec.getAverage();
        double pct = totalAvg > 0 ? avg * 1.0 / totalAvg : 0.0;
        lines.add(indent + sec.name + ": avg " + formatTime(avg) + " (" + PERCENT_FORMAT.format(pct) + ")");
        for (Section child : sec.children.values()) {
            printSection(child, depth + 1, totalAvg, lines);
        }
    }

    private static String formatTime(long nanos) {
        long ms = TimeUnit.NANOSECONDS.toMillis(nanos);
        return ms >= 1 ? ms + " ms" : nanos + " ns";
    }

    /**
     * Repräsentiert eine Sektion mit einer History für gleitenden Durchschnitt.
     */
    private static class Section {
        final String name;
        final Section parent;
        final Map<String, Section> children = new LinkedHashMap<>();
        long startTime;
        long endTime;
        long duration;

        private final Deque<Long> history;
        private long sum;
        private final int windowSize;

        Section(String name, Section parent, int windowSize) {
            this.name = name;
            this.parent = parent;
            this.windowSize = windowSize;
            this.history = new ArrayDeque<>(windowSize);
            this.sum = 0L;
        }

        Section getOrCreateChild(String name, int windowSize) {
            return children.computeIfAbsent(name, k -> new Section(name, this, windowSize));
        }

        void clear() {
            for (Section child : children.values()) {
                child.clear();
            }
            history.clear();
            sum = 0L;
            startTime = endTime = duration = 0L;
        }

        void recordDuration(long d) {
            history.addLast(d);
            sum += d;
            if (history.size() > windowSize) {
                sum -= history.removeFirst();
            }
        }

        long getAverage() {
            return history.isEmpty() ? 0L : sum / history.size();
        }
    }
}
