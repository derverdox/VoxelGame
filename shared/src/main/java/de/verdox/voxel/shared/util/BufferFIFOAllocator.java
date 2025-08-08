package de.verdox.voxel.shared.util;

import lombok.Getter;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

@Getter
public class BufferFIFOAllocator {
    private final int sizeBytes;
    private final NavigableSet<BufferSegment> freeBufferSegments;

    public BufferFIFOAllocator(int sizeBytes) {
        this.sizeBytes = sizeBytes;
        this.freeBufferSegments = new TreeSet<>();
        this.freeBufferSegments.add(new BufferSegment(0, sizeBytes));
    }

    /**
     * Allocates a segment of at least the requested size.
     * @param requestSize number of bytes requested
     * @return the offset within the buffer where the segment starts
     * @throws OutOfMemoryError if no sufficient segment is available
     */
    public synchronized long allocate(long requestSize) {
        for (BufferSegment seg : freeBufferSegments) {
            if (seg.size >= requestSize) {
                long allocOffset = seg.offset;
                if (seg.size == requestSize) {
                    freeBufferSegments.remove(seg);
                } else {
                    seg.offset += requestSize;
                    seg.size -= requestSize;
                }
                return allocOffset;
            }
        }
        throw new OutOfMemoryError("Insufficient free space for allocation of size " + requestSize);
    }

    /**
     * Frees a previously allocated segment, returning it to the free list and merging if possible.
     * @param offset the starting offset of the segment
     * @param size   the size of the segment
     */
    public synchronized void free(long offset, long size) {
        BufferSegment toFree = new BufferSegment(offset, size);

        BufferSegment lower = freeBufferSegments.lower(toFree);
        BufferSegment higher = freeBufferSegments.higher(toFree);

        if (lower != null && lower.offset + lower.size == toFree.offset) {
            toFree.offset = lower.offset;
            toFree.size += lower.size;
            freeBufferSegments.remove(lower);
        }

        if (higher != null && toFree.offset + toFree.size == higher.offset) {
            toFree.size += higher.size;
            freeBufferSegments.remove(higher);
        }

        freeBufferSegments.add(toFree);
    }

    @Getter
    public static class BufferSegment implements Comparable<BufferSegment> {
        private long offset;
        private long size;

        BufferSegment(long offset, long size) {
            this.offset = offset;
            this.size = size;
        }

        @Override
        public int compareTo(BufferSegment other) {
            return Long.compare(this.offset, other.offset);
        }

        @Override
        public String toString() {
            return String.format("[%d, %d]", offset, size);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            BufferSegment bufferSegment = (BufferSegment) o;
            return offset == bufferSegment.offset && size == bufferSegment.size;
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, size);
        }
    }
    
}
