package de.verdox.voxel.shared.util.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CoalescingScheduler<K> {
    private final ExecutorService exec;

    // Ein Eintrag pro Key; wird entfernt, wenn idle.
    static final class Entry {
        final AtomicLong requested = new AtomicLong();
        final AtomicLong processed = new AtomicLong();
        final AtomicBoolean running = new AtomicBoolean(false);
        volatile long lastTouchNs;
    }

    private final ConcurrentHashMap<K, Entry> entries = new ConcurrentHashMap<>();

    public CoalescingScheduler(ExecutorService exec) { this.exec = exec; }

    /** Koaleszierter Request für key; startet genau einen Runner, falls keiner läuft. */
    public void request(K key, Runnable job) {
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        e.requested.incrementAndGet();
        e.lastTouchNs = System.nanoTime();
        if (e.running.compareAndSet(false, true)) {
            exec.execute(() -> runLoop(key, e, job));
        }
    }

    private void runLoop(K key, Entry e, Runnable job) {
        try {
            while (true) {
                long req  = e.requested.get();
                long done = e.processed.get();
                if (done == req) break;     // nichts mehr offen
                job.run();                  // deckt alle bis hier koaleszierten Wünsche ab
                e.processed.set(req);
            }
        } finally {
            e.running.set(false);
            // Falls währenddessen Neues kam -> sofort weiterlaufen
            if (e.processed.get() != e.requested.get()
                    && e.running.compareAndSet(false, true)) {
                exec.execute(() -> runLoop(key, e, job));
                return;
            }
            // Idle: Entry aus Map entfernen (Auto-Cleanup)
            entries.remove(key, e); // entfernt nur, wenn noch derselbe Entry drin ist
        }
    }

    /** Entfernt ALLE idle-Keys (nichts queued, nichts running). */
    void clearIdle() {
        entries.forEach((k, e) -> {
            if (!e.running.get() && e.processed.get() == e.requested.get()) {
                entries.remove(k, e);
            }
        });
    }

    /** Entfernt gezielt einen Key, falls nicht gerade laufend. Return: true=entfernt. */
    boolean purge(K key) {
        Entry e = entries.get(key);
        if (e == null) return false;
        if (e.running.get()) return false;               // gerade aktiv -> nicht löschen
        if (e.processed.get() != e.requested.get()) return false; // noch queued
        return entries.remove(key, e);
    }

    /** Optional: zeitbasierte Bereinigung (TTL in ns). */
    void pruneOlderThan(long ttlNs) {
        long now = System.nanoTime();
        entries.forEach((k, e) -> {
            if (!e.running.get()
                    && e.processed.get() == e.requested.get()
                    && now - e.lastTouchNs > ttlNs) {
                entries.remove(k, e);
            }
        });
    }

    public String metrics(String label) {
        long active = entries.values().stream().filter(en -> en.running.get()).count();
        return label + "[keys=" + entries.size() + ", active=" + active + "]";
    }
}

