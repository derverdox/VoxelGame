package de.verdox.voxel.server.level.chunk.grid;

import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.chunk.Box;

import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.util.BitPackingUtil;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.Objects;

public class SparseOTChunkGrid {

    private static final byte SIZE = 4;

    private static final byte MAX_LEVEL = 15;

    private static final byte SIZE_BITS = 2;

    private static final byte LEVEL_BITS = 4;

    private static final byte GRID_BITS = 7;

    private static final long FULL_MASK = ~0L; // 64 Bits gesetzt

    private final World world;
    private final byte maxLevel;
    private final long xBoundsTopWorldLevel, yBoundsTopWorldLevel, zBoundsTopWorldLevel;

    private final Long2ObjectMap<RegionNode> nodes = new Long2ObjectOpenHashMap<>();

    @Getter
    private int amountMerged = 0;

    public SparseOTChunkGrid(World world, byte maxLevel) {
        this.world = world;
        this.maxLevel = maxLevel;

        xBoundsTopWorldLevel = calculateWorldBoundsXForLevel((byte) 0);
        yBoundsTopWorldLevel = calculateWorldBoundsYForLevel((byte) 0);
        zBoundsTopWorldLevel = calculateWorldBoundsZForLevel((byte) 0);
    }

    public int getSize() {
        return nodes.size();
    }

    public void addOrUpdateChunk(Chunk chunk) {
        int treeX = Math.toIntExact(Math.floorMod((long) chunk.getChunkX() * world.getChunkSizeX(), xBoundsTopWorldLevel));
        int treeY = Math.toIntExact(Math.floorMod((long) chunk.getChunkY() * world.getChunkSizeY(), yBoundsTopWorldLevel));
        int treeZ = Math.toIntExact(Math.floorMod((long) chunk.getChunkZ() * world.getChunkSizeZ(), zBoundsTopWorldLevel));

        byte lxLeaf = (byte) Math.floorMod(chunk.getChunkX(), SIZE);
        byte lyLeaf = (byte) Math.floorMod(chunk.getChunkY(), SIZE);
        byte lzLeaf = (byte) Math.floorMod(chunk.getChunkZ(), SIZE);

        long leafKey = constructKey(maxLevel, treeX, treeY, treeZ, lxLeaf, lyLeaf, lzLeaf);

        boolean isUniform = isUniform(chunk);
        short prominentMaterial = extractProminentMaterial(chunk);

        // ========= UPDATE-PFAD: Leaf existiert bereits =========
        if (nodes.containsKey(leafKey)) {
            LeafNode leafNode = (LeafNode) nodes.get(leafKey);

            if (leafNode.prominentMaterial == prominentMaterial && leafNode.uniform == isUniform) {
                // Nichts zu tun
                return;
            }

            // Leaf inhaltlich aktualisieren
            leafNode.prominentMaterial = prominentMaterial;
            leafNode.uniform = isUniform;

            // Aufwärts propagieren: Eltern existieren garantiert.
            propagateUpAfterChildChange(chunk, treeX, treeY, treeZ);
            return;
        }

        // ========= INSERT-PFAD: Leaf existiert noch nicht =========

        // Pfad-Digits je Ebene vorbereiten: pathL*[level] = lokaler Index auf der Ebene "level"
        byte[] pathLx = new byte[maxLevel + 1];
        byte[] pathLy = new byte[maxLevel + 1];
        byte[] pathLz = new byte[maxLevel + 1];

        {
            long tx = chunk.getChunkX();
            long ty = chunk.getChunkY();
            long tz = chunk.getChunkZ();
            for (int depthFromLeaf = 0; depthFromLeaf <= maxLevel; depthFromLeaf++) {
                byte digitX = (byte) Math.floorMod(tx, SIZE);
                byte digitY = (byte) Math.floorMod(ty, SIZE);
                byte digitZ = (byte) Math.floorMod(tz, SIZE);
                int level = maxLevel - depthFromLeaf; // tatsächliche Ebene
                pathLx[level] = digitX;
                pathLy[level] = digitY;
                pathLz[level] = digitZ;

                tx = Math.floorDiv(tx, SIZE);
                ty = Math.floorDiv(ty, SIZE);
                tz = Math.floorDiv(tz, SIZE);
            }
        }

        // Von oben nach unten gehen (Ebene 0 .. maxLevel-1)
        for (byte level = 0; level < maxLevel; level++) {
            long key = constructKey(level, treeX, treeY, treeZ, pathLx[level], pathLy[level], pathLz[level]);
            IntermediateNode parent = (IntermediateNode) nodes.get(key);
            if (parent == null) {
                parent = new IntermediateNode();
                // Neuer Parent: zunächst "komprimiert" mit Zielwerten
                parent.prominentMaterial = prominentMaterial;
                parent.uniform = isUniform;
                parent.merged = true;
                nodes.put(key, parent);
            }

            // Kindindex (nächste Ebene)
            byte cx = pathLx[level + 1];
            byte cy = pathLy[level + 1];
            byte cz = pathLz[level + 1];

            if (parent.merged) {
                if (parent.uniform == isUniform && parent.prominentMaterial == prominentMaterial) {
                    // Inhalt identisch -> nicht weiter absteigen, keine Kinder materialisieren,
                    // und KEIN existBit setzen (gemergte Parents führen keine Kinder).
                    return;
                } else {
                    // Split: alle 64 Kinder materialisieren und parent wird "expanded"
                    splitMergedParent(parent, (byte) (level + 1), treeX, treeY, treeZ);
                    parent.merged = false;
                }
            }

            // Sicherstellen, dass das (level+1)-Kind existiert:
            long childKey = constructKey((byte) (level + 1), treeX, treeY, treeZ, cx, cy, cz);
            RegionNode child = nodes.get(childKey);
            if (child == null) {
                if (level + 1 == maxLevel) {
                    LeafNode leaf = new LeafNode();
                    leaf.prominentMaterial = prominentMaterial;
                    leaf.uniform = isUniform;
                    leaf.merged = false;
                    nodes.put(childKey, leaf);
                } else {
                    IntermediateNode inter = new IntermediateNode();
                    // neue Zwischenknoten: zunächst "komprimiert" mit Zielwerten
                    inter.prominentMaterial = prominentMaterial;
                    inter.uniform = isUniform;
                    inter.merged = true;
                    nodes.put(childKey, inter);
                }
            }

            // Parent ist nicht gemergt -> existierendes Kind markieren
            parent.markExisting(cx, cy, cz);

            // Leaf erreicht → Aufwärts-Propagation und fertig
            if (level + 1 == maxLevel) {
                propagateUpAfterChildChange(chunk, treeX, treeY, treeZ);
                return;
            }
        }
    }

