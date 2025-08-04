package de.verdox.voxel.shared.util.buffer;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

public class SIMDDynamicFloatBuffer implements DynamicFloatBuffer {
    private final boolean alwaysExact;
    private float[] buffer;
    private int size;
    private static final int DEFAULT_CAPACITY = 16;
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    /**
     * Erzeugt mit gegebener Anfangskapazität.
     */
    public SIMDDynamicFloatBuffer(int initialCapacity, boolean alwaysExact) {
        buffer = new float[Math.max(DEFAULT_CAPACITY, initialCapacity)];
        size = 0;
        this.alwaysExact = alwaysExact;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return buffer.length;
    }

    @Override
    public void append(float value) {
        ensureCapacity(size + 1, alwaysExact);
        buffer[size++] = value;
    }

    @Override
    public void set(int index, float value) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index < 0");
        }
        if (index >= size) {
            ensureCapacity(index + 1, alwaysExact);
            size = index + 1;
        }
        buffer[index] = value;
    }

    @Override
    public void fill(int start, int length, float value) {
        ensureCapacity(start + length, alwaysExact);
        if (start < 0 || length < 0 || start + length > buffer.length) {
            throw new IndexOutOfBoundsException(start + length + " > " + size);
        }
        // Species für die beste Registerbreite
        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
        FloatVector vecValue = FloatVector.broadcast(SPECIES, value);

        int i = start;
        int upper = start + length;
        // Blockweise füllen mit SIMD
        int loopBound = SPECIES.loopBound(length);
        for (; i < start + loopBound; i += SPECIES.length()) {
            vecValue.intoArray(buffer, i);
        }
        // Rest per Skalar
        for (; i < upper; i++) {
            buffer[i] = value;
        }
    }

    @Override
    public void remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index außerhalb: " + index);
        }
        buffer[index] = 0f;
        // Shrink bei geringer Auslastung
        if (size > 0 && size <= buffer.length / 4 && buffer.length > DEFAULT_CAPACITY) {
            int newCap = Math.max(DEFAULT_CAPACITY, buffer.length / 2);
            resizeBuffer(newCap);
        }
    }

    @Override
    public void insert(int index, float value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index außerhalb: " + index);
        }
        ensureCapacity(size + 1, alwaysExact);
        // Verschieben [index, size) --> [index+1, size+1)
        int from = index;
        int to = index + 1;
        int len = size - index;
        // Vektorisiertes Kopieren rückwärts
        int i = from + len - SPECIES.length();
        for (; i >= from; i -= SPECIES.length()) {
            FloatVector vec = FloatVector.fromArray(SPECIES, buffer, i);
            vec.intoArray(buffer, i + 1);
        }
        // Rest per Skalar
        int end = from + len;
        int start = Math.max(from, i + SPECIES.length());
        for (int j = start; j < end; j++) {
            buffer[j + 1] = buffer[j];
        }
        buffer[index] = value;
        size++;
    }

    @Override
    public void set(int pos, float[] src) {
        int len = src.length;
        ensureCapacity(pos + len, alwaysExact);
        if (pos < 0 || pos + len > size) {
            throw new IndexOutOfBoundsException(
                    "pos=" + pos + ", len=" + len + ", size=" + size
            );
        }

        // Wie viele Elemente passen in whole vectors?
        int upper = SPECIES.loopBound(len);
        int i = 0;

        // 1) Vektor­weise kopieren
        for (; i < upper; i += SPECIES.length()) {
            FloatVector.fromArray(SPECIES, src, i)
                    .intoArray(buffer, pos + i);
        }

        // 2) Rest-Elemente skalar kopieren
        for (; i < len; i++) {
            buffer[pos + i] = src[i];
        }
    }

    @Override
    public int update(int startIndex, int endIndex, float[] newData) {
        int oldLen = endIndex - startIndex;
        int newLen = newData.length;
        int diff = newLen - oldLen;

        // --- Bounds-Check ---
        if (startIndex < 0 || endIndex > size || startIndex > endIndex) {
            throw new IndexOutOfBoundsException(
                    "update(" + startIndex + "," + endIndex + "): size=" + size
            );
        }

        // --- 1) Platz schaffen bzw. Lücke schließen ---
        if (diff > 0) {
            // Puffer vergrößern und rechten Teil nach hinten verschieben
            ensureCapacity(size + diff, alwaysExact);
            System.arraycopy(
                    buffer,
                    endIndex,
                    buffer,
                    endIndex + diff,
                    size - endIndex
            );
        } else if (diff < 0) {
            // rechten Teil nach links verschieben (Lücke schließen)
            System.arraycopy(
                    buffer,
                    endIndex,
                    buffer,
                    startIndex + newLen,
                    size - endIndex
            );
        }
        // jetzt ist Platz für newLen Elemente an [startIndex..)

        // --- 2) SIMD-beschleunigt kopieren ---
        int i = 0;
        int upper = SPECIES.loopBound(newLen);
        for (; i < upper; i += SPECIES.length()) {
            FloatVector
                    .fromArray(SPECIES, newData, i)
                    .intoArray(buffer, startIndex + i);
        }
        // Rest-Elemente scalar kopieren
        for (; i < newLen; i++) {
            buffer[startIndex + i] = newData[i];
        }

        // --- 3) Größe anpassen und neuen End-Index berechnen ---
        size += diff;
        return startIndex + newLen;
    }

    @Override
    public void insert(int pos, float[] src) {
        int len = src.length;
        if (pos < 0 || pos > size) {
            throw new IndexOutOfBoundsException(pos + " > size=" + size);
        }

        // 1) Platz schaffen
        ensureCapacity(size + len, alwaysExact);
        System.arraycopy(buffer, pos, buffer, pos + len, size - pos);

        // 2) SIMD-kopierter Teil
        int i = 0;
        int upper = SPECIES.loopBound(len);  // größtes Vielfaches von SPECIES.length()
        for (; i < upper; i += SPECIES.length()) {
            // lade SRC[i..i+L) und speichere direkt in buffer[pos+i..)
            FloatVector.fromArray(SPECIES, src, i)
                    .intoArray(buffer, pos + i);
        }

        // 3) Rest-Elemente
        for (; i < len; i++) {
            buffer[pos + i] = src[i];
        }

        size += len;
    }

    @Override
    public void ensureCapacity(int minCapacity, boolean exact) {
        if (minCapacity >= buffer.length) {
            int newCap = exact ? minCapacity : Math.max(buffer.length * 2, minCapacity);
            resizeBuffer(newCap);
        }
    }

    @Override
    public void resizeBuffer(int newCapacity) {
        float[] newBuf = new float[newCapacity];
        int len = size;
        int i = 0;
        // SIMD-Kopien in Blöcken
        for (; i <= len - SPECIES.length(); i += SPECIES.length()) {
            FloatVector vec = FloatVector.fromArray(SPECIES, buffer, i);
            vec.intoArray(newBuf, i);
        }
        // Rest per Skalar
        for (; i < len; i++) {
            newBuf[i] = buffer[i];
        }
        buffer = newBuf;
    }

    @Override
    public float[] getSnapshot() {
        float[] arr = new float[size];
        System.arraycopy(buffer, 0, arr, 0, size);
        return arr;
    }
}
