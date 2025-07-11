package de.verdox.voxel.shared.util.palette;

import lombok.Getter;

import java.util.*;

@Getter
public class ThreeDimensionalPalette<T> {
    private final short dimensionX, dimensionY, dimensionZ;
    private final int totalSize;

    // Palette: ID 0 reserved for defaultValue
    private final Map<T, Integer> blockToId = new HashMap<>();
    private final List<T> idToBlock = new ArrayList<>();
    private final T defaultValue;

    // Bit-packed storage
    private int bitsPerBlock;
    private long[] data;

    private long localDataChangeVersion;

    /**
     * Create a palette region of given dimensions, all initialized to defaultValue.
     */
    public ThreeDimensionalPalette(T defaultValue, short dimensionX, short dimensionY, short dimensionZ) {
        this.defaultValue = defaultValue;
        this.dimensionX = dimensionX;
        this.dimensionY = dimensionY;
        this.dimensionZ = dimensionZ;
        this.totalSize = (int) dimensionX * dimensionY * dimensionZ;

        // initialize palette
        idToBlock.add(defaultValue);
        blockToId.put(defaultValue, 0);

        // minimum 4 bits per block
        this.bitsPerBlock = 4;
        int longs = (totalSize * bitsPerBlock + 63) >>> 6;
        this.data = new long[longs];
    }

    /**
     * Returns a deep copy of this palette, including internal bit-array.
     */
    public ThreeDimensionalPalette<T> copy() {
        ThreeDimensionalPalette<T> clone = new ThreeDimensionalPalette<>(defaultValue, dimensionX, dimensionY, dimensionZ);
        // copy palette lists
        clone.idToBlock.clear();
        clone.idToBlock.addAll(this.idToBlock);
        clone.blockToId.clear();
        clone.blockToId.putAll(blockToId);
        // copy bit-width and data
        clone.bitsPerBlock = this.bitsPerBlock;
        clone.data = this.data.clone();
        return clone;
    }

    /**
     * Get block at (x,y,z).
     */
    public T get(short x, short y, short z) {
        checkBounds(x, y, z);
        int idx = computeIndex(x, y, z);
        int id = readPaletteIndex(idx);
        return idToBlock.get(id);
    }

    /**
     * Set block at (x,y,z).
     */
    public void set(short x, short y, short z, T block) {
        checkBounds(x, y, z);
        int idx = computeIndex(x, y, z);

        int oldId = readPaletteIndex(idx);

        Integer id = blockToId.get(block);
        if (id == null) {
            id = idToBlock.size();
            idToBlock.add(block);
            blockToId.put(block, id);
            resizeIfNeeded();
        }
        writePaletteIndex(idx, id);

        if (oldId != id) {
            writePaletteIndex(idx, id);
            localDataChangeVersion++;
        }
    }

    /**
     * Returns an unmodifiable view of the palette (ID → block).
     */
    public List<T> getPalette() {
        return Collections.unmodifiableList(idToBlock);
    }

    // --- internal methods ---

    private void checkBounds(short x, short y, short z) {
        if (x < 0 || x >= dimensionX || y < 0 || y >= dimensionY || z < 0 || z >= dimensionZ) {
            throw new IndexOutOfBoundsException(
                "Coordinates out of bounds: (" + x + "," + y + "," + z + ")");
        }
    }

    private int computeIndex(short x, short y, short z) {
        return x + dimensionX * (y + dimensionY * z);
    }

    private void resizeIfNeeded() {
        int requiredBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(idToBlock.size() - 1));
        if (requiredBits != bitsPerBlock) {
            // allocate new data array
            long[] newData = new long[(totalSize * requiredBits + 63) >>> 6];
            // re-pack old values
            for (int i = 0; i < totalSize; i++) {
                int oldId = readPaletteIndex(i);
                writeBits(newData, i, oldId, requiredBits);
            }
            bitsPerBlock = requiredBits;
            data = newData;
        }
    }

    private int readPaletteIndex(int cellIndex) {
        int bitPos = cellIndex * bitsPerBlock;
        int longPos = bitPos >>> 6;
        int offset = bitPos & 0x3F;
        long segment = data[longPos] >>> offset;
        int remaining = bitsPerBlock;
        if (offset + bitsPerBlock > 64) {
            int overflow = offset + bitsPerBlock - 64;
            segment |= data[longPos + 1] << (64 - offset);
        }
        return (int) (segment & ((1L << bitsPerBlock) - 1));
    }

    private void writePaletteIndex(int cellIndex, int id) {
        writeBits(this.data, cellIndex, id, bitsPerBlock);
    }

    private void writeBits(long[] targetData, int cellIndex, int value, int bitWidth) {
        int bitPos = cellIndex * bitWidth;
        int longPos = bitPos >>> 6;
        int offset = bitPos & 0x3F;
        long mask = ((1L << bitWidth) - 1) << offset;
        // clear bits then set
        targetData[longPos] = (targetData[longPos] & ~mask) | (((long) value << offset) & mask);
        int overflow = offset + bitWidth - 64;
        if (overflow > 0) {
            long mask2 = (1L << overflow) - 1;
            targetData[longPos + 1] =
                (targetData[longPos + 1] & ~mask2) |
                    ((long) value >>> (bitWidth - overflow) & mask2);
        }
    }

    public void setForSerialization(T block, int id) {
        if (blockToId.containsKey(block)) {
            return;
        }
        idToBlock.add(block);
        blockToId.put(block, id);
    }

    public void setForDeserialization(int bitsPerBlock, long[] data) {
        this.bitsPerBlock = bitsPerBlock;
        this.data = data;
    }

    @Override
    public String toString() {
        return "ThreeDimensionalPalette{" +
            "dimensionX=" + dimensionX +
            ", dimensionY=" + dimensionY +
            ", dimensionZ=" + dimensionZ +
            ", totalSize=" + totalSize +
            ", blockToId=" + blockToId +
            ", idToBlock=" + idToBlock +
            ", defaultValue=" + defaultValue +
            ", bitsPerBlock=" + bitsPerBlock +
            ", data=" + Arrays.toString(data) +
            '}';
    }
}
