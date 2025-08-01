package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.shader.Shaders;
import gaiasky.util.gdx.mesh.IntMesh;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;
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
    @Getter
    private int lodLevel = 0;

    private int amountBlockFaces;
    private int numVertices;
    private int numIndices;
    private float[] vertices;
    private short[] shortIndices;
    private int[] intIndices;
    private int verticesCount;
    private int indicesCount;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public int getAmountOfBlockFaces() {
        return this.amountBlockFaces;
    }

    public void setRawBlockFaces(TerrainFaceStorage blockFaceStorage, boolean complete, int lodLevel) {
        this.complete = complete;
        this.lodLevel = lodLevel;
        this.prepareMesh(blockFaceStorage, TextureAtlasManager.getInstance().getBlockTextureAtlas());
        this.dirty = true;
    }

    private void prepareMesh(TerrainFaceStorage blockFaces, TextureAtlas textureAtlas) {
        if (blockFaces.getSize() == 0) {
            return;
        }

        float[] floats = new float[blockFaces.getAmountFloats()];
        short[] shortIndices;
        int[] intIndices;

        if (blockFaces.getAmountIndices() > Short.MAX_VALUE) {
            intIndices = new int[blockFaces.getAmountIndices()];
            shortIndices = null;
        } else {
            shortIndices = new short[blockFaces.getAmountIndices()];
            intIndices = null;
        }


        AtomicInteger vertexOffset = new AtomicInteger();
        AtomicInteger indexOffset = new AtomicInteger();
        AtomicInteger baseVertex = new AtomicInteger();
        AtomicInteger amountVertices = new AtomicInteger();

        blockFaces.forEachChunkFace((storage, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks) -> storage.forEachFace(face -> {
            face.appendToBuffers(floats, shortIndices, intIndices, vertexOffset.get(), indexOffset.get(), baseVertex.get(), textureAtlas, face.getFloatsPerVertex(), offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

            vertexOffset.addAndGet(face.getVerticesPerFace() * face.getFloatsPerVertex());
            indexOffset.addAndGet(face.getIndicesPerFace());
            baseVertex.addAndGet(face.getVerticesPerFace());
            amountVertices.addAndGet(face.getVerticesPerFace());
        }));

        lock.writeLock().lock();
        try {
            this.amountBlockFaces = blockFaces.getSize();
            numVertices = amountVertices.get();
            numIndices = indexOffset.get();

            verticesCount = vertexOffset.get();
            indicesCount = indexOffset.get();

            this.vertices = floats;
            this.shortIndices = shortIndices;
            this.intIndices = intIndices;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void dispose() {
        lock.writeLock().lock();
        try {
            if (this.calculatedMesh != null) {
                this.calculatedMesh.dispose();
                this.calculatedMesh = null;

                this.amountBlockFaces = 0;
                numVertices = 0;
                numIndices = 0;

                verticesCount = 0;
                indicesCount = 0;

                vertices = null;
                shortIndices = null;
                intIndices = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MeshWithBounds getOrGenerateMeshFromFaces(ClientWorld world, TerrainRegion terrainRegion) {
        if (calculatedMesh != null && !dirty) {
            return calculatedMesh;
        }
        dirty = false;
        if (amountBlockFaces == 0 || vertices == null || (shortIndices == null && intIndices == null)) {
            return null;
        }
        int minChunkX = world.getTerrainManager().getBounds().getMinChunkX(terrainRegion.getRegionX());
        int minChunkY = world.getTerrainManager().getBounds().getMinChunkY(terrainRegion.getRegionY());
        int minChunkZ = world.getTerrainManager().getBounds().getMinChunkZ(terrainRegion.getRegionZ());

        lock.writeLock().lock();
        try {

            if (this.calculatedMesh != null) {
                this.calculatedMesh.dispose();
            }

            if (intIndices != null) {
                IntMesh mesh = new IntMesh(
                        true,
                        numVertices,
                        numIndices,
                        Shaders.SINGLE_OPAQUE_ATTRIBUTES
                );

                mesh.setVertices(vertices, 0, verticesCount);
                mesh.setIndices(intIndices, 0, indicesCount);

                calculatedMesh = new MeshWithBounds.IntRawMeshBased(mesh, Shaders.SINGLE_OPAQUE_BLOCK_SHADER, TextureAtlasManager
                        .getInstance().getBlockTextureAtlas(), 0);
                calculatedMesh.setPos(world.getChunkSizeX() * minChunkX, world.getChunkSizeY() * minChunkY, world.getChunkSizeZ() * minChunkZ);
                return calculatedMesh;
            } else if (shortIndices != null) {
                Mesh mesh = new Mesh(
                        true,
                        numVertices,
                        numIndices,
                        Shaders.SINGLE_OPAQUE_ATTRIBUTES
                );

                mesh.setVertices(vertices, 0, verticesCount);
                mesh.setIndices(shortIndices, 0, indicesCount);

                calculatedMesh = new MeshWithBounds.ShortRawMeshBased(mesh, Shaders.SINGLE_OPAQUE_BLOCK_SHADER, TextureAtlasManager
                        .getInstance().getBlockTextureAtlas(), 0);
                calculatedMesh.setPos(world.getChunkSizeX() * minChunkX, world.getChunkSizeY() * minChunkY, world.getChunkSizeZ() * minChunkZ);
                return calculatedMesh;
            } else {
                throw new IllegalStateException("Could neither build int mesh nor short mesh");
            }
        } finally {
            vertices = null;
            shortIndices = null;
            intIndices = null;
            lock.writeLock().unlock();
        }
    }
}
