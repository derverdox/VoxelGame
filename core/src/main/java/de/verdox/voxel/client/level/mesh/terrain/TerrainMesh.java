package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.block.face.BlockFace;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.client.shader.Shaders;
import de.verdox.voxel.shared.lighting.LightAccessor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TerrainMesh {
    @Getter
    private MeshWithBounds calculatedMesh;
    @Getter
    @Setter
    private boolean dirty;
    @Getter
    private boolean complete;

    private int amountBlockFaces;
    private int numVertices;
    private int numIndices;
    private float[] vertices;
    private short[] indices;
    private int verticesCount;
    private int indicesCount;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public int getAmountOfBlockFaces() {
        return this.amountBlockFaces;
    }

    public void setRawBlockFaces(BlockFaceStorage blockFaceStorage, boolean complete, LightAccessor lightAccessor) {
        this.complete = complete;
        this.prepareMesh(blockFaceStorage, TextureAtlasManager.getInstance().getBlockTextureAtlas(), lightAccessor);
        this.dirty = true;
    }

    private void prepareMesh(BlockFaceStorage blockFaces, TextureAtlas textureAtlas, LightAccessor lightAccessor) {
        if (blockFaces.size() == 0) {
            return;
        }
        float[] vertices = new float[blockFaces.getVertices()];
        short[] indices = new short[blockFaces.getIndices()];

        int vertexOffset = 0;
        int indexOffset = 0;
        short baseVertex = 0;

        int amountVertices = 0;

        for (BlockFace face : blockFaces) {
            face.appendToBuffers(vertices, indices, vertexOffset, indexOffset, baseVertex, textureAtlas, face.getFloatsPerVertex(), lightAccessor);

            vertexOffset += face.getVerticesPerFace() * face.getFloatsPerVertex();
            indexOffset += face.getIndicesPerFace();
            baseVertex += (short) face.getVerticesPerFace();

            amountVertices += face.getVerticesPerFace();
        }

        lock.writeLock().lock();
        try {
            this.amountBlockFaces = blockFaces.size();
            numVertices = amountVertices;
            numIndices = indexOffset;

            verticesCount = vertexOffset;
            indicesCount = indexOffset;

            this.vertices = vertices;
            this.indices = indices;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MeshWithBounds getOrGenerateMeshFromFaces(TextureAtlas textureAtlas, ClientWorld world, int regionX, int regionY, int regionZ, LightAccessor lightAccessor) {
        if (calculatedMesh != null && !dirty) {
            return calculatedMesh;
        }
        dirty = false;
        if (amountBlockFaces == 0) {
            return null;
        }
        int minChunkX = world.getTerrainManager().getMeshPipeline().getRegionBounds().getMinChunkX(regionX);
        int minChunkY = world.getTerrainManager().getMeshPipeline().getRegionBounds().getMinChunkY(regionY);
        int minChunkZ = world.getTerrainManager().getMeshPipeline().getRegionBounds().getMinChunkZ(regionZ);

        lock.readLock().lock();
        try {
            Mesh mesh = new Mesh(
                    true,
                    numVertices,
                    numIndices,
                    Shaders.SINGLE_OPAQUE_ATTRIBUTES
            );

            mesh.setVertices(vertices, 0, verticesCount);
            mesh.setIndices(indices, 0, indicesCount);

            calculatedMesh = new MeshWithBounds.RawMeshBased(mesh, Shaders.SINGLE_OPAQUE_BLOCK_SHADER, TextureAtlasManager.getInstance().getBlockTextureAtlas(), 0);
            calculatedMesh.setPos(world.getChunkSizeX() * minChunkX, world.getChunkSizeY() * minChunkY, world.getChunkSizeZ() * minChunkZ);
            return calculatedMesh;
        } finally {
            lock.readLock().unlock();
        }
    }
}
