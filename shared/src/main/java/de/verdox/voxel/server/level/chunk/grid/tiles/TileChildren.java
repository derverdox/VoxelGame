package de.verdox.voxel.server.level.chunk.grid.tiles;

import de.verdox.voxel.server.level.chunk.grid.GridConfig;
import de.verdox.voxel.server.level.chunk.grid.SparseOTChunkGrid;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.Arrays;

/**
 * Sparse kachelige Ablage für beliebige sizeX/Y/Z.
 * - Welt in Tiles à t^3 geteilt. Pro belegtem Tile:
 * * 64-bit Bitmaske (max t=4 -> 64 Zellen),
 * * Object[] in Bit-Reihenfolge für Werte.
 * - Tile-Arrays werden dicht gehalten; eine Int2Int-Map mappt tileId -> Position im Array.
 */
public final class TileChildren<T> {
    private final GridConfig gridConfig;

    // Sparse Arrays
    private long[] tileBits;       // pro belegtem Tile: 64-bit Belegungsmaske
    private Object[][] tileVals;   // pro Tile: Werte in Bit-Reihenfolge
    private int[] tileIdByPos;     // tileId an array-Position (zur Iteration)
    private Int2IntMap tilePosById;// tileId -> position im Array (oder -1)

    private int size; // Anzahl belegter Zellen gesamt

    public TileChildren(GridConfig gridConfig) {
        this.gridConfig = gridConfig;
        this.tileBits = new long[0];
        this.tileVals = new Object[0][];
        this.tileIdByPos = new int[0];
        this.tilePosById = new Int2IntOpenHashMap();
        this.tilePosById.defaultReturnValue(-1);
        this.size = 0;
    }

    public byte getSizeX() {
        return gridConfig.sizeX();
    }

    public byte getSizeY() {
        return gridConfig.sizeY();
    }

    public byte getSizeZ() {
        return gridConfig.sizeZ();
    }

    public int getTileDim() {
        return gridConfig.tileDim();
    }

    public int getTilesX() {
        return getTiles(getSizeX(), getTileDim());
    }

    public int getTilesY() {
        return getTiles(getSizeY(), getTileDim());
    }

    public int getTilesZ() {
        return getTiles(getSizeZ(), getTileDim());
    }

    private int getTiles(byte size, int tileDim) {
        return (size + tileDim - 1) / tileDim;
    }

    public int size() {
        return size;
    }

    public boolean contains(int x, int y, int z) {
        int tId = tileId(x, y, z);
        int pos = tilePosById.get(tId);
        if (pos < 0) return false;
        long bits = tileBits[pos];
        return (bits & (1L << localId(x, y, z))) != 0L;
    }

    @SuppressWarnings("unchecked")
    public T get(int x, int y, int z) {
        int tId = tileId(x, y, z);
        int pos = tilePosById.get(tId);
        if (pos < 0) return null;
        long bits = tileBits[pos];
        int l = localId(x, y, z);
        long b = 1L << l;
        if ((bits & b) == 0) return null;
        int idx = rank(bits, l);
        return (T) tileVals[pos][idx];
    }

    /**
     * Setzt Wert an (x,y,z). null entfernt die Zelle.
     *
     * @return vorheriger Wert (oder null)
     */
    public T set(int x, int y, int z, T val) {
        if (val == null) return remove(x, y, z);
        int tId = tileId(x, y, z);
        int pos = ensureTile(tId);
        long bits = tileBits[pos];
        int l = localId(x, y, z);
        long b = 1L << l;
        if ((bits & b) == 0L) {
            int idx = rank(bits, l);
            bits |= b;
            tileBits[pos] = bits;
            tileVals[pos] = insertAt(tileVals[pos], idx, val);
            size++;
            return null;
        } else {
            int idx = rank(bits, l);
            @SuppressWarnings("unchecked") T old = (T) tileVals[pos][idx];
            tileVals[pos][idx] = val;
            return old;
        }
    }

    @SuppressWarnings("unchecked")
    public T remove(int x, int y, int z) {
        int tId = tileId(x, y, z);
        int pos = tilePosById.get(tId);
        if (pos < 0) return null;
        long bits = tileBits[pos];
        int l = localId(x, y, z);
        long b = 1L << l;
        if ((bits & b) == 0L) return null;
        int idx = rank(bits, l);
        T old = (T) tileVals[pos][idx];
        bits &= ~b;
        tileBits[pos] = bits;
        tileVals[pos] = removeAt(tileVals[pos], idx);
        size--;
        if (bits == 0L) removeTileAt(pos, tId);
        return old;
    }

    /**
     * Iterator über alle belegten Zellen (sparse).
     */
    public Cursor cursor() {
        return new Cursor();
    }

