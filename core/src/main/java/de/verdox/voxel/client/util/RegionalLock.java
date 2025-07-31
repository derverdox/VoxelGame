package de.verdox.voxel.client.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages locking of 3D chunk-regions and a global master lock to prevent
 * concurrent access issues at both granular and global levels.
 *
 * Beispiel-Nutzung für Region:
 *   RegionalLock lock = new RegionalLock();
 *   lock.withLock(baseX, baseY, baseZ, radius, () -> {
 *       // kritischer Code auf Regionsebene
 *   });
 *
 * Radius-Definition:
 * - radius = 0: nur der eigene Chunk bei (offsetX, offsetY, offsetZ)
 * - radius = 1: direkter Nachbar-Umkreis (3×3×3 = 27 Chunks)
 * - allgemein: (2·radius + 1)^3 Chunks
 *
 * Beispiel-Nutzung für Master:
 *   lock.withMasterLock(() -> {
 *       // kritischer Code auf globaler Ebene
 *   });
 */
public class RegionalLock {
    // Einzelne Locks pro Chunk-Key
    private final Long2ObjectMap<ReentrantLock> regionLocks =
            Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    // Master-Lock für globale Exklusion (Reader-Writer-Prinzip)
    private final ReentrantReadWriteLock masterLock = new ReentrantReadWriteLock();

    /**
     * Sperrt eine 3D-Region aus aufeinanderfolgenden Chunks (Leser-Modus),
     * führt die Aktion aus und gibt die Locks wieder frei.
     * Blockiert, wenn gerade ein globaler Master-Lock im Schreib-Modus aktiv ist.
     *
     * Radius-Logik: Schleifen von -radius bis +radius (inklusive).
     *
     * @param offsetX Zentrum X-Koordinate in Chunks
     * @param offsetY Zentrum Y-Koordinate in Chunks
     * @param offsetZ Zentrum Z-Koordinate in Chunks
     * @param radius  Umkreis in Chunks (0 = nur eigener Chunk)
     * @param action  Kritischer Abschnitt, der unter Schutz laufen soll
     */
    public void withLock(int offsetX, int offsetY, int offsetZ, int radius, Runnable action) {
        // Globalen Read-Lock erwerben (blockierend gegen Master-Write)
        masterLock.readLock().lock();
        try {
            List<Long> keys = new ArrayList<>();
            // Schleifen über Cube von -radius..+radius
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        long key = computeChunkKey(offsetX + dx, offsetY + dy, offsetZ + dz);
                        keys.add(key);
                    }
                }
            }

            // Sortieren für konsistente Lock-Reihenfolge (Deadlock-Vermeidung)
            Collections.sort(keys);

            // Locks holen/erzeugen
            List<ReentrantLock> locksToAcquire = new ArrayList<>(keys.size());
            for (long key : keys) {
                ReentrantLock lock;
                synchronized (regionLocks) {
                    lock = regionLocks.get(key);
                    if (lock == null) {
                        lock = new ReentrantLock();
                        regionLocks.put(key, lock);
                    }
                }
                locksToAcquire.add(lock);
            }

            // Locks erwerben
            for (ReentrantLock lock : locksToAcquire) {
                lock.lock();
            }

            try {
                // Aktion ausführen
                action.run();
            } finally {
                // Locks in umgekehrter Reihenfolge freigeben
                for (int i = locksToAcquire.size() - 1; i >= 0; i--) {
                    locksToAcquire.get(i).unlock();
                }
            }
        } finally {
            // Globalen Read-Lock freigeben
            masterLock.readLock().unlock();
        }
    }

    /**
     * Sperrt die gesamte Datenstruktur exklusiv (Master-Write),
     * führt die Aktion aus und gibt den Lock wieder frei.
     * Blockiert alle Region-Locks und andere Master-Locks.
     *
     * @param action Kritischer Abschnitt, der globalen Zugriff erfordert
     */
    public void withMasterLock(Runnable action) {
        masterLock.writeLock().lock();
        try {
            action.run();
        } finally {
            masterLock.writeLock().unlock();
        }
    }

    /**
     * Berechnet einen eindeutigen Key aus drei Chunk-Koordinaten (cx, cy, cz).
     * Packt cx in Bits 42–63, cy in Bits 21–41, cz in Bits 0–20.
     *
     * @param cx Chunk-X
     * @param cy Chunk-Y
     * @param cz Chunk-Z
     * @return Eindeutiger 64-Bit-Key
     */
    public static long computeChunkKey(int cx, int cy, int cz) {
        return (((long) cx & 0x3FFFFF) << 42)
                | (((long) cy & 0x3FFFFF) << 21)
                | (((long) cz & 0x1FFFFF));
    }
}