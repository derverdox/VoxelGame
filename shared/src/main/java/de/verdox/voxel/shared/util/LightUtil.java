package de.verdox.voxel.shared.util;

public class LightUtil {
    public static float packLightToFloat(byte sky, byte r, byte g, byte b) {
        // 4 Bit pro Komponente
        int packed = ((sky & 0xF) << 12)
            | ((r   & 0xF) <<  8)
            | ((g   & 0xF) <<  4)
            |  (b   & 0xF);
        return (float) packed;
    }
}
