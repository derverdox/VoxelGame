package de.verdox.voxel.client.level.chunk.proto;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import it.unimi.dsi.fastutil.floats.FloatArrays;

public abstract class ProtoMask {
    public abstract byte getMaskId();

    public abstract ChunkProtoMesh.FaceData get(ChunkProtoMesh chunkProtoMesh, Type faceType, int index);

    public abstract int getFloatsPerVertex();

    public abstract int getVerticesPerFace();

    public abstract int getIndicesPerFace();

    public float[] createArrayForVertices(int amountFaces) {
        return new float[amountFaces * getVerticesPerFace() * getFloatsPerVertex()];
    }

    public int[] createArrayForIndices(int amountFaces) {
        return new int[amountFaces * getIndicesPerFace()];
    }

    public abstract void appendToArrays(
            ChunkProtoMesh chunkProtoMesh,
            Type faceType,
            float[] vertices,
            int[] intIndices,
            int vertexOffsetFloats,
            int indexOffset,
            int baseVertexIndex,
            TextureAtlas textureAtlas,
            byte lodLevel,
            int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks
    );

    public abstract void appendToBuffers(
            ChunkProtoMesh chunkProtoMesh,
            Type faceType,
            FloatArray vertices,
            IntArray indices,
            int baseVertexIndex,
            TextureAtlas textureAtlas,
            byte lodLevel,
            int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks
    );

    public enum Type {
        OPAQUE,
        TRANSPARENT
    }

}
