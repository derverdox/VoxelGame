package gaiasky.util.gdx.mesh;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class VertexBufferObjectSubData implements IntVertexData {
    final VertexAttributes attributes;
    final FloatBuffer buffer;
    final ByteBuffer byteBuffer;
    final boolean isDirect;
    final boolean isStatic;
    final int usage;
    int bufferHandle;
    boolean isDirty = false;
    boolean isBound = false;

    /**
     * Constructs a new interleaved VertexBufferObject.
     *
     * @param isStatic    whether the vertex data is static.
     * @param numVertices the maximum number of vertices
     * @param attributes  the {@link VertexAttributes}.
     */
    public VertexBufferObjectSubData(boolean isStatic, int numVertices, VertexAttribute... attributes) {
        this(isStatic, numVertices, new VertexAttributes(attributes));
    }

    /**
     * Constructs a new interleaved VertexBufferObject.
     *
     * @param isStatic    whether the vertex data is static.
     * @param numVertices the maximum number of vertices
     * @param attributes  the {@link VertexAttribute}s.
     */
    public VertexBufferObjectSubData(boolean isStatic, int numVertices, VertexAttributes attributes) {
        this.isStatic = isStatic;
        this.attributes = attributes;
        byteBuffer = BufferUtils.newByteBuffer(this.attributes.vertexSize * numVertices);
        isDirect = true;

        usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
        buffer = byteBuffer.asFloatBuffer();
        bufferHandle = createBufferObject();
        buffer.flip();
        byteBuffer.flip();
    }

    private int createBufferObject() {
        int result = Gdx.gl20.glGenBuffer();
        Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, result);
        Gdx.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.capacity(), null, usage);
        Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        return result;
    }

    @Override
    public VertexAttributes getAttributes() {
        return attributes;
    }

    @Override
    public int getNumVertices() {
        return buffer.limit() * 4 / attributes.vertexSize;
    }

    @Override
    public int getNumMaxVertices() {
        return byteBuffer.capacity() / attributes.vertexSize;
    }

    @Override
    public FloatBuffer getBuffer() {
        isDirty = true;
        return buffer;
    }

    private void bufferChanged() {
        if (isBound) {
            Gdx.gl20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
            isDirty = false;
        }
    }

    @Override
    public void setVertices(float[] vertices, int offset, int count) {
        isDirty = true;
        if (isDirect) {
            BufferUtils.copy(vertices, byteBuffer, count, offset);
            buffer.position(0);
            buffer.limit(count);
        } else {
            buffer.clear();
            buffer.put(vertices, offset, count);
            buffer.flip();
            byteBuffer.position(0);
            byteBuffer.limit(buffer.limit() << 2);
        }

        bufferChanged();
    }

    @Override
    public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
        isDirty = true;
        if (isDirect) {
            final int pos = byteBuffer.position();
            byteBuffer.position(targetOffset * 4);
            BufferUtils.copy(vertices, sourceOffset, count, byteBuffer);
            byteBuffer.position(pos);
        } else
            throw new GdxRuntimeException("Buffer must be allocated direct."); // Should never happen

        bufferChanged();
    }

    /**
     * Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
     *
     * @param shader the shader
     */
    @Override
    public void bind(final ShaderProgram shader) {
        bind(shader, null);
    }

    @Override
    public void bind(final ShaderProgram shader, final int[] locations) {
        final GL20 gl = Gdx.gl20;

        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle);
        if (isDirty) {
            byteBuffer.limit(buffer.limit() * 4);
            gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
            isDirty = false;
        }

        final int numAttributes = attributes.size();
        if (locations == null) {
            for (int i = 0; i < numAttributes; i++) {
                final VertexAttribute attribute = attributes.get(i);
                final int location = shader.getAttributeLocation(attribute.alias);
                if (location < 0)
                    continue;
                shader.enableVertexAttribute(location);

                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize,
                        attribute.offset);
            }
        } else {
            for (int i = 0; i < numAttributes; i++) {
                final VertexAttribute attribute = attributes.get(i);
                final int location = locations[i];
                if (location < 0)
                    continue;
                shader.enableVertexAttribute(location);

                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize,
                        attribute.offset);
            }
        }
        isBound = true;
    }

    /**
     * Unbinds this VertexBufferObject.
     *
     * @param shader the shader
     */
    @Override
    public void unbind(final ShaderProgram shader) {
        unbind(shader, null);
    }

    @Override
    public void unbind(final ShaderProgram shader, final int[] locations) {
        final GL20 gl = Gdx.gl20;
        final int numAttributes = attributes.size();
        if (locations == null) {
            for (int i = 0; i < numAttributes; i++) {
                shader.disableVertexAttribute(attributes.get(i).alias);
            }
        } else {
            for (int i = 0; i < numAttributes; i++) {
                final int location = locations[i];
                if (location >= 0)
                    shader.disableVertexAttribute(location);
            }
        }
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        isBound = false;
    }

    /** Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
    public void invalidate() {
        bufferHandle = createBufferObject();
        isDirty = true;
    }

    /** Disposes of all resources this VertexBufferObject uses. */
    @Override
    public void dispose() {
        GL20 gl = Gdx.gl20;
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        gl.glDeleteBuffer(bufferHandle);
        bufferHandle = 0;
    }

    /**
     * Returns the VBO handle
     *
     * @return the VBO handle
     */
    public int getBufferHandle() {
        return bufferHandle;
    }
}