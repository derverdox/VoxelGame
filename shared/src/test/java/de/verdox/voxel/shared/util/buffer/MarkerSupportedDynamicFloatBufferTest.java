package de.verdox.voxel.shared.util.buffer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MarkerSupportedDynamicFloatBufferTest {
    private MarkerSupportedDynamicFloatBuffer buffer;

    @BeforeEach
    public void setUp() {
        // initial capacity 10, exact=false (doubling strategy)
        buffer = new MarkerSupportedDynamicFloatBuffer(10, false);
        // fill buffer with sample data (size = 20)
        for (int i = 0; i < 20; i++) {
            buffer.append(i);
        }
    }

    @Test
    public void testAddAndGetMarker() {
        buffer.addMarker(1, 5, 4);  // marker id=1 at [5,9)
        MarkerSupportedDynamicFloatBuffer.Marker m = buffer.getMarker(1);
        assertEquals(1, m.getId());
        assertEquals(5, m.getStartIndex());
        assertEquals(4, m.getLength());

        MarkerSupportedDynamicFloatBuffer.Marker[] markers = buffer.getMarkers();
        assertEquals(1, markers.length);
        assertEquals(1, markers[0].getId());
    }

    @Test
    public void testDuplicateMarkerIdThrows() {
        buffer.addMarker(2, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> buffer.addMarker(2, 4, 2));
    }

    @Test
    public void testRemoveMarkerById() {
        buffer.addMarker(3, 1, 2);
        buffer.addMarker(4, 10, 5);
        buffer.removeMarker(3);
        MarkerSupportedDynamicFloatBuffer.Marker[] markers = buffer.getMarkers();
        assertEquals(1, markers.length);
        assertEquals(4, markers[0].getId());
        // Removing non-existent id should not fail
        buffer.removeMarker(99);
    }

    @Test
    public void testInsertShiftsMarkerStart() {
        buffer.addMarker(5, 8, 3); // [8,11)
        buffer.insert(4, 99f);
        MarkerSupportedDynamicFloatBuffer.Marker m = buffer.getMarker(5);
        // start >=4 should shift by +1
        assertEquals(9, m.getStartIndex());
        assertEquals(3, m.getLength());
    }

    @Test
    public void testInsertInsideMarkerIncreasesLength() {
        buffer.addMarker(6, 5, 4); // covers indices 5,6,7,8
        buffer.insert(7, 100f);
        MarkerSupportedDynamicFloatBuffer.Marker m = buffer.getMarker(6);
        // since insert inside marker, length increases by 1
        assertEquals(5, m.getLength());
        assertEquals(5, m.getStartIndex());
    }

    @Test
    public void testRemoveShiftsAndShrinksMarker() {
        buffer.addMarker(7, 3, 5); // [3,8)
        // remove at 2 shifts start down
        buffer.remove(2);
        MarkerSupportedDynamicFloatBuffer.Marker m1 = buffer.getMarker(7);
        assertEquals(2, m1.getStartIndex());
        assertEquals(5, m1.getLength());

        // remove inside marker reduces length
        buffer.remove(4); // original index 4 now inside marker span
        MarkerSupportedDynamicFloatBuffer.Marker m2 = buffer.getMarker(7);
        assertEquals(2, m2.getStartIndex());
        assertEquals(4, m2.getLength());
    }

    @Test
    public void testRemoveInsideMarkerRemovesIfEmpty() {
        buffer.addMarker(8, 1, 1); // single element marker
        buffer.remove(1);
        // marker length becomes zero, should be removed
        assertThrows(NoSuchElementException.class, () -> buffer.getMarker(8));
        assertEquals(0, buffer.getMarkers().length);
    }

    @Test
    public void testUpdateShiftsPostMarkers() {
        buffer.addMarker(9, 0, 2);
        buffer.addMarker(10, 5, 3);
        buffer.addMarker(11, 15, 2);
        // update range [2,5) (length 3) replaced by length 5: delta=+2
        float[] newData = new float[] {1,2,3,4,5};
        buffer.update(2, 5, newData);
        // marker 9 (start 0) overlaps start<2 so unchanged
        MarkerSupportedDynamicFloatBuffer.Marker m9 = buffer.getMarker(9);
        assertEquals(0, m9.getStartIndex());
        assertEquals(2, m9.getLength());
        // marker10 start==5 shifts to 7
        MarkerSupportedDynamicFloatBuffer.Marker m10 = buffer.getMarker(10);
        assertEquals(7, m10.getStartIndex());
        assertEquals(3, m10.getLength());
        // marker11 start==15 shifts to 17
        MarkerSupportedDynamicFloatBuffer.Marker m11 = buffer.getMarker(11);
        assertEquals(17, m11.getStartIndex());
        assertEquals(2, m11.getLength());
    }
}
