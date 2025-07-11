package de.verdox.voxel.shared.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Eine allgemeine 3D-Map für Integer-Koordinaten (x,y,z) ohne Key-Objekte.
 * Intern werden verschachtelte Int2ObjectOpenHashMaps von FastUtil genutzt,
 * um Autoboxing und Garbage-Collection zu minimieren.
 */
public class ThreeDimensionalMap<V> implements Iterable<ThreeDimensionalMap.Entry<V>> {
    // Ebene 1: X-Koordinate -> (Y-Koordinate -> (Z-Koordinate -> Value))
    private final Int2ObjectMap<Int2ObjectMap<Int2ObjectMap<V>>> map = new Int2ObjectOpenHashMap<>();

    /**
     * Speichert einen Wert an (x,y,z). Liefert den vorherigen Wert zurück.
     */
    public V put(int x, int y, int z, V value) {
        Int2ObjectMap<Int2ObjectMap<V>> yMap = map.computeIfAbsent(x, k -> new Int2ObjectOpenHashMap<>());
        Int2ObjectMap<V> zMap = yMap.computeIfAbsent(y, k -> new Int2ObjectOpenHashMap<>());
        return zMap.put(z, value);
    }

    /**
     * Gibt den Wert an (x,y,z) zurück oder null, wenn nicht vorhanden.
     */
    public V get(int x, int y, int z) {
        Int2ObjectMap<Int2ObjectMap<V>> yMap = map.get(x);
        if (yMap == null) return null;
        Int2ObjectMap<V> zMap = yMap.get(y);
        if (zMap == null) return null;
        return zMap.get(z);
    }

    /**
     * Entfernt den Eintrag an (x,y,z) und gibt den entfernten Wert zurück.
     * Leere Unter-Maps werden entfernt.
     */
    public V remove(int x, int y, int z) {
        Int2ObjectMap<Int2ObjectMap<V>> yMap = map.get(x);
        if (yMap == null) return null;
        Int2ObjectMap<V> zMap = yMap.get(y);
        if (zMap == null) return null;
        V removed = zMap.remove(z);
        if (zMap.isEmpty()) {
            yMap.remove(y);
            if (yMap.isEmpty()) {
                map.remove(x);
            }
        }
        return removed;
    }

    /**
     * Prüft, ob ein Eintrag an (x,y,z) existiert.
     */
    public boolean containsKey(int x, int y, int z) {
        Int2ObjectMap<Int2ObjectMap<V>> yMap = map.get(x);
        if (yMap == null) return false;
        Int2ObjectMap<V> zMap = yMap.get(y);
        return zMap != null && zMap.containsKey(z);
    }

    /**
     * Anzahl aller gespeicherten Werte.
     */
    public int size() {
        int count = 0;
        for (Int2ObjectMap<Int2ObjectMap<V>> yMap : map.values()) {
            for (Int2ObjectMap<V> zMap : yMap.values()) {
                count += zMap.size();
            }
        }
        return count;
    }

    /**
     * Entfernt alle Einträge.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Gibt alle belegten Koordinaten zurück.
     */
    public Set<Coordinates> keySet() {
        Set<Coordinates> keys = new HashSet<>();
        for (var xEntry : map.int2ObjectEntrySet()) {
            int x = xEntry.getIntKey();
            for (var yEntry : xEntry.getValue().int2ObjectEntrySet()) {
                int y = yEntry.getIntKey();
                for (int z : yEntry.getValue().keySet()) {
                    keys.add(new Coordinates(x, y, z));
                }
            }
        }
        return keys;
    }

    @Override
    public Iterator<Entry<V>> iterator() {
        return new Iterator<>() {
            private final Iterator<Coordinates> iter = keySet().iterator();
            @Override public boolean hasNext() { return iter.hasNext(); }
            @Override public Entry<V> next() {
                Coordinates coord = iter.next();
                V val = get(coord.x, coord.y, coord.z);
                return new Entry<>(coord.x, coord.y, coord.z, val);
            }
        };
    }

    /**
     * Datenklasse für Koordinaten.
     */
    public static class Coordinates {
        public final int x, y, z;
        public Coordinates(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public String toString() { return "(" + x + "," + y + "," + z + ")"; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coordinates)) return false;
            Coordinates that = (Coordinates) o;
            return x == that.x && y == that.y && z == that.z;
        }
        @Override public int hashCode() {
            int result = Integer.hashCode(x);
            result = 31 * result + Integer.hashCode(y);
            result = 31 * result + Integer.hashCode(z);
            return result;
        }
    }

    /**
     * Eintrag für Iteration.
     */
    public static class Entry<V> extends AbstractMap.SimpleEntry<Coordinates, V> {
        public Entry(int x, int y, int z, V value) {
            super(new Coordinates(x, y, z), value);
        }
    }
}

