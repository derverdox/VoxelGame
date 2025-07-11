package de.verdox.voxel.shared.util;

public class BitPackingUtil {

    /**
     * Berechnet die minimale Länge eines 1D-Arrays, in das man
     * bitgepackte (x,y,z)-Koordinaten im Bereich
     * x ∈ [–radiusX…+radiusX],
     * y ∈ [–radiusY…+radiusY],
     * z ∈ [–radiusZ…+radiusZ]
     * eindeutig unterbringen kann.
     *
     * @param radiusX Maximaler Absolutwert auf X (d.h. Range = 2*radiusX+1)
     * @param radiusY Maximaler Absolutwert auf Y
     * @param radiusZ Maximaler Absolutwert auf Z
     * @return erforderliche Länge des 1D-Arrays
     * @throws IllegalArgumentException wenn Ergebnis > Integer.MAX_VALUE
     */
    public static int calculateRadialBitPackingArraySize(int radiusX, int radiusY, int radiusZ) {
        if (radiusX < 0 || radiusY < 0 || radiusZ < 0) {
            throw new IllegalArgumentException("Radien müssen ≥ 0 sein");
        }
        // Anzahl der Werte pro Achse
        int sizeX = 2 * radiusX + 1;
        int sizeY = 2 * radiusY + 1;
        int sizeZ = 2 * radiusZ + 1;
        // Bits je Achse = ceil(log2(size))
        int bitsX = sizeX > 1 ? 32 - Integer.numberOfLeadingZeros(sizeX - 1) : 1;
        int bitsY = sizeY > 1 ? 32 - Integer.numberOfLeadingZeros(sizeY - 1) : 1;
        int bitsZ = sizeZ > 1 ? 32 - Integer.numberOfLeadingZeros(sizeZ - 1) : 1;
        // tatsächliche Kapazität je Achse = 2^bits
        int dimX = 1 << bitsX;
        int dimY = 1 << bitsY;
        int dimZ = 1 << bitsZ;
        long total = (long) dimX * dimY * dimZ;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Benötigte Array-Größe zu groß: " + total);
        }
        return (int) total;
    }

    /**
     * Verwandelt eine Koordinate (x,y,z) ∈ [–radius…+radius] in den
     * 1D-Index des bitgepackten Arrays.
     *
     * @param x       X-Koordinate (–radiusX ≤ x ≤ radiusX)
     * @param y       Y-Koordinate (–radiusY ≤ y ≤ radiusY)
     * @param z       Z-Koordinate (–radiusZ ≤ z ≤ radiusZ)
     * @param radiusX Maximaler Absolutwert auf X
     * @param radiusY Maximaler Absolutwert auf Y
     * @param radiusZ Maximaler Absolutwert auf Z
     * @return Index im 1D-Array
     * @throws IllegalArgumentException wenn Koordinate außerhalb liegt
     */
    public static int getRadialBitPackingArrayIndex(int x, int y, int z, int radiusX, int radiusY, int radiusZ) {
        // Bounds-Check
        if (x < -radiusX || x > radiusX ||
            y < -radiusY || y > radiusY ||
            z < -radiusZ || z > radiusZ) {
            throw new IllegalArgumentException(
                String.format("Koordinate (%d,%d,%d) außerhalb ±(%d,%d,%d)",
                    x, y, z, radiusX, radiusY, radiusZ));
        }
        // Offsets ins Positive
        int ux = x + radiusX;
        int uy = y + radiusY;
        int uz = z + radiusZ;
        // Range-Längen
        int sizeX = 2 * radiusX + 1;
        int sizeY = 2 * radiusY + 1;
        int sizeZ = 2 * radiusZ + 1;
        // Bits je Achse
        int bitsZ = sizeZ > 1 ? 32 - Integer.numberOfLeadingZeros(sizeZ - 1) : 1;
        int bitsY = sizeY > 1 ? 32 - Integer.numberOfLeadingZeros(sizeY - 1) : 1;
        // X wird in die obersten Bits geschoben
        int shiftXY = bitsY + bitsZ;
        // Index-Berechnung: | xBits | yBits | zBits |
        return (ux << shiftXY)
            | (uy << bitsZ)
            | uz;
    }

    public static boolean isRadialValidIndex(int x, int y, int z,
                                             int radiusX, int radiusY, int radiusZ) {
        return x >= -radiusX && x <= radiusX &&
            y >= -radiusY && y <= radiusY &&
            z >= -radiusZ && z <= radiusZ;
    }

    /**
     * Berechnet die minimale Länge eines 1D-Arrays, in das man
     * bitgepackte (x,y,z)-Koordinaten im Bereich
     * x ∈ [0…limitX-1],
     * y ∈ [0…limitY-1],
     * z ∈ [0…limitZ-1]
     * eindeutig unterbringen kann.
     *
     * @param limitX exklusive Obergrenze auf X (d.h. Werte 0…limitX-1)
     * @param limitY exklusive Obergrenze auf Y
     * @param limitZ exklusive Obergrenze auf Z
     * @return erforderliche Länge des 1D-Arrays
     * @throws IllegalArgumentException wenn einer der Limits ≤ 0 oder Ergebnis > Integer.MAX_VALUE
     */
    public static int calculateBitPackingArraySize(int limitX, int limitY, int limitZ) {
        if (limitX <= 0 || limitY <= 0 || limitZ <= 0) {
            throw new IllegalArgumentException("Limits müssen > 0 sein");
        }
        // Werte pro Achse
        int sizeX = limitX;
        int sizeY = limitY;
        int sizeZ = limitZ;
        // Bits je Achse = ceil(log2(size))
        int bitsX = sizeX > 1 ? 32 - Integer.numberOfLeadingZeros(sizeX - 1) : 1;
        int bitsY = sizeY > 1 ? 32 - Integer.numberOfLeadingZeros(sizeY - 1) : 1;
        int bitsZ = sizeZ > 1 ? 32 - Integer.numberOfLeadingZeros(sizeZ - 1) : 1;
        // tatsächliche Kapazität je Achse = 2^bits
        int dimX = 1 << bitsX;
        int dimY = 1 << bitsY;
        int dimZ = 1 << bitsZ;
        long total = (long) dimX * dimY * dimZ;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Benötigte Array-Größe zu groß: " + total);
        }
        return (int) total;
    }

    /**
     * Verwandelt eine Koordinate (x,y,z) ∈ [0…limit-1] in den
     * 1D-Index des bitgepackten Arrays.
     *
     * @param x      X-Koordinate (0 ≤ x < limitX)
     * @param y      Y-Koordinate (0 ≤ y < limitY)
     * @param z      Z-Koordinate (0 ≤ z < limitZ)
     * @param limitX exklusive Obergrenze auf X
     * @param limitY exklusive Obergrenze auf Y
     * @param limitZ exklusive Obergrenze auf Z
     * @return Index im 1D-Array
     * @throws IllegalArgumentException wenn Koordinate außerhalb liegt oder Limit ≤ 0
     */
    public static int getBitPackingArrayIndex(int x, int y, int z,
                                              int limitX, int limitY, int limitZ) {
        if (limitX <= 0 || limitY <= 0 || limitZ <= 0) {
            throw new IllegalArgumentException("Limits müssen > 0 sein");
        }
        // Bounds-Check
        if (x < 0 || x >= limitX ||
            y < 0 || y >= limitY ||
            z < 0 || z >= limitZ) {
            throw new IllegalArgumentException(
                String.format("Koordinate (%d,%d,%d) außerhalb [0…(%d,%d,%d)]",
                    x, y, z, limitX-1, limitY-1, limitZ-1));
        }
        // Bits je Achse
        int bitsZ = limitZ > 1 ? 32 - Integer.numberOfLeadingZeros(limitZ - 1) : 1;
        int bitsY = limitY > 1 ? 32 - Integer.numberOfLeadingZeros(limitY - 1) : 1;
        // X wird in die obersten Bits geschoben
        int shiftXY = bitsY + bitsZ;
        // Index-Berechnung: | xBits | yBits | zBits |
        return (x << shiftXY)
            | (y << bitsZ)
            | z;
    }

    /**
     * Prüft, ob eine Koordinate (x,y,z) im Bereich [0…limit-1] liegt.
     *
     * @param x      X-Koordinate
     * @param y      Y-Koordinate
     * @param z      Z-Koordinate
     * @param limitX exklusive Obergrenze auf X
     * @param limitY exklusive Obergrenze auf Y
     * @param limitZ exklusive Obergrenze auf Z
     * @return true, wenn 0 ≤ x < limitX etc., sonst false
     */
    public static boolean isBitPackingValidIndex(int x, int y, int z,
                                                 int limitX, int limitY, int limitZ) {
        return limitX > 0 && limitY > 0 && limitZ > 0 &&
            x >= 0 && x < limitX &&
            y >= 0 && y < limitY &&
            z >= 0 && z < limitZ;
    }
}
