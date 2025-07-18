package de.verdox.voxel.shared.util.datastructure;

public class LongQueue {
    private long[] elements;
    private int head;
    private int tail;
    private int size;

    private static final int DEFAULT_CAPACITY = 16;

    public LongQueue() {
        this(DEFAULT_CAPACITY);
    }

    public LongQueue(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        elements = new long[initialCapacity];
        head = 0;
        tail = 0;
        size = 0;
    }

    /**
     * Adds a value to the end of the queue.
     */
    public void enqueue(long value) {
        ensureCapacity(size + 1);
        elements[tail] = value;
        tail = (tail + 1) % elements.length;
        size++;
    }

    /**
     * Removes and returns the value at the front of the queue.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    public long dequeue() {
        if (size == 0) {
            throw new java.util.NoSuchElementException("Queue is empty");
        }
        long value = elements[head];
        head = (head + 1) % elements.length;
        size--;
        return value;
    }

    /**
     * Returns the value at the front without removing it.
     * @throws java.util.NoSuchElementException if the queue is empty.
     */
    public long peek() {
        if (size == 0) {
            throw new java.util.NoSuchElementException("Queue is empty");
        }
        return elements[head];
    }

    /**
     * @return the number of elements in the queue.
     */
    public int size() {
        return size;
    }

    /**
     * @return true if the queue contains no elements.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes all elements from the queue.
     */
    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    /**
     * Ensures the internal array can hold at least the given capacity.
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= elements.length) {
            return;
        }
        int newCapacity = elements.length << 1; // double size
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        long[] newArray = new long[newCapacity];
        // copy elements to new array
        for (int i = 0; i < size; i++) {
            newArray[i] = elements[(head + i) % elements.length];
        }
        elements = newArray;
        head = 0;
        tail = size;
    }
}
