package de.verdox.voxel.client.level.mesh.chunk.calculation;

import com.badlogic.gdx.graphics.GL20;
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
        return new MeshWithBounds(modelInstance, bb);
    }
}
