package de.verdox.voxel.server.level.chunk;

import de.verdox.voxel.shared.level.chunk.Box;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SparseOTChunkGrid {
    private final byte sizeX;
    private final byte sizeY;
    private final byte sizeZ;

    public SparseOTChunkGrid(byte sizeX, byte sizeY, byte sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    abstract class RegionNode<CHILD> implements Box {
        private Object[] children = new Object[getSizeX() * getSizeY() * getSizeZ()];

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

        @Override
        public int getSizeY() {
            return sizeY;
        }

        @Override
        public int getSizeX() {
            return sizeX;
        }

        @Override
        public int getSizeZ() {
            return sizeZ;
        }

        public abstract boolean isFullyTransparent(byte x, byte y, byte z);
    }

    private class IntermediateNode extends RegionNode {
        private ObjectArrayList<> children;

        @Override
        public boolean isFullyTransparent(byte x, byte y, byte z) {
            return false;
        }
    }

    private class LeafNode extends RegionNode {
        private short existFlag = 0;

        @Override
        public boolean isFullyTransparent(byte x, byte y, byte z) {
            return false;
        }
    }
}
