package de.verdox.voxel.client.renderer.modern;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.BufferFIFOAllocator;
import gaiasky.util.gdx.mesh.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.opengl.GL43;

public class WorldMesh implements Disposable {
/*    private final IntVertexData vertexData;
    private final IntIndexData indexData;

    private final BufferFIFOAllocator vertexAllocator;
    private final BufferFIFOAllocator indexAllocator;

    private final Long2ObjectMap<ChunkBufferRegion> chunksInBuffer = new Long2ObjectOpenHashMap<>();
    private final ClientWorld world;
    private final VertexAttributes vertexAttributes;

    public WorldMesh(ClientWorld world, long VRAMLimitInBytes, VertexAttribute... attributes) {
        this.world = world;
        vertexAttributes = new VertexAttributes(attributes);

        long vertexDataConsumptionBytes = vertexAttributes.vertexSize;
        long indexDataConsumptionBytes = Integer.BYTES;

        int amountVerticesPerFace = 4;
        int amountIndicesPerFace = 6;

        long vertexDataConsumptionPerFaceBytes = amountVerticesPerFace * vertexDataConsumptionBytes;
        long indexDataConsumptionPerFaceBytes = amountIndicesPerFace * indexDataConsumptionBytes;
        long totalConsumptionPerFace = vertexDataConsumptionPerFaceBytes + indexDataConsumptionPerFaceBytes;

        long VRAMForVertices = VRAMLimitInBytes * vertexDataConsumptionPerFaceBytes / totalConsumptionPerFace;
        long VRAMForIndices = VRAMLimitInBytes * indexDataConsumptionPerFaceBytes / totalConsumptionPerFace;

        int maxVertices = (int) (VRAMForVertices / vertexDataConsumptionBytes);
        int maxIndices = (int) (VRAMForIndices / indexDataConsumptionBytes);

        this.vertexData = makeVertexBuffer(true, maxVertices, vertexAttributes);
        this.indexData = new IntIndexBufferObject(true, maxIndices);

        this.vertexAllocator = new BufferFIFOAllocator((int) VRAMForVertices);
        this.indexAllocator = new BufferFIFOAllocator((int) VRAMForIndices);

        if (isSSBOSupported()) {

        }
    }

    public void addChunkMesh(int chunkX, int chunkY, int chunkZ, float[] vertices, int[] indices) {
        long chunkKey = Chunk.computeChunkKey(chunkX, chunkY, chunkZ);

        ChunkBufferRegion old = chunksInBuffer.remove(chunkKey);

        if(old != null) {
            vertexAllocator.free(old.vertexOffsetBytes, old.vertexSizeBytes);
            indexAllocator.free(old.indexOffsetBytes, old.indexSizeBytes);
            if(isSSBOSupported()) {
                chunkOffsetSSBO.clearEntry(old.ssboIndex);
            }
        }

        // STEP 2: allocate new segments
        int vBytes = vertices.length * Float.BYTES;
        int iBytes = indices.length  * Integer.BYTES;

        //TODO: Exception catch or other limiting feature
        long vOffset = vertexAllocator.allocate(vBytes);
        long iOffset = indexAllocator.allocate(iBytes);

        vertexData.updateVertices((int) vOffset, vertices, 0, vertices.length);
        indexData.updateIndices((int) iOffset, indices, 0, indices.length);

        if(isSSBOSupported()) {
            // STEP 3: record chunk offset in SSBO for draw
            int ssboIdx = chunkOffsetSSBO.addEntry(
                    chunkX * world.getChunkSizeX(),
                    chunkY * world.getChunkSizeY(),
                    chunkZ * world.getChunkSizeZ(),
                    indices.length,       // indexCount
                    (int)(iOffset  / Integer.BYTES),
                    (int)(vOffset  / vertexAttributes.vertexSize)
            );
        }


        // STEP 4: store region metadata
        ChunkBufferRegion region = new ChunkBufferRegion(vOffset, vBytes, iOffset, iBytes, ssboIdx);
        chunksInBuffer.put(chunkKey, region);
    }

    public void render(Camera camera, ShaderProgram shader) {
        shader.bind();
        shader.setUniformMatrix("u_projView", camera.combined);

        vertexData.bind(shader);
        indexData.bind();

        if (isSSBOSupported()) {
            chunkOffsetSSBO.bind(0);
            Gdx.gl30.glBindBuffer(GL31.GL_DRAW_INDIRECT_BUFFER, chunkOffsetSSBO.getIndirectHandle());
            GL43.glMultiDrawElementsIndirect(
                    GL20.GL_TRIANGLES,
                    GL20.GL_UNSIGNED_SHORT,
                    0,                                   // byte offset in INDIRECT_BUFFER
                    chunkOffsetSSBO.getEntryCount(),     // number of draw commands
                    ChunkOffsetSSBO.COMMAND_SIZE         // stride between commands
            );
            Gdx.gl30.glBindBuffer(GL32.GL_DRAW_INDIRECT_BUFFER, 0);

        } else {
            // 3b) Fallback‐Path: loop over regions and draw each with a single draw call
            for (ChunkRegion r : regions.values()) {
                // Set per‐region (chunk) world‐offset
                shader.setUniformf("u_chunkOrigin",
                        r.cx * CHUNK_SIZE,
                        r.cy * CHUNK_SIZE,
                        r.cz * CHUNK_SIZE);

                // Draw only the indices belonging to this chunk
                bigMesh.render(
                        shader,
                        GL20.GL_TRIANGLES,
                        r.indexOffsetBytes / Integer.BYTES,  // firstIndex
                        r.indexCount                          // count
                );
            }
        }
    }

    private IntVertexData makeVertexBuffer(boolean isStatic, int maxVertices, VertexAttributes vertexAttributes) {
        if (Gdx.gl30 != null) {
            return new VertexBufferObjectWithVAO(isStatic, maxVertices, vertexAttributes);
        } else {
            return new VertexBufferObjectSubData(isStatic, maxVertices, vertexAttributes);
        }
    }



    private boolean isSSBOSupported() {
        GLVersion version = Gdx.graphics.getGLVersion();
        boolean desktop43 = version.getMajorVersion() >= 4 && version.getMinorVersion() >= 3;
        boolean es31 = version.getMajorVersion() == 3 && version.getMinorVersion() >= 1;
        return Gdx.gl30 != null && (desktop43 || es31);
    }

    private record ChunkBufferRegion(long vertexOffsetBytes, long vertexSizeBytes, long indexOffsetBytes,
                                     long indexSizeBytes, int ssboIndex) {

    }*/

    @Override
    public void dispose() {
/*        this.vertexData.dispose();
        this.indexData.dispose();*/
    }
}