    /**
     * Materialisiert alle 64 Kinder eines gemergten Parents mit dessen bisherigem Inhalt.
     * Kinder bekommen dieselben (uniform, prominentMaterial) und starten selbst auf merged=true.
     */
    private void splitMergedParent(RegionNode parent, byte childLevel, int treeX, int treeY, int treeZ) {
        for (byte ix = 0; ix < SIZE; ix++) {
            for (byte iy = 0; iy < SIZE; iy++) {
                for (byte iz = 0; iz < SIZE; iz++) {
                    long childKey = constructKey(childLevel, treeX, treeY, treeZ, ix, iy, iz);

                    if (!nodes.containsKey(childKey)) {
                        RegionNode child;
                        if (childLevel == maxLevel) {
                            child = new LeafNode();
                        } else {
                            child = new IntermediateNode();
                        }
                        child.prominentMaterial = parent.prominentMaterial;
                        child.uniform = parent.uniform;
                        child.merged = true; // weiterhin komprimiert
                        nodes.put(childKey, child);
                    }
                    parent.markExisting(ix, iy, iz);
                }
            }
        }
    }

    /**
     * Prüft von Ebene (maxLevel-1) bis 0 die Eltern; wenn alle 64 Kinder existieren und
     * alle identische (uniform, material) haben, wird der Parent gemergt:
     * - alle direkten Kinder werden aus der Map entfernt
     * - existMask wird auf 0 gesetzt
     * - Parent übernimmt (uniform, material) und setzt merged=true
     */
    private void propagateUpAfterChildChange(Chunk chunk, int treeX, int treeY, int treeZ) {
        // Pfad-Digits je Ebene
        byte[] pathLx = new byte[maxLevel + 1];
        byte[] pathLy = new byte[maxLevel + 1];
        byte[] pathLz = new byte[maxLevel + 1];

        long tx = chunk.getChunkX();
        long ty = chunk.getChunkY();
        long tz = chunk.getChunkZ();
        for (int depthFromLeaf = 0; depthFromLeaf <= maxLevel; depthFromLeaf++) {
            byte digitX = (byte) Math.floorMod(tx, SIZE);
            byte digitY = (byte) Math.floorMod(ty, SIZE);
            byte digitZ = (byte) Math.floorMod(tz, SIZE);
            int level = maxLevel - depthFromLeaf;
            pathLx[level] = digitX;
            pathLy[level] = digitY;
            pathLz[level] = digitZ;

            tx = Math.floorDiv(tx, SIZE);
            ty = Math.floorDiv(ty, SIZE);
            tz = Math.floorDiv(tz, SIZE);
        }

        for (byte level = (byte) (maxLevel - 1); level >= 0; level--) {
            long parentKey = constructKey(level, treeX, treeY, treeZ, pathLx[level], pathLy[level], pathLz[level]);
            IntermediateNode parent = (IntermediateNode) nodes.get(parentKey);
            Objects.requireNonNull(parent, "Missing parent node in tree");

            // Kindindex in diesem Parent:
            byte cx = pathLx[level + 1];
            byte cy = pathLy[level + 1];
            byte cz = pathLz[level + 1];

            // Parent könnte gemergt sein (z.B. ganz oben) – in diesem Fall führt er keine Kinder.
            // Wenn er nicht gemergt ist, stellen wir sicher, dass das Existenzbit gesetzt ist.
            if (!parent.merged) {
                parent.markExisting(cx, cy, cz);
            }

            // Nur "expandierte" Parents können zu einem neuen Merge evaluiert werden
            if (!parent.merged && parent.existMask == FULL_MASK) {
                Boolean commonUniform = null;
                Short commonMaterial = null;
                boolean allEqual = true;

                outer:
                for (byte ix = 0; ix < SIZE; ix++) {
                    for (byte iy = 0; iy < SIZE; iy++) {
                        for (byte iz = 0; iz < SIZE; iz++) {
                            long childKey = constructKey((byte) (level + 1), treeX, treeY, treeZ, ix, iy, iz);
                            RegionNode child = nodes.get(childKey);
                            if (child == null) { // Inkonsistenz – dann kein Merge
                                allEqual = false;
                                break outer;
                            }
                            if (commonUniform == null) {
                                commonUniform = child.uniform;
                                commonMaterial = child.prominentMaterial;
                            } else {
                                if (commonUniform != child.uniform || commonMaterial != child.prominentMaterial) {
                                    allEqual = false;
                                    break outer;
                                }
                            }
                        }
                    }
                }

                if (allEqual) {
                    // MERGE: Parent übernimmt die Werte, Kinder werden entfernt
                    parent.uniform = commonUniform;
                    parent.prominentMaterial = commonMaterial;
                    parent.merged = true;
                    amountMerged++;

                    // Direkte Kinder löschen und existMask leeren
                    pruneChildrenForMerge((byte) (level + 1), treeX, treeY, treeZ,
                            pathLx[level], pathLy[level], pathLz[level]);
                    parent.existMask = 0L;
                } else {
                    parent.merged = false;
                    amountMerged--;
                }
            }

            if (level == 0) break; // byte-Underflow vermeiden
        }
    }