    /**
     * true, wenn alle belegten Zellen transparent sind (Kind == null oder Kind.isFullyTransparent()).
     */
    public boolean allTransparent() {
        for (Cursor c = cursor(); c.hasNext(); ) {
            TileCell<T> cell = c.next();
            if (cell.value != null) {
                if (cell.value instanceof SparseOTChunkGrid.Node n) {
                    if (!n.isFullyTransparent()) return false;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    // ---- intern / Hilfsfunktionen ----

    private int tileId(int x, int y, int z) {
        if ((x | y | z) < 0 || x >= getSizeX() || y >= getSizeY() || z >= getSizeZ())
            throw new IndexOutOfBoundsException();
        int tileDim = getTileDim();
        int tilesX = getTilesX();
        int tilesY = getTilesY();
        int tx = x / getTileDim(), ty = y / tileDim, tz = z / tileDim;
        return (tz * (tilesX * tilesY)) + (ty * tilesX) + tx;
    }

    private int localId(int x, int y, int z) {
        int tileDim = getTileDim();
        int lx = x % tileDim, ly = y % tileDim, lz = z % tileDim;
        // (max 4) -> 2 Bits pro Achse => 6 Bits total => 0..63
        return (lz << 4) | (ly << 2) | lx;
    }

    private static int rank(long mask, int bit) {
        if (bit == 0) return 0;
        long lower = (-1L >>> (64 - bit));
        return Long.bitCount(mask & lower);
    }

    private int ensureTile(int tileId) {
        int pos = tilePosById.get(tileId);
        if (pos >= 0) return pos;

        int oldLen = tileBits.length;
        int newLen = oldLen + 1;
        long[] nb = Arrays.copyOf(tileBits, newLen);
        Object[][] nv = Arrays.copyOf(tileVals, newLen);
        int[] nt = Arrays.copyOf(tileIdByPos, newLen);

        nb[oldLen] = 0L;
        nv[oldLen] = new Object[0];
        nt[oldLen] = tileId;

        tileBits = nb;
        tileVals = nv;
        tileIdByPos = nt;
        tilePosById.put(tileId, oldLen);
        return oldLen;
    }

    private void removeTileAt(int pos, int tileId) {
        int last = tileBits.length - 1;
        if (pos != last) {
            // letzte nach vorn ziehen
            tileBits[pos] = tileBits[last];
            tileVals[pos] = tileVals[last];
            int movedId = tileIdByPos[last];
            tileIdByPos[pos] = movedId;
            tilePosById.put(movedId, pos);
        }
        // kürzen
        tileBits = Arrays.copyOf(tileBits, last);
        tileVals = Arrays.copyOf(tileVals, last);
        tileIdByPos = Arrays.copyOf(tileIdByPos, last);
        tilePosById.remove(tileId);
    }

    private static Object[] insertAt(Object[] arr, int idx, Object v) {
        Object[] out = new Object[arr.length + 1];
        if (idx > 0) System.arraycopy(arr, 0, out, 0, idx);
        out[idx] = v;
        if (idx < arr.length) System.arraycopy(arr, idx, out, idx + 1, arr.length - idx);
        return out;
    }

    private static Object[] removeAt(Object[] arr, int idx) {
        Object[] out = new Object[arr.length - 1];
        if (idx > 0) System.arraycopy(arr, 0, out, 0, idx);
        if (idx < arr.length - 1) System.arraycopy(arr, idx + 1, out, idx, arr.length - idx - 1);
        return out;
    }

    public final class Cursor {
        private int pos = 0;        // tile array pos
        private long bits = (tileBits.length > 0) ? tileBits[0] : 0L;
        private int nextBit = (bits != 0) ? Long.numberOfTrailingZeros(bits) : 64;

        public boolean hasNext() {
            while (true) {
                if (pos >= tileBits.length) return false;
                if (nextBit < 64) return true;
                // jump to next tile
                pos++;
                if (pos >= tileBits.length) return false;
                bits = tileBits[pos];
                nextBit = (bits != 0) ? Long.numberOfTrailingZeros(bits) : 64;
            }
        }

        @SuppressWarnings("unchecked")
        public TileCell<T> next() {
            byte sizeX = getSizeX();
            byte sizeY = getSizeY();
            int tilesX = getTilesX();
            int tilesY = getTilesY();
            int tileDim = getTileDim();

            int l = nextBit;
            long mask = 1L << l;
            int idx = rank(bits, l);
            T val = (T) tileVals[pos][idx];

            // global linear index rekonstruieren
            int tileId = tileIdByPos[pos];
            int tilesXY = tilesX * tilesY;
            int tz = tileId / tilesXY;
            int rem = tileId - tz * tilesXY;
            int ty = rem / tilesX;
            int tx = rem - ty * tilesX;

            int lx = l & 3;
            int ly = (l >>> 2) & 3;
            int lz = (l >>> 4) & 3;

            int x = tx * tileDim + lx;
            int y = ty * tileDim + ly;
            int z = tz * tileDim + lz;
            int linear = (z * (sizeX * sizeY)) + (y * sizeX) + x;

            // nächstes Bit vorbereiten
            bits &= ~mask;
            nextBit = (bits != 0) ? Long.numberOfTrailingZeros(bits) : 64;

            return new TileCell<>(linear, val);
        }
    }
}