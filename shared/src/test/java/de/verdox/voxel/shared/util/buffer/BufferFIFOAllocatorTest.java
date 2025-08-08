package de.verdox.voxel.shared.util.buffer;

import de.verdox.voxel.shared.util.BufferFIFOAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.NavigableSet;

import static org.junit.jupiter.api.Assertions.*;

public class BufferFIFOAllocatorTest {
    private BufferFIFOAllocator allocator;
    private final int SIZE = 1000;

    @BeforeEach
    void setUp() {
        allocator = new BufferFIFOAllocator(SIZE);
    }

    @Test
    void testInitialFreeList() {
        NavigableSet<BufferFIFOAllocator.BufferSegment> free = allocator.getFreeBufferSegments();
        assertEquals(1, free.size());
        BufferFIFOAllocator.BufferSegment seg = free.first();
        assertEquals(0, seg.getOffset());
        assertEquals(SIZE, seg.getSize());
    }

    @Test
    void testAllocateExactFit() {
        long off = allocator.allocate(SIZE);
        assertEquals(0, off);
        assertTrue(allocator.getFreeBufferSegments().isEmpty());
    }

    @Test
    void testAllocateSplit() {
        long off = allocator.allocate(100);
        assertEquals(0, off);
        NavigableSet<BufferFIFOAllocator.BufferSegment> free = allocator.getFreeBufferSegments();
        assertEquals(1, free.size());
        BufferFIFOAllocator.BufferSegment seg = free.first();
        assertEquals(100, seg.getOffset());
        assertEquals(SIZE - 100, seg.getSize());
    }

    @Test
    void testFreeAndMergeLower() {
        long off1 = allocator.allocate(100);
        long off2 = allocator.allocate(200);
        allocator.free(off1, 100);
        allocator.free(off2, 200);
        NavigableSet<BufferFIFOAllocator.BufferSegment> free = allocator.getFreeBufferSegments();
        assertEquals(1, free.size());
        BufferFIFOAllocator.BufferSegment seg = free.first();
        assertEquals(0, seg.getOffset());
        assertEquals(SIZE, seg.getSize());
    }

    @Test
    void testFreeMergeAdjacentSegments() {
        long a = allocator.allocate(100);
        long b = allocator.allocate(200);
        long c = allocator.allocate(300);
        allocator.free(b, 200);
        allocator.free(a, 100);
        allocator.free(c, 300);
        NavigableSet<BufferFIFOAllocator.BufferSegment> free = allocator.getFreeBufferSegments();
        assertEquals(1, free.size());
        BufferFIFOAllocator.BufferSegment seg = free.first();
        assertEquals(0, seg.getOffset());
        assertEquals(SIZE, seg.getSize());
    }

    @Test
    void testFreeNonAdjacentCreatesSeparateSegment() {
        long a = allocator.allocate(100);
        long b = allocator.allocate(200);
        allocator.free(a, 100);
        NavigableSet<BufferFIFOAllocator.BufferSegment> free = allocator.getFreeBufferSegments();
        assertEquals(2, free.size());
        BufferFIFOAllocator.BufferSegment first = free.pollFirst();
        BufferFIFOAllocator.BufferSegment second = free.pollFirst();
        assertEquals(0, first.getOffset());
        assertEquals(100, first.getSize());
        assertEquals(300, second.getOffset());
        assertEquals(SIZE - 300, second.getSize());
    }

    @Test
    void testAllocateMultipleSegments() {
        long a = allocator.allocate(400);
        long b = allocator.allocate(300);
        long c = allocator.allocate(200);
        assertEquals(0, a);
        assertEquals(400, b);
        assertEquals(700, c);
        assertThrows(OutOfMemoryError.class, () -> allocator.allocate(200));
    }
}
