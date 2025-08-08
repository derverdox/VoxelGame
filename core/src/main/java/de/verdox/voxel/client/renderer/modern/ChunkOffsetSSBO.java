package de.verdox.voxel.client.renderer.modern;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.utils.Disposable;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Manages a Shader Storage Buffer Object (SSBO) for chunk-origin offsets
 * and an Indirect Draw buffer for DrawElementsIndirectCommand structures.
 */
public class ChunkOffsetSSBO implements Disposable {
    // std430 layout: vec3 (12 bytes) padded to 16 bytes
    private static final int ENTRY_SIZE = 16;
    // DrawElementsIndirectCommand: 5 ints = 20 bytes
    public static final int COMMAND_SIZE = Integer.BYTES * 5;

    private final int maxChunks;
    private final int ssboHandle;
    /**
     * -- GETTER --
     *
     * @return the GL handle for the indirect draw buffer
     */
    @Getter
    private final int indirectHandle;
    private int nextIndex = 0;

    private final ByteBuffer tempBuffer;

    /**
     * @param maxChunks Maximum number of chunk entries to support
     */
    public ChunkOffsetSSBO(int maxChunks) {
        this.maxChunks = maxChunks;
        ssboHandle = Gdx.gl30.glGenBuffer();
        Gdx.gl30.glBindBuffer(GL31.GL_SHADER_STORAGE_BUFFER, ssboHandle);
        Gdx.gl30.glBufferData(
                GL31.GL_SHADER_STORAGE_BUFFER,
                maxChunks * ENTRY_SIZE,
                null,
                GL30.GL_DYNAMIC_DRAW
        );
        Gdx.gl30.glBindBuffer(GL31.GL_SHADER_STORAGE_BUFFER, 0);

        indirectHandle = Gdx.gl30.glGenBuffer();
        Gdx.gl30.glBindBuffer(GL31.GL_DRAW_INDIRECT_BUFFER, indirectHandle);
        Gdx.gl30.glBufferData(
                GL31.GL_DRAW_INDIRECT_BUFFER,
                maxChunks * COMMAND_SIZE,
                null,
                GL30.GL_DYNAMIC_DRAW
        );
        Gdx.gl30.glBindBuffer(GL31.GL_DRAW_INDIRECT_BUFFER, 0);

        tempBuffer = ByteBuffer.allocateDirect(Math.max(ENTRY_SIZE, COMMAND_SIZE))
                .order(ByteOrder.nativeOrder());
    }

    /**
     * Adds a new chunk origin and corresponding indirect draw command.
     * @param originX world X offset of the chunk
     * @param originY world Y offset of the chunk
     * @param originZ world Z offset of the chunk
     * @param indexCount number of indices to draw
     * @param firstIndex starting index in the IBO
     * @param baseVertex base vertex offset in the VBO
     * @return the entry index (0-based) for future reference
     */
    public int addEntry(float originX, float originY, float originZ,
                        int indexCount, int firstIndex, int baseVertex) {
        if (nextIndex >= maxChunks) {
            throw new IllegalStateException("Exceeded max chunk entries: " + maxChunks);
        }
        int entry = nextIndex;

        // Upload chunk origin to SSBO
        Gdx.gl30.glBindBuffer(GL31.GL_SHADER_STORAGE_BUFFER, ssboHandle);
        tempBuffer.clear();
        tempBuffer.putFloat(originX)
                .putFloat(originY)
                .putFloat(originZ)
                // padding to 16 bytes
                .putFloat(0f)
                .flip();
        Gdx.gl30.glBufferSubData(
                GL31.GL_SHADER_STORAGE_BUFFER,
                entry * ENTRY_SIZE,
                ENTRY_SIZE,
                tempBuffer
        );
        Gdx.gl30.glBindBuffer(GL31.GL_SHADER_STORAGE_BUFFER, 0);

        // Prepare indirect draw command
        Gdx.gl30.glBindBuffer(GL31.GL_DRAW_INDIRECT_BUFFER, indirectHandle);
        tempBuffer.clear();
        tempBuffer.putInt(indexCount)    // count
                .putInt(1)             // instanceCount = 1
                .putInt(firstIndex)    // firstIndex
                .putInt(baseVertex)    // baseVertex
                .putInt(entry)         // baseInstance = entry (for gl_DrawID)
                .flip();
        Gdx.gl30.glBufferSubData(
                GL31.GL_DRAW_INDIRECT_BUFFER,
                entry * COMMAND_SIZE,
                COMMAND_SIZE,
                tempBuffer
        );
        Gdx.gl30.glBindBuffer(GL31.GL_DRAW_INDIRECT_BUFFER, 0);

        nextIndex++;
        return entry;
    }

    /**
     * Clears an existing entry (both SSBO and Indirect) by zeroing its bytes.
     * @param entry the 0-based index to clear
     */
    public void clearEntry(int entry) {
        // Zero SSBO entry
        Gdx.gl30.glBindBuffer(GL31.GL_SHADER_STORAGE_BUFFER, ssboHandle);
        tempBuffer.clear();
        tempBuffer.limit(ENTRY_SIZE);
        Gdx.gl30.glBufferSubData(
                GL31.GL_SHADER_STORAGE_BUFFER,
                entry * ENTRY_SIZE,
                ENTRY_SIZE,
                tempBuffer
        );
        Gdx.gl30.glBindBuffer(GL31.GL_SHADER_STORAGE_BUFFER, 0);

        // Zero Indirect command
        Gdx.gl30.glBindBuffer(GL31.GL_DRAW_INDIRECT_BUFFER, indirectHandle);
        tempBuffer.clear();
        tempBuffer.limit(COMMAND_SIZE);
        Gdx.gl30.glBufferSubData(
                GL31.GL_DRAW_INDIRECT_BUFFER,
                entry * COMMAND_SIZE,
                COMMAND_SIZE,
                tempBuffer
        );
        Gdx.gl30.glBindBuffer(GL31.GL_DRAW_INDIRECT_BUFFER, 0);
    }

    /**
     * Binds the SSBO for shader access at the given binding point.
     * @param bindingIndex the SSBO binding index to use in the shader
     */
    public void bind(int bindingIndex) {
        Gdx.gl30.glBindBufferBase(
                GL31.GL_SHADER_STORAGE_BUFFER,
                bindingIndex,
                ssboHandle
        );
    }

    @Override
    public void dispose() {
        Gdx.gl30.glDeleteBuffer(ssboHandle);
        Gdx.gl30.glDeleteBuffer(indirectHandle);
    }
}
