package de.verdox.voxel.shared.level;

import de.verdox.voxel.shared.level.chunk.ChunkBase;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class WorldHeightMap<CHUNK extends ChunkBase<?>> {
    /** Chunk-Koordinate in Y-Richtung (ganzzahlig) und X/Z-Position (Chunk-Index) */
    record ChunkPos(int cx, int cy, int cz) {}

    /** Speichert pro Chunk das lokale Min/Max (0..chunkHeight-1) und Basis-Y */
    private static class ChunkExtrema {
        int baseY;     // = cy * chunkHeight
        int localMin;  // z.B. depthMap[x][z] im Chunk
        int localMax;  // z.B. heightmap[x][z] im Chunk

        ChunkExtrema(int baseY, int localMin, int localMax) {
            this.baseY    = baseY;
            this.localMin = localMin;
            this.localMax = localMax;
        }
        int absMin() { return baseY + localMin; }
        int absMax() { return baseY + localMax; }
    }

    private final int chunkHeight;
    /** Behalte alle geladenen Chunks und ihre Extremwerte */
    private final Map<ChunkPos, ChunkExtrema> chunkMap = new HashMap<>();

    /** Multiset: global count der absoluten Min-Y */
    private final NavigableMap<Integer,Integer> globalMins = new TreeMap<>();
    /** Multiset: global count der absoluten Max-Y */
    private final NavigableMap<Integer,Integer> globalMaxs = new TreeMap<>();

    public WorldHeightMap(int chunkHeight) {
        this.chunkHeight = chunkHeight;
    }

    /** Hilfsmethode: Multiset++ */
    private void inc(NavigableMap<Integer,Integer> m, int key) {
        m.merge(key, 1, Integer::sum);
    }
    /** Hilfsmethode: Multiset-- und ggf. entfernen */
    private void dec(NavigableMap<Integer,Integer> m, int key) {
        int cnt = m.getOrDefault(key, 0);
        if (cnt <= 1) m.remove(key);
        else          m.put(key, cnt - 1);
    }

    /**
     * Chunk hinzufügen: aus seiner heightmap/depthMap lokale Extrema berechnen
     * und global in die Multisets eintragen.
     */
    public void addChunk(CHUNK chunk) {
        int cx = chunk.getChunkX(), cy = chunk.getChunkY(), cz = chunk.getChunkZ();
        ChunkPos pos = new ChunkPos(cx, cy, cz);

        // 1) Basis-Y berechnen
        int baseY = cy * chunkHeight;

        // 2) Scan über heightmap/depthMap des Chunks
        byte[][] hMap = chunk.getHeightmap();
        byte[][] dMap = chunk.getDepthMap();
        int sizeX = hMap.length, sizeZ = hMap[0].length;

        int localMax = Integer.MIN_VALUE;
        int localMin = Integer.MAX_VALUE;
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                int h = Byte.toUnsignedInt(hMap[x][z]);
                int d = Byte.toUnsignedInt(dMap[x][z]);
                localMax = Math.max(localMax, h);
                localMin = Math.min(localMin, d);
            }
        }
        ChunkExtrema ext = new ChunkExtrema(baseY, localMin, localMax);

        // 3) global hinzufügen
        inc(globalMins, ext.absMin());
        inc(globalMaxs, ext.absMax());
        chunkMap.put(pos, ext);
    }

    /**
     * Chunk entfernen: seine alten Extremwerte aus den Multisets dekrementieren.
     */
    public void removeChunk(CHUNK chunk) {
        ChunkPos pos = new ChunkPos(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
        ChunkExtrema old = chunkMap.remove(pos);
        if (old == null) return;
        dec(globalMins, old.absMin());
        dec(globalMaxs, old.absMax());
    }

    /**
     * Einzelblock-Update in einem Chunk:
     * Nachdem im Chunk die heightmap/depthMap angepasst wurde,
     * rufe das hier auf, um die globalen Multisets zu korrigieren.
     */
    public void blockUpdate(CHUNK chunk) {
        ChunkPos pos = new ChunkPos(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
        ChunkExtrema old = chunkMap.get(pos);
        if (old == null) {
            // noch nicht bekannt – einfach als neu anlegen
            addChunk(chunk);
            return;
        }

        // entferne alte Beiträge
        dec(globalMins, old.absMin());
        dec(globalMaxs, old.absMax());

        // berechne die neuen lokalen Extrema
        int localMax = Integer.MIN_VALUE;
        int localMin = Integer.MAX_VALUE;
        byte[][] hMap = chunk.getHeightmap();
        byte[][] dMap = chunk.getDepthMap();
        for (int x = 0; x < hMap.length; x++) {
            for (int z = 0; z < hMap[0].length; z++) {
                localMax = Math.max(localMax, Byte.toUnsignedInt(hMap[x][z]));
                localMin = Math.min(localMin, Byte.toUnsignedInt(dMap[x][z]));
            }
        }
        // aktualisiere ChunkExtrema
        old.localMax = localMax;
        old.localMin = localMin;

        // füge neue Beiträge hinzu
        inc(globalMins, old.absMin());
        inc(globalMaxs, old.absMax());
    }

    /** Zugriff auf das globale Minimum (kleinster key in globalMins) */
    public int getGlobalMin() {
        return globalMins.isEmpty() ? 0 : globalMins.firstKey();
    }
    /** Zugriff auf das globale Maximum (größter key in globalMaxs) */
    public int getGlobalMax() {
        return globalMaxs.isEmpty() ? 0 : globalMaxs.lastKey();
    }
}

