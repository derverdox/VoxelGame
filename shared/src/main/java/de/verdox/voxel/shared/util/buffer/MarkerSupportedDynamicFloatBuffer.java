package de.verdox.voxel.shared.util.buffer;

import lombok.Getter;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * DynamicFloatBuffer with int markers that shift in O(n_markers) using primitive arrays.
 */
public class MarkerSupportedDynamicFloatBuffer extends PlainDynamicFloatBuffer {
    private int[] markerIds;
    private int[] markerStarts;
    private int[] markerLengths;
    private int markerCount;

    public MarkerSupportedDynamicFloatBuffer(int initialCapacity, boolean alwaysExact) {
        super(initialCapacity, alwaysExact);
        markerIds = new int[4];
        markerStarts = new int[4];
        markerLengths = new int[4];
        markerCount = 0;
    }

    @Getter
    public static class Marker {
        private final int id;
        private final int startIndex;
        private final int length;

        public Marker(int id, int startIndex, int length) {
            this.id = id;
            this.startIndex = startIndex;
            this.length = length;
        }
    }

    /**
     * Add a marker with given id at [startIndex, startIndex+length).
     */
    public void addMarker(int id, int startIndex, int length) {
        if (startIndex < 0 || length < 0 || startIndex + length > size()) {
            throw new IndexOutOfBoundsException("Invalid marker range");
        }
        // ensure unique
        if (findMarkerIndex(id) >= 0) {
            throw new IllegalArgumentException("Marker id exists: " + id);
        }
        // grow arrays if needed
        if (markerCount == markerIds.length) {
            int newCap = markerIds.length << 1;
            markerIds = Arrays.copyOf(markerIds, newCap);
            markerStarts = Arrays.copyOf(markerStarts, newCap);
            markerLengths = Arrays.copyOf(markerLengths, newCap);
        }
        markerIds[markerCount] = id;
        markerStarts[markerCount] = startIndex;
        markerLengths[markerCount] = length;
        markerCount++;
    }

    /**
     * Remove marker by id.
     */
    public void removeMarker(int id) {
        int idx = findMarkerIndex(id);
        if (idx < 0) return;
        // move last into idx
        int last = markerCount - 1;
        markerIds[idx] = markerIds[last];
        markerStarts[idx] = markerStarts[last];
        markerLengths[idx] = markerLengths[last];
        markerCount--;
    }

    /**
     * Retrieve marker; throws if not found.
     */
    public Marker getMarker(int id) {
        int idx = findMarkerIndex(id);
        if (idx < 0) throw new NoSuchElementException("Marker not found: " + id);
        return new Marker(id, markerStarts[idx], markerLengths[idx]);
    }

    /**
     * All markers snapshot.
     */
    public Marker[] getMarkers() {
        Marker[] arr = new Marker[markerCount];
        for (int i = 0; i < markerCount; i++) {
            arr[i] = new Marker(markerIds[i], markerStarts[i], markerLengths[i]);
        }
        return arr;
    }

    private int findMarkerIndex(int id) {
        for (int i = 0; i < markerCount; i++) {
            if (markerIds[i] == id) return i;
        }
        return -1;
    }

    @Override
    public void insert(int index, float value) {
        super.insert(index, value);
        // shift all markers quickly
        for (int i = 0; i < markerCount; i++) {
            int s = markerStarts[i];
            int len = markerLengths[i];
            if (s >= index) {
                markerStarts[i] = s + 1;
            } else if (s + len > index) {
                markerLengths[i] = len + 1;
            }
        }
    }

    @Override
    public void remove(int index) {
        super.remove(index);
        // shift markers
        for (int i = 0; i < markerCount; i++) {
            int s = markerStarts[i];
            int len = markerLengths[i];
            if (s > index) {
                // marker entirely after removed element
                markerStarts[i] = s - 1;
            } else if (s + len > index) {
                // removal inside marker
                markerLengths[i] = len - 1;
            }
        }
        // optionally drop zero-length markers
        int w = 0;
        for (int i = 0; i < markerCount; i++) {
            if (markerLengths[i] > 0) {
                markerIds[w] = markerIds[i];
                markerStarts[w] = markerStarts[i];
                markerLengths[w] = markerLengths[i];
                w++;
            }
        }
        markerCount = w;
    }

    @Override
    public int update(int startIndex, int endIndex, float[] newData) {
        int oldLen = endIndex - startIndex;
        int newLen = newData.length;
        int result = super.update(startIndex, endIndex, newData);
        int delta = newLen - oldLen;
        if (delta != 0) {
            // markers after endIndex shift
            for (int i = 0; i < markerCount; i++) {
                int s = markerStarts[i];
                int len = markerLengths[i];
                if (s >= endIndex) {
                    markerStarts[i] = s + delta;
                } else if (s + len > startIndex && s < startIndex) {
                    // marker overlaps update range: adjust length
                    markerLengths[i] = len + delta;
                }
            }
        }
        return result;
    }
}