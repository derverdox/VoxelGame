package de.verdox.voxel.client.level.mesh.chunk.calculation;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.block.BlockFace;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.lighting.LightAccessor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public interface ChunkMeshCalculator {
    long ATTRIBUTES = VertexAttributes.Usage.Position
        | VertexAttributes.Usage.Normal
        | VertexAttributes.Usage.TextureCoordinates;

    BlockFaceStorage calculateChunkMesh(ClientChunk chunk);

    static MeshWithBounds buildChunkMesh(BlockFaceStorage blockFaceStorage, TextureAtlas textureAtlas) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();

        MeshPartBuilder opaqueBuilder = mb.part(
            "chunk_opaque",
            GL20.GL_TRIANGLES,
            ATTRIBUTES,
            new Material(TextureAttribute.createDiffuse(textureAtlas.getTextures().first()))
        );
        //
        MeshPartBuilder transBuilder = mb.part(
            "chunk_transparent",
            GL20.GL_TRIANGLES,
            ATTRIBUTES,
            new Material(TextureAttribute.createDiffuse(textureAtlas.getTextures().first()))
        );

        BoundingBox bb = new BoundingBox();

        for (BlockFace rawBlockFace : blockFaceStorage) {
            rawBlockFace.addToBuilder(opaqueBuilder, TextureAtlasManager.getInstance().getBlockTextureAtlas());

            bb.ext(rawBlockFace.corner1X, rawBlockFace.corner1Y, rawBlockFace.corner1Z);
            bb.ext(rawBlockFace.corner2X, rawBlockFace.corner2Y, rawBlockFace.corner2Z);
            bb.ext(rawBlockFace.corner3X, rawBlockFace.corner3Y, rawBlockFace.corner3Z);
            bb.ext(rawBlockFace.corner4X, rawBlockFace.corner4Y, rawBlockFace.corner4Z);
        }

        Model model = mb.end();
        ModelInstance modelInstance = new ModelInstance(model);
        return new MeshWithBounds.ModelInstanceBased(modelInstance, bb);
    }

    static MeshWithBounds buildRawMesh(BlockFaceStorage blockFaceStorage, TextureAtlas textureAtlas, LightAccessor lightAccessor) {
        final int floatsPerVertex = 14;
        final int verticesPerFace = 4;
        final int indicesPerFace = 6;

        int estimatedFaces = blockFaceStorage.size();
        float[] vertices = new float[estimatedFaces * verticesPerFace * floatsPerVertex];
        short[] indices = new short[estimatedFaces * indicesPerFace];

        int vertexOffset = 0;
        int indexOffset = 0;
        short baseVertex = 0;

        for (BlockFace face : blockFaceStorage) {

            face.appendToBuffers(vertices, indices, vertexOffset, indexOffset, baseVertex, textureAtlas, floatsPerVertex, lightAccessor);
            vertexOffset += verticesPerFace * floatsPerVertex;
            indexOffset += indicesPerFace;
            baseVertex += verticesPerFace;
        }

        int numVertices = vertexOffset / floatsPerVertex;
        int numIndices = indexOffset;

        Mesh mesh = new Mesh(
            true,
            numVertices,
            numIndices,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_greedy_start"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_greedy_end"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_light"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_ambient_occlusion")
        );

        // Kritisch: Hier darf NUR numVertices * floatsPerVertex rein!
        mesh.setVertices(vertices, 0, numVertices * floatsPerVertex);
        mesh.setIndices(indices, 0, numIndices);

        return new MeshWithBounds.RawMeshBased(mesh, Shaders.OPAQUE_BLOCK_SHADER, TextureAtlasManager.getInstance().getBlockTextureAtlas(), 0);
    }

}
