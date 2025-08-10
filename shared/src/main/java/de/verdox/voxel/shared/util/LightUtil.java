package de.verdox.voxel.shared.util;

public class LightUtil {
    public static float packLightToFloat(byte sky, byte r, byte g, byte b) {
        // 4 Bit pro Komponente
        int packed = ((sky & 0xF) << 12)
                | ((r & 0xF) << 8)
                | ((g & 0xF) << 4)
                | (b & 0xF);
        return (float) packed;
    }

    /**
     * Extrahiert die Himmelslicht-Komponente (Bits 12–15) aus dem gepackten float.
     */
    public static byte unpackSkyFromFloat(float packedLight) {
        int packed = (int) packedLight;
        return (byte) ((packed >>> 12) & 0xF);
    }

    /**
     * Extrahiert die Rot-Komponente (Bits 8–11) aus dem gepackten float.
     */
    public static byte unpackRedFromFloat(float packedLight) {
        int packed = (int) packedLight;
        return (byte) ((packed >>> 8) & 0xF);
    }

    /**
     * Extrahiert die Grün-Komponente (Bits 4–7) aus dem gepackten float.
     */
    public static byte unpackGreenFromFloat(float packedLight) {
        int packed = (int) packedLight;
        return (byte) ((packed >>> 4) & 0xF);
    }

    /**
     * Extrahiert die Blau-Komponente (Bits 0–3) aus dem gepackten float.
     */
    public static byte unpackBlueFromFloat(float packedLight) {
        int packed = (int) packedLight;
        return (byte) (packed & 0xF);
    }

    /**
     * Packt Sky-, R-, G-, B- (jeweils 4 Bit) und AO- (2 Bit, Werte 0–3) in einen Float.
     * Die einzelnen Bit‑Layouts sind:
     * bits 16–17: AO (0–3)
     * bits 12–15: Sky (0–15)
     * bits  8–11: R   (0–15)
     * bits  4– 7: G   (0–15)
     * bits  0– 3: B   (0–15)
     *
     * @param sky Himmelslicht (0–15)
     * @param r   R‑Komponente (0–15)
     * @param g   G‑Komponente (0–15)
     * @param b   B‑Komponente (0–15)
     * @param ao  Ambient Occlusion (0–3)
     * @return gepackter Float-Wert (genau bis 2^24)
     */
    public static float packLightAndAoToFloat(byte sky, byte r, byte g, byte b, byte ao) {
        // 4 Bit für sky,r,g,b und 2 Bit für ao
        int packed =
                ((ao & 0x3) << 16)      // AO (2 Bit)
                        | ((sky & 0xF) << 12)      // Sky (4 Bit)
                        | ((r & 0xF) << 8)      // R   (4 Bit)
                        | ((g & 0xF) << 4)      // G   (4 Bit)
                        | (b & 0xF);            // B   (4 Bit)
        return (float) packed;
    }

    public static byte packAo(byte c1Ao, byte c2Ao, byte c3Ao, byte c4Ao) {
        // c1 use bits 6–7, c2 bits 4–5, c3 bits 2–3, c4 bits 0–1
        return (byte) (((c1Ao & 0x3))
                | ((c2Ao & 0x3) << 2)
                | ((c3Ao & 0x3) << 4)
                | ((c4Ao & 0x3) << 6));
    }

    /**
     * Liest das 2-Bit-AO für eine bestimmte Ecke (0…3) aus dem gepackten Byte.
     *
     * @param packedAo  das Byte mit allen vier AO-Werten (je 2 Bit)
     * @param cornerIdx Index der Ecke 0 (c1), 1 (c2), 2 (c3) oder 3 (c4)
     * @return der AO-Wert 0…3
     */
    public static byte unpackAo(byte packedAo, int cornerIdx) {
        // Wir haben: c1 in Bits 6–7, c2 in 4–5, c3 in 2–3, c4 in 0–1
        int shift = (cornerIdx) * 2;        // cornerIdx 0 → shift=6, 1→4, 2→2, 3→0
        return (byte) ((packedAo >> shift) & 0x3);
    }
}