    /**
     * Entfernt alle 64 direkten Kinder des Parents mit lokaler Position (px,py,pz) auf Ebene 'level'
     * → also Kinder auf 'childLevel = level+1' unter demselben Grid (treeX/Y/Z) und Parent-Lokalen.
     */
    private void pruneChildrenForMerge(byte childLevel, int treeX, int treeY, int treeZ,
                                       byte px, byte py, byte pz) {
        for (byte ix = 0; ix < SIZE; ix++) {
            for (byte iy = 0; iy < SIZE; iy++) {
                for (byte iz = 0; iz < SIZE; iz++) {
                    long childKey = constructKey(childLevel, treeX, treeY, treeZ, ix, iy, iz);
                    // Wichtig: childKeys müssen zu demselben Parent gehören.
                    // Da level/lokale Koordinaten kodiert sind, reicht es aus,
                    // dass childLevel exakt level+1 ist und gridX/Y/Z gleich sind.
                    // (Die Parent-Lokalen (px,py,pz) sind Bestandteil des parentKey,
                    // die Kinder haben ihre eigenen lokalen (ix,iy,iz) auf childLevel.)
                    nodes.remove(childKey);
                }
            }
        }
    }


    abstract class RegionNode implements Box {
        protected short prominentMaterial;
        protected boolean uniform;
        protected boolean merged; // a flag indicating whether childs are missing because they are merged into this parent node or because they have not been generated yet.
        protected long existMask; // 4x4x4 Child nodes per node. Existence masked into 64bit

