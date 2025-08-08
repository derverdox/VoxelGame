package de.verdox.voxel.client.renderer.classic;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.level.mesh.terrain.TerrainRegion;
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
    @Deprecated
    @Getter
    private MeshWithBounds calculatedMeshFromBuffer;

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

    private TerrainMeshService.TerrainMeshBuffer linkedBuffer;


    public int getAmountOfBlockFaces() {
        return this.amountBlockFaces;
    }

    @Deprecated
    public void setRawBlockFaces(TerrainFaceStorage blockFaceStorage, boolean complete, int lodLevel) {
        this.complete = complete;
        this.lodLevel = lodLevel;
        this.prepareMesh(blockFaceStorage, TextureAtlasManager.getInstance().getBlockTextureAtlas());
        this.dirty = true;
    }

    public void setMeshData(float[] vertices, int[] indices, int amountFaces, int numVertices, int numIndices) {
        lock.writeLock().lock();
        try {
            this.vertices = vertices;
            this.intIndices = indices;

            this.amountBlockFaces = amountFaces;
            this.numVertices = numVertices;
            this.numIndices = numIndices;
        } finally {
            this.dirty = true;
            lock.writeLock().unlock();
        }
    }

    @Deprecated
    public void linkBuffer(TerrainMeshService.TerrainMeshBuffer meshBuffer) {
        this.linkedBuffer = meshBuffer;
    }

    @Deprecated
    public void gpuUpload(ClientWorld world, TerrainRegion terrainRegion) {
        if (linkedBuffer != null) {
            IntMesh mesh = linkedBuffer.getOrCreateMesh();
            if (mesh == null) {
                return;
            }

            lock.writeLock().lock();
            try {
                int minChunkX = world.getTerrainManager().getBounds().getMinChunkX(terrainRegion.getRegionX());
                int minChunkY = world.getTerrainManager().getBounds().getMinChunkY(terrainRegion.getRegionY());
                int minChunkZ = world.getTerrainManager().getBounds().getMinChunkZ(terrainRegion.getRegionZ());

                calculatedMeshFromBuffer = new MeshWithBounds.IntRawMeshBased(mesh, Shaders.SINGLE_OPAQUE_BLOCK_SHADER, TextureAtlasManager
                        .getInstance().getBlockTextureAtlas(), 0);
                calculatedMeshFromBuffer.setPos(world.getChunkSizeX() * minChunkX, world.getChunkSizeY() * minChunkY, world.getChunkSizeZ() * minChunkZ);
            } finally {
                lock.writeLock().unlock();
                dirty = false;
            }
        }
    }

    @Deprecated
    private void prepareMesh(TerrainFaceStorage blockFaces, TextureAtlas textureAtlas) {
        if (blockFaces.getSize() == 0) {
            return;
        }
        float[] floats = new float[blockFaces.getAmountFloats()];
        int[] intIndices = new int[blockFaces.getAmountIndices()];


        AtomicInteger vertexOffset = new AtomicInteger();
        AtomicInteger indexOffset = new AtomicInteger();
        AtomicInteger baseVertex = new AtomicInteger();
        AtomicInteger amountVertices = new AtomicInteger();
        blockFaces.forEachChunkFace((storage, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks) ->
                storage.forEachFace((face, _, _, _) -> {
                    face.appendToBuffers(floats, null, intIndices, vertexOffset.get(), indexOffset.get(), baseVertex.get(), textureAtlas, (byte) lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

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

        //gpuUpload(world, terrainRegion);

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
                        Shaders.SINGLE_OPAQUE_ATTRIBUTES_ARRAY
                );

                mesh.setVertices(vertices);
                mesh.setIndices(intIndices);

                calculatedMesh = new MeshWithBounds.IntRawMeshBased(mesh, Shaders.SINGLE_OPAQUE_BLOCK_SHADER, TextureAtlasManager
                        .getInstance().getBlockTextureAtlas(), 0);
                calculatedMesh.setPos(world.getChunkSizeX() * minChunkX, world.getChunkSizeY() * minChunkY, world.getChunkSizeZ() * minChunkZ);
                return calculatedMesh;
            } else if (shortIndices != null) {
                Mesh mesh = new Mesh(
                        true,
                        numVertices,
                        numIndices,
                        Shaders.SINGLE_OPAQUE_ATTRIBUTES_ARRAY
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
