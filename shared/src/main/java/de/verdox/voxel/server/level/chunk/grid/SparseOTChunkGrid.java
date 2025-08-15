package de.verdox.voxel.server.level.chunk.grid;

import de.verdox.voxel.server.level.chunk.grid.tiles.TileCell;
import de.verdox.voxel.server.level.chunk.grid.tiles.TileChildren;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.data.types.Registries;
import de.verdox.voxel.shared.level.chunk.Box;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStrategy;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.Objects;

public class SparseOTChunkGrid {

    /**
     * Kantenlänge pro Tile innerhalb eines Region-Nodes. 4 ist ein guter Kompromiss.
     */
    private static final int TILE_DIM = 4;

    /**
     * Material-ID, die als "Luft/transparent" gilt – mit eurer Block-ID abstimmen.
     */
    public static final int MATERIAL_AIR = Blocks.AIR.getMaterialID();


    private final GridConfig gridConfig;

    // Nodes are identified by a key.
    // A key is 64 bits.
    // 5 Bits are to decode the level of the octree node. [0-maxLevel] Max Possible Level = 2^5 = 32 Which will never be used
    // 30 bits are to decode the coordinate in the sparse octree grid. Each coordinate gets 10 bits.
    // 24 bits are to decode the relative coordinates in this level [0 - sizeX, 0 - sizeY, 0 - sizeZ] with 24 / 3 = 8 bits being the maximum size of entries per region node.
    private final Long2ObjectMap<RegionNode> octreeGrid = new Long2ObjectOpenHashMap<>();

    public SparseOTChunkGrid(byte sizeX, byte sizeY, byte sizeZ) {
        this.gridConfig = new GridConfig(sizeX, sizeY, sizeZ, TILE_DIM);
    }


    public void addChunk(Chunk chunk) {
        final byte level = 0;

        // Region-Koords + lokale Koords (alles primitive, keine Allokation)
        final int rgx = Math.floorDiv(chunk.gridX(), gridConfig.sizeX());
        final int rgy = Math.floorDiv(chunk.gridY(), gridConfig.sizeY());
        final int rgz = Math.floorDiv(chunk.gridZ(), gridConfig.sizeZ());
        final int lx  = Math.floorMod(chunk.gridX(), gridConfig.sizeX());
        final int ly  = Math.floorMod(chunk.gridY(), gridConfig.sizeY());
        final int lz  = Math.floorMod(chunk.gridZ(), gridConfig.sizeZ());

        // Leaf-Region holen/erzeugen
        final long regionKey = packKey(level, rgx, rgy, rgz);
        RegionNode rn = octreeGrid.get(regionKey);
        IntermediateNode region;
        if (rn instanceof IntermediateNode in) {
            region = in;
        } else {
            // Auf dem Leaf-Level existieren nur IntermediateNodes (Container), die Kinder = LeafNodes (Chunks) halten
            region = new IntermediateNode(level);
            octreeGrid.put(regionKey, region);
        }

        // Chunk als LeafNode einsetzen
        LeafNode leaf = new LeafNode(chunk);
        Node prev = region.getChild(lx, ly, lz);
        if (!Objects.equals(prev, leaf)) {
            region.setChild(lx, ly, lz, leaf);
            region.tryCollapse();
        } else {
            return; // nichts geändert
        }

        // Bubbling nach oben (nur IntermediateNodes über dem Leaf-Level)
        Node carry = reduceForParent(region);
        int pgx = rgx, pgy = rgy, pgz = rgz;
        byte curLevel = level;

        while (true) {
            final int prgx = Math.floorDiv(pgx, gridConfig.sizeX());
            final int prgy = Math.floorDiv(pgy, gridConfig.sizeY());
            final int prgz = Math.floorDiv(pgz, gridConfig.sizeZ());
            final int plx  = Math.floorMod(pgx, gridConfig.sizeX());
            final int ply  = Math.floorMod(pgy, gridConfig.sizeY());
            final int plz  = Math.floorMod(pgz, gridConfig.sizeZ());
            final byte parentLevel = (byte)(curLevel + 1);

            final long pKey = packKey(parentLevel, prgx, prgy, prgz);
            RegionNode pNode = octreeGrid.get(pKey);

            if (carry == null && !(pNode instanceof IntermediateNode)) break;

            IntermediateNode parent;
            if (pNode instanceof IntermediateNode pin) {
                parent = pin;
            } else {
                parent = new IntermediateNode(parentLevel);
                octreeGrid.put(pKey, parent);
            }

            Node before = parent.getChild(plx, ply, plz);
            boolean changed;
            if (carry == null) {
                if (before != null) {
                    parent.setChild(plx, ply, plz, null);
                    changed = true;
                } else {
                    changed = false;
                }
            } else {
                if (!Objects.equals(before, carry)) {
                    parent.setChild(plx, ply, plz, carry);
                    changed = true;
                } else {
                    changed = false;
                }
            }
            if (!changed) break;

            parent.tryCollapse();

            if (parent.childCount() == 0) {
                octreeGrid.remove(pKey);
                carry = null;
            } else {
                carry = reduceForParent(parent);
            }

            pgx = prgx; pgy = prgy; pgz = prgz;
            curLevel = parentLevel;
            if (curLevel == 31) break;
        }
    }

