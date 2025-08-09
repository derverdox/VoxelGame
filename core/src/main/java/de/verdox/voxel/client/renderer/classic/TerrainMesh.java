package de.verdox.voxel.client.renderer.classic;

import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.TerrainRegion;
import de.verdox.voxel.client.renderer.shader.Shaders;
import gaiasky.util.gdx.mesh.IntMesh;
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
    private int lodLevel = 0;

    private int amountBlockFaces;
    private int numVertices;
    private int numIndices;
    private float[] vertices;
    private int[] intIndices;
    private int verticesCount;
    private int indicesCount;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public int getAmountOfBlockFaces() {
        return this.amountBlockFaces;
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
        if (amountBlockFaces == 0 || vertices == null || intIndices == null) {
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
        } finally {
            vertices = null;
            intIndices = null;
            lock.writeLock().unlock();
        }
    }
}
