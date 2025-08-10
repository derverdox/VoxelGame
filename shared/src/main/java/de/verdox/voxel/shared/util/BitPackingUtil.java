package de.verdox.voxel.shared.util;

public class BitPackingUtil {
    public static float packToFloat(int bitOffset, int data, int bitSize) {
        if (bitOffset < 0 || bitSize <= 0 || bitOffset + bitSize > Float.SIZE)
            throw new IllegalArgumentException("invalid range");

        final int mask = (bitSize == Float.SIZE) ? -1 : ((1 << bitSize) - 1);
        final int bits = (data & mask) << bitOffset;
        return Float.intBitsToFloat(bits);
    }

    /**
     * Erweiterung: schreibt in einen bestehenden Float ab bitOffset.
     */
    public static float packToFloat(float base, int bitOffset, int data, int bitSize) {
        if (bitOffset < 0 || bitSize <= 0 || bitOffset + bitSize > Float.SIZE)
            throw new IllegalArgumentException("invalid range");

        final int mask = (bitSize == Float.SIZE) ? -1 : ((1 << bitSize) - 1);
        int b = Float.floatToRawIntBits(base);
        b = (b & ~(mask << bitOffset)) | ((data & mask) << bitOffset);
        return Float.intBitsToFloat(b);
    }

    public static int unpackFromFloat(float packed, int bitOffset, int bitSize) {
        if (bitOffset < 0 || bitSize <= 0 || bitOffset + bitSize > Float.SIZE)
            throw new IllegalArgumentException("invalid range");

        final int raw = Float.floatToRawIntBits(packed);
        final int mask = (bitSize == Float.SIZE) ? -1 : ((1 << bitSize) - 1);
        return (raw >>> bitOffset) & mask;
    }
}