    public void removeChunkOrMarkEmpty(Chunk chunk) {
        final byte level = leafLevel;

        final int rgx = Math.floorDiv(chunk.gridX(), sizeX);
        final int rgy = Math.floorDiv(chunk.gridY(), sizeY);
        final int rgz = Math.floorDiv(chunk.gridZ(), sizeZ);
        final int lx  = Math.floorMod(chunk.gridX(), sizeX);
        final int ly  = Math.floorMod(chunk.gridY(), sizeY);
        final int lz  = Math.floorMod(chunk.gridZ(), sizeZ);

        final long regionKey = packKey(level, rgx, rgy, rgz);
        RegionNode<?> rn = octreeGrid.get(regionKey);
        if (!(rn instanceof IntermediateNode region)) return; // nichts da

        Node before = region.getChild(lx, ly, lz);
        if (before == null) return; // bereits leer

        // Entfernen (kein Kind = Luft)
        region.setChild(lx, ly, lz, null);
        region.tryCollapse();

        Node carry;
        if (region.childCount() == 0) {
            octreeGrid.remove(regionKey);
            carry = null;
        } else {
            carry = reduceForParent(region);
        }

        int pgx = rgx, pgy = rgy, pgz = rgz;
        byte curLevel = level;

        while (true) {
            final int prgx = Math.floorDiv(pgx, sizeX);
            final int prgy = Math.floorDiv(pgy, sizeY);
            final int prgz = Math.floorDiv(pgz, sizeZ);
            final int plx  = Math.floorMod(pgx, sizeX);
            final int ply  = Math.floorMod(pgy, sizeY);
            final int plz  = Math.floorMod(pgz, sizeZ);
            final byte parentLevel = (byte)(curLevel + 1);

            final long pKey = packKey(parentLevel, prgx, prgy, prgz);
            RegionNode<?> pNode = octreeGrid.get(pKey);

            if (carry == null && !(pNode instanceof IntermediateNode)) break;

            IntermediateNode parent;
            if (pNode instanceof IntermediateNode pin) {
                parent = pin;
            } else {
                parent = new IntermediateNode(sizeX, sizeY, sizeZ, parentLevel);
                octreeGrid.put(pKey, parent);
            }

            Node beforeParent = parent.getChild(plx, ply, plz);
            boolean changed;
            if (carry == null) {
                if (beforeParent != null) {
                    parent.setChild(plx, ply, plz, null);
                    changed = true;
                } else {
                    changed = false;
                }
            } else {
                if (!Objects.equals(beforeParent, carry)) {
                    parent.setChild(plx, ply, plz, carry);
                    changed = true;
                } else {
                    changed = false;
                }
            }
            if (!changed) break;

            parent.tryCollapse();

            if (parent.childCount() == 0) {
                octreeGrid.remove(pKey);
                carry = null;
            } else {
                carry = reduceForParent(parent);
            }

            pgx = prgx; pgy = prgy; pgz = prgz;
            curLevel = parentLevel;
            if (curLevel == 31) break;
        }
    }



    public interface Node extends Box {
        boolean isHomogeneous();

        boolean isFullyTransparent();

        long getContentHash();

        byte getLevel();
    }


    abstract class RegionNode implements Node {
        RegionNode(byte level) {
        }

        public abstract boolean isFullyTransparent();

        @Override
        public int getSizeY() {
            return gridConfig.sizeY();
        }

        @Override
        public int getSizeX() {
            return gridConfig.sizeX();
        }

        @Override
        public int getSizeZ() {
            return gridConfig.sizeZ();
        }

        public abstract boolean isFullyTransparent(byte x, byte y, byte z);
    }

    /**
     * Region-Node mit (potenziell) sizeX*sizeY*sizeZ Kindern (andere Nodes).
     * Kinder werden kachelig (Tiles à TILE_DIM^3) sparse gehalten.
     */
    private final class IntermediateNode extends RegionNode {

        private final TileChildren<Node> children;

        // Optional: schneller Homogenitäts-Flag (wenn alle Kinder homogen & identisch)
        private boolean homogeneous;
        @Getter
        private byte level;
        private int homogeneousMaterial = MATERIAL_AIR;

        public IntermediateNode(byte level) {
            super(level);
            this.level = level;
            this.children = new TileChildren<>(gridConfig);
        }

        /**
         * O(1): Kind setzen (null entfernt).
         */
        public void setChild(int x, int y, int z, Node child) {
            Node prev = children.set(x, y, z, child);
            if (!Objects.equals(prev, child)) {
                homogeneous = false;
            }
        }

        /**
         * O(1): Kind holen.
         */
        public Node getChild(int x, int y, int z) {
            return children.get(x, y, z);
        }

        @Override
        public boolean isFullyTransparent() {
            if (homogeneous && homogeneousMaterial == MATERIAL_AIR) return true;

            return children.allTransparent();
        }

