package de.verdox.voxel.shared.util.lod;

import de.verdox.voxel.shared.level.chunk.Box;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.List;

public final class LodPyramid implements Box {
    private final class Level {
        final int[] volumes;   // Zellvolumen (Randzellen kleiner)
        final int[] counts;    // Anzahl "belegt" in Zelle
        final BitSet occupied; // >50%

        private final byte level;

        Level(int lodLevel) {
            this.level = (byte) lodLevel;
            int size = getSize(getStep());
            this.volumes = new int[size];
            this.counts = new int[size];
            this.occupied = new BitSet(size);
        }

        int index(int lx, int ly, int lz) {
            int step = getStep();

            return lx + getLevelSizeX(step) * (ly + getLevelSizeY(step) * lz);
        }

        private int getStep() {
            return LODUtil.getLodScale(level);
        }

        private int getLevelSizeX(int step) {
            return ceilDiv(getSizeX(), step);
        }

        private int getLevelSizeY(int step) {
            return ceilDiv(getSizeY(), step);
        }

        private int getLevelSizeZ(int step) {
            return ceilDiv(getSizeZ(), step);
        }

        private int getSize(int step) {
            return getLevelSizeX(step) * getLevelSizeY(step) * getLevelSizeZ(step);
        }

        private int getLevelSizeX() {
            return getLevelSizeX(getStep());
        }

        private int getLevelSizeY() {
            return getLevelSizeY(getStep());
        }

        private int getLevelSizeZ() {
            return getLevelSizeZ(getStep());
        }

        private int getSize() {
            return getSize(getStep());
        }
    }

    private final GridDim baseDim;
    private final int maxLevel;
    private final OccupancyFn occ;
    private final int lodBlockId; // ID, die zurückgegeben wird, wenn occupied (oder -1, wenn uninteressant)

    private final List<Level> levels;

    public LodPyramid(GridDim baseDim, int maxLevel, OccupancyFn occupancyFn, int lodBlockId) {
        if (maxLevel < 1) throw new IllegalArgumentException("maxLevel must be >= 1");
        this.baseDim = baseDim;
        this.maxLevel = maxLevel;
        this.occ = occupancyFn;
        this.lodBlockId = lodBlockId;
        this.levels = new ArrayList<>(maxLevel);
        buildLevelDescriptors();
    }

    @Override
    public int getSizeY() {
        return baseDim.sy();
    }

    @Override
    public int getSizeX() {
        return baseDim.sx();
    }

    @Override
    public int getSizeZ() {
        return baseDim.sz();
    }

    private void buildLevelDescriptors() {
        final int X = baseDim.sx(), Y = baseDim.sy(), Z = baseDim.sz();
        for (int level = 1; level <= maxLevel; level++) {
            Level lodLevelStorage = new Level(level);

            final int step = lodLevelStorage.getStep();
            final int sx = lodLevelStorage.getLevelSizeX(step);
            final int sy = lodLevelStorage.getLevelSizeY(step);
            final int sz = lodLevelStorage.getLevelSizeZ(step);

            // Volumina je Zelle (Rand beachten)
            for (int lz = 0; lz < sz; lz++) {
                final int z0 = lz << level;
                final int dz = Math.min(step, Z - z0);
                for (int ly = 0; ly < sy; ly++) {
                    final int y0 = ly << level;
                    final int dy = Math.min(step, Y - y0);
                    for (int lx = 0; lx < sx; lx++) {
                        final int x0 = lx << level;
                        final int dx = Math.min(step, X - x0);
                        lodLevelStorage.volumes[lodLevelStorage.index(lx, ly, lz)] = dx * dy * dz;
                    }
                }
            }
            levels.add(lodLevelStorage);
        }
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    /**
     * Einmaliger Aufbau aus der Quelle.
     */
    public void rebuildFromSource(IntIdReader src) {
        // Counts leeren
        for (Level L : levels) {
            java.util.Arrays.fill(L.counts, 0);
            L.occupied.clear();
        }

        final int X = baseDim.sx(), Y = baseDim.sy(), Z = baseDim.sz();
        for (int z = 0; z < Z; z++) {
            for (int y = 0; y < Y; y++) {
                for (int x = 0; x < X; x++) {
                    final int id = src.readId(x, y, z);
                    if (!occ.test(id)) continue;

                    // Inkrementiere alle Level-Zellen, die (x,y,z) enthalten
                    for (Level L : levels) {
                        final int lx = x >> L.level;
                        final int ly = y >> L.level;
                        final int lz = z >> L.level;
                        final int li = L.index(lx, ly, lz);
                        L.counts[li]++;
                    }
                }
            }
        }

        // Occupancy aus counts ableiten (>50%)
        for (Level L : levels) {
            final int n = L.getSize();
            for (int i = 0; i < n; i++) {
                final boolean occ = L.counts[i] > (L.volumes[i] >> 1);
                if (occ) L.occupied.set(i);
            }
        }
    }

    /**
     * Inkrementelles Update einer Zelle (alter → neuer ID).
     */
    public void onCellChange(int x, int y, int z, int oldId, int newId) {
        final boolean was = occ.test(oldId);
        final boolean now = occ.test(newId);
        if (was == now) return;

        final int delta = now ? 1 : -1;
        for (Level L : levels) {
            final int lx = x >> L.level;
            final int ly = y >> L.level;
            final int lz = z >> L.level;
            final int li = L.index(lx, ly, lz);

            final int c = (L.counts[li] += delta);
            final boolean prev = L.occupied.get(li);
            final boolean cur = c > (L.volumes[li] >> 1);
            if (prev != cur) {
                if (cur) L.occupied.set(li);
                else L.occupied.clear(li);
            }
        }
    }

    public int levels() {
        return maxLevel;
    }

    public boolean isOccupied(int level, int lx, int ly, int lz) {
        checkLevel(level);
        Level L = levels.get(level - 1);
        int step = L.getStep();
        if ((lx | ly | lz) < 0 || lx >= L.getLevelSizeX(step) || ly >= L.getLevelSizeY(step) || lz >= L.getLevelSizeZ(step))
            return false;
        return L.occupied.get(L.index(lx, ly, lz));
    }

    /**
     * Liefert entweder lodBlockId (falls occupied) oder 0 (Default).
     */
    public int getLodIdOrDefault(int level, int lx, int ly, int lz) {
        final boolean occ = isOccupied(level, lx, ly, lz);
        return occ ? (lodBlockId >= 0 ? lodBlockId : 1) : 0; // falls -1: gib 1 als "belegt"-ID zurück
    }

    private void checkLevel(int level) {
        if (level < 1 || level > maxLevel) {
            throw new IllegalArgumentException("level must be in [1.." + maxLevel + "], got " + level);
        }
    }
}

