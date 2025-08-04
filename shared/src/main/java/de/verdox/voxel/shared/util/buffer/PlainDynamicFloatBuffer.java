package de.verdox.voxel.shared.util.buffer;

import java.util.Arrays;

public class PlainDynamicFloatBuffer implements DynamicFloatBuffer {
    private final boolean alwaysExact;
    protected float[] buffer;
    protected int size;

    /**
     * Erstellt einen neuen Buffer mit gegebener Anfangskapazität.
     *
     * @param initialCapacity Mindestkapazität (>0)
     */
    public PlainDynamicFloatBuffer(int initialCapacity, boolean alwaysExact) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be > 0");
        }
        this.buffer = new float[initialCapacity];
        this.size = 0;
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
            throw new IndexOutOfBoundsException("index < 0: " + index);
        }
        if (index >= size) {
            // Lücken mit 0 füllen
            ensureCapacity(index + 1, alwaysExact);
            Arrays.fill(buffer, size, index, 0f);
            size = index + 1;
        }
        buffer[index] = value;
    }

    @Override
    public void fill(int start, int length, float value) {
        if (start < 0 || length < 0) {
            throw new IndexOutOfBoundsException("start or length negative");
        }
        int end = start + length;
        if (end < start) throw new ArithmeticException("int overflow in start+length");
        ensureCapacity(end, alwaysExact);
        Arrays.fill(buffer, start, end, value);
        if (end > size) size = end;
    }

    @Override
    public void remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index out of bounds: " + index);
        }
        // Elemente links verschieben
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(buffer, index + 1, buffer, index, numMoved);
        }
        // letztes Element löschen
        buffer[--size] = 0f;

        // Kapazität bei geringer Auslastung halbieren
        int cap = buffer.length;
        if (size <= cap / 4 && cap > 1) {
            resizeBuffer(cap / 2);
        }
    }

    @Override
    public void insert(int index, float value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index out of bounds: " + index);
        }
        ensureCapacity(size + 1, alwaysExact);
        // alles ab index um 1 nach rechts schieben
        System.arraycopy(buffer, index, buffer, index + 1, size - index);
        buffer[index] = value;
        size++;
    }

    @Override
    public void set(int pos, float[] src) {
        if (src == null) throw new NullPointerException("src");
        int len = src.length;
        if (pos < 0 || pos + len > size) {
            throw new IndexOutOfBoundsException("pos=" + pos + ", length=" + len + ", size=" + size);
        }
        if (len == 0) return;
        System.arraycopy(src, 0, buffer, pos, len);
    }

    @Override
    public int update(int startIndex, int endIndex, float[] newData) {
        if (newData == null) throw new NullPointerException("newData");
        if (startIndex < 0 || endIndex > size || startIndex > endIndex) {
            throw new IndexOutOfBoundsException("update(" + startIndex + "," + endIndex + "), size=" + size);
        }
        int oldLen = endIndex - startIndex;
        int newLen = newData.length;
        int diff   = newLen - oldLen;

        if (diff == 0) { // Fast path
            if (newLen > 0) System.arraycopy(newData, 0, buffer, startIndex, newLen);
            return endIndex;
        }

        if (diff > 0) {
            ensureCapacity(size + diff, alwaysExact);
            System.arraycopy(buffer, endIndex, buffer, endIndex + diff, size - endIndex);
        } else {
            System.arraycopy(buffer, endIndex, buffer, startIndex + newLen, size - endIndex);
        }

        if (newLen > 0) System.arraycopy(newData, 0, buffer, startIndex, newLen);

        size += diff;
        return startIndex + newLen;
    }

    @Override
    public void insert(int pos, float[] src) {
        if (pos < 0 || pos > size) {
            throw new IndexOutOfBoundsException("pos out of bounds: " + pos);
        }
        if (src == null) throw new NullPointerException("src");
        int len = src.length;
        if (len == 0) return;

        ensureCapacity(size + len, alwaysExact);

        // 1) Tail nach rechts verschieben, um Platz zu schaffen
        System.arraycopy(buffer, pos, buffer, pos + len, size - pos);

        // 2) Block kopieren (schneller als for-Schleife)
        System.arraycopy(src, 0, buffer, pos, len);

        size += len;
    }

    @Override
    public void ensureCapacity(int minCapacity, boolean exact) {
        if (buffer.length < minCapacity) {
            int newCap = exact ? minCapacity : Math.max(buffer.length * 2, minCapacity);
            resizeBuffer(newCap);
        }
    }

    @Override
    public void resizeBuffer(int newCapacity) {
        if (newCapacity < size) {
            newCapacity = size;
        }
        buffer = Arrays.copyOf(buffer, newCapacity);
    }

    @Override
    public float[] getSnapshot() {
        if(buffer == null) {
            return null;
        }
        return Arrays.copyOf(buffer, buffer.length);
    }
}