        @Override
        public boolean isFullyTransparent(byte x, byte y, byte z) {
            Node c = children.get(x, y, z);
            return (c == null) || c.isFullyTransparent();
        }

        /**
         * Versucht zu "collapsen": wenn alle belegten Kinder homogen und mit gleichem Material sind,
         * markiere diesen Node als homogen. (Optional: zu Leaf umwandeln.)
         */
        public void tryCollapse() {
            if (children.size() == 0) {
                homogeneous = true;
                homogeneousMaterial = MATERIAL_AIR;
                return;
            }
            int commonMat = Integer.MIN_VALUE;

            for (var cur = children.cursor(); cur.hasNext(); ) {
                TileCell<Node> cell = cur.next();
                Node n = cell.value;
                if (n == null || !n.isHomogeneous()) {
                    homogeneous = false;
                    return;
                }
                // Wenn es ein Leaf ist, Material auslesen, sonst abbrechen
                int mat = (n instanceof LeafNode lf) ? lf.getHomogeneousMaterial() : Integer.MIN_VALUE;
                if (mat == Integer.MIN_VALUE) {
                    homogeneous = false;
                    return;
                }
                if (commonMat == Integer.MIN_VALUE) commonMat = mat;
                else if (commonMat != mat) {
                    homogeneous = false;
                    return;
                }
            }
            homogeneous = true;
            homogeneousMaterial = (commonMat == Integer.MIN_VALUE) ? MATERIAL_AIR : commonMat;
        }

        @Override
        public boolean isHomogeneous() {
            return homogeneous;
        }

        public int homogeneousMaterial() {
            return homogeneous ? homogeneousMaterial : MATERIAL_AIR;
        }

        @Override
        public long getContentHash() {
            // robust: Hash über (tile index, bitmask, child.hash)
            long h = 1469598103934665603L;

            for (var cur = children.cursor(); cur.hasNext(); ) {
                TileCell<Node> cell = cur.next();
                int linear = cell.linearIndex;
                Node n = cell.value;
                long ch = (n == null) ? 0L : n.getContentHash();
                h = fnv64(h ^ (linear * 0x9E3779B97F4A7C15L));
                h = fnv64(h ^ ch);
            }
            // Homogenitätsflag mit hashen
            h = fnv64(h ^ (homogeneous ? 0xC0FFEE : 0xBADC0DE));
            h = fnv64(h ^ homogeneousMaterial);
            return h;
        }

        private static long fnv64(long x) {
            long hash = 1469598103934665603L;
            hash ^= x;
            hash *= 1099511628211L;
            return hash;
        }
    }

    private final class LeafNode extends RegionNode {
        @Getter
        private final boolean fullyTransparent;
        @Getter
        private final boolean isHomogeneous;
        @Getter
        private final long contentHash;
        @Getter
        private short homogeneousMaterial;


        public LeafNode(Chunk chunk) {
            super((byte) 0);

            switch (chunk.getChunkBlockPalette().getState()) {
                case EMPTY -> {
                    fullyTransparent = true;
                    isHomogeneous = true;
                    homogeneousMaterial = MATERIAL_AIR;
                }
                case UNIFORM -> {
                    PaletteStrategy.Uniform<ResourceLocation> strategy = (PaletteStrategy.Uniform<ResourceLocation>) chunk
                            .getChunkBlockPalette().getStrategy();
                    homogeneousMaterial = getBlockMaterialID(strategy.getUniformValue());
                    isHomogeneous = true;
                    fullyTransparent = true;
                }
                default -> {
                    fullyTransparent = false;
                    isHomogeneous = false;
                }
            }

            this.contentHash = chunk.getChunkBlockPalette().contentHash();
        }

        @Override
        public boolean isFullyTransparent(byte x, byte y, byte z) {
            return false;
        }

        @Override
        public byte getLevel() {
            //TODO: Leaf nodes always have the highest possible level
            return 0;
        }
    }

    private static short getBlockMaterialID(ResourceLocation blockKey) {
        return Registries.BLOCKS.get(blockKey).getMaterialID();
    }

    // Für den Parent „reduzieren“: null (leer), homogene Leaf, oder Node selbst
    private Node reduceForParent(Node node) {
        if (node == null) return null;
        if (node instanceof LeafNode leaf) {
            if (leaf.isFullyTransparent()) return null; // Luft -> Parent-Kind weglassen
            if (leaf.isHomogeneous()) {
                return LeafNode.homogeneous(leaf.sizeX, leaf.sizeY, leaf.sizeZ, leaf.level(), leaf.getHomogeneousMaterial());
            }
            return leaf;
        }
        if (node instanceof IntermediateNode in) {
            if (in.childCount() == 0) return null;
            if (in.isHomogeneous()) {
                return LeafNode.homogeneous((byte)in.getSizeX(), (byte)in.getSizeY(), (byte)in.getSizeZ(),
                        in.level(), in.homogeneousMaterial());
            }
            return in;
        }
        return node;
    }
}