        public boolean isFullyTransparent() {
            for (byte x = 0; x < getSizeX(); x++) {
                for (byte y = 0; y < getSizeY(); y++) {
                    for (byte z = 0; z < getSizeZ(); z++) {
                        if (!isFullyTransparent(x, y, z)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        public void markExisting(byte lx, byte ly, byte lz) {
            checkLocals(lx, ly, lz);

            int bitToMarkTrue = lz + lx * getSizeX() + ly * getSizeY() * getSizeX();
            existMask |= (1L << bitToMarkTrue);
        }

        public boolean exists(byte lx, byte ly, byte lz) {
            int bit = ((ly & 3) << 4) | ((lx & 3) << 2) | (lz & 3);
            return (existMask & (1L << bit)) != 0;
        }

        @Override
        public int getSizeY() {
            return SIZE;
        }

        @Override
        public int getSizeX() {
            return SIZE;
        }

        @Override
        public int getSizeZ() {
            return SIZE;
        }

        public abstract boolean isFullyTransparent(byte x, byte y, byte z);
    }

    private class IntermediateNode extends RegionNode {
        @Override
        public boolean isFullyTransparent(byte x, byte y, byte z) {
            return false;
        }
    }

    private class LeafNode extends RegionNode {
        @Override
        public boolean isFullyTransparent(byte x, byte y, byte z) {
            return false;
        }
    }

    private boolean isUniform(Chunk chunk) {
        return chunk.isEmpty() || chunk.getChunkBlockPalette().getState().equals(ThreeDimensionalPalette.State.UNIFORM);
    }

    private short extractProminentMaterial(Chunk chunk) {
        return Blocks.AIR.getMaterialID();
    }

    private long calculateWorldBoundsXForLevel(byte level) {
        return (long) (world.getChunkSizeX() * (Math.pow(2, MAX_LEVEL - level)) * SIZE);
    }

    private long calculateWorldBoundsYForLevel(byte level) {
        return (long) (world.getChunkSizeY() * (Math.pow(2, MAX_LEVEL - level)) * SIZE);
    }

    private long calculateWorldBoundsZForLevel(byte level) {
        return (long) (world.getChunkSizeZ() * (Math.pow(2, MAX_LEVEL - level)) * SIZE);
    }

    /**
     * 4 bits for Level (0-15)
     * 2 bits for lx
     * 2 bits for ly
     * 2 bits for lz
     */
    private static long constructKey(byte level, int gridX, int gridY, int gridZ, byte lx, byte ly, byte lz) {
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("level must be between 0 and " + MAX_LEVEL);
        }
        checkLocals(lx, ly, lz);

        long packed = 0L;
        int offset = 0;


        packed = BitPackingUtil.packToLong(packed, offset, level, LEVEL_BITS);
        offset += LEVEL_BITS;
        // 4 Bits

        packed = BitPackingUtil.packToLong(packed, offset, lx, SIZE_BITS);
        offset += SIZE_BITS;
        // 6 Bits

        packed = BitPackingUtil.packToLong(packed, offset, ly, SIZE_BITS);
        offset += SIZE_BITS;
        // 8 Bits

        packed = BitPackingUtil.packToLong(packed, offset, lz, SIZE_BITS);
        offset += SIZE_BITS;
        // 10 Bits

        packed = BitPackingUtil.packToLong(packed, offset, gridX, GRID_BITS);
        offset += GRID_BITS;

        packed = BitPackingUtil.packToLong(packed, offset, gridY, GRID_BITS);
        offset += GRID_BITS;

        packed = BitPackingUtil.packToLong(packed, offset, gridZ, GRID_BITS);
        offset += GRID_BITS;

        return packed;
    }

    private static void checkLocals(byte lx, byte ly, byte lz) {
        if (lx < 0 || lx >= SIZE) {
            throw new IllegalArgumentException("lx must be between 0 and " + SIZE);
        }
        if (ly < 0 || ly >= SIZE) {
            throw new IllegalArgumentException("ly must be between 0 and " + SIZE);
        }
        if (lz < 0 || lz >= SIZE) {
            throw new IllegalArgumentException("lz must be between 0 and " + SIZE);
        }
    }
}
