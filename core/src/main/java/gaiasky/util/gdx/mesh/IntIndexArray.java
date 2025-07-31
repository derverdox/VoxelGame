package gaiasky.util.gdx.mesh;


import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntIndexArray implements IntIndexData {
    final IntBuffer buffer;
    final ByteBuffer byteBuffer;

    // used to work around bug: https://android-review.googlesource.com/#/c/73175/
    private final boolean empty;

    /**
     * Creates a new IntIndexArray to be used with vertex arrays.
     *
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    public IntIndexArray(int maxIndices) {

        empty = maxIndices == 0;
        if (empty) {
            maxIndices = 1; // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0
        }

        byteBuffer = BufferUtils.newUnsafeByteBuffer(maxIndices * 4);
        buffer = byteBuffer.asIntBuffer();
        buffer.flip();
        byteBuffer.flip();
    }

    /** @return the number of indices currently stored in this buffer */
    public int getNumIndices() {
        return empty ? 0 : buffer.limit();
    }

    /** @return the maximum number of indices this IntIndexArray can store. */
    public int getNumMaxIndices() {
        return empty ? 0 : buffer.capacity();
    }

    /**
     * <p>
     * Sets the indices of this IntIndexArray, discarding the old indices. The count must equal the number of indices to be copied to
     * this IntIndexArray.
     * </p>
     *
     * <p>
     * This can be called in between calls to {@link #bind()} and {@link #unbind()}. The index data will be updated instantly.
     * </p>
     *
     * @param indices the vertex data
     * @param offset  the offset to start copying the data from
     * @param count   the number of ints to copy
     */
    public void setIndices(int[] indices, int offset, int count) {
        buffer.clear();
        buffer.put(indices, offset, count);
        buffer.flip();
        byteBuffer.position(0);
        byteBuffer.limit(count << 2);
    }

    public void setIndices(IntBuffer indices) {
        int pos = indices.position();
        buffer.clear();
        buffer.limit(indices.remaining());
        buffer.put(indices);
        buffer.flip();
        indices.position(pos);
        byteBuffer.position(0);
        byteBuffer.limit(buffer.limit() << 2);
    }

    @Override
    public void updateIndices(int targetOffset, int[] indices, int offset, int count) {
        final int pos = byteBuffer.position();
        byteBuffer.position(targetOffset * 4);
        BufferUtils.copy(indices, offset, byteBuffer, count);
        byteBuffer.position(pos);
    }

    /**
     * <p>
     * Returns the underlying IntBuffer. If you modify the buffer contents they will be uploaded on the call to {@link #bind()}.
     * If you need immediate uploading use {@link #setIndices(int[], int, int)}.
     * </p>
     *
     * @return the underlying int buffer.
     */
    public IntBuffer getBuffer() {
        return buffer;
    }

    /** Binds this IntIndexArray for rendering with glDrawElements. */
    public void bind() {
    }

    /** Unbinds this IntIndexArray. */
    public void unbind() {
    }

    /** Invalidates the IntIndexArray so a new OpenGL buffer handle is created. Use this in case of a context loss. */
    public void invalidate() {
    }

    /** Disposes this IntIndexArray and all its associated OpenGL resources. */
    public void dispose() {
        BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
    }
}
