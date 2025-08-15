package de.verdox.voxel.client.renderer.terrain.regions;

import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.renderer.mesh.MeshWithBounds;
import de.verdox.voxel.client.renderer.shader.Shaders;
import de.verdox.voxel.client.util.InstancedTerrainMesh;
import de.verdox.voxel.shared.util.TerrainRenderStats;
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
    private int numIntIndices;
    private float[] vertices;
    private int[] intIndices;

    private float[] instances;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int amountInstanceVertices;


    public int getAmountOfBlockFaces() {
        return this.amountBlockFaces;
    }

    public void count(TerrainRenderStats stats) {
        if (this.vertices != null) {
            stats.amountFloatVerticesVRAM += vertices.length;
        }
        if (this.instances != null) {
            stats.amountFloatVerticesVRAM += instances.length;
        }
        if (this.intIndices != null) {
            stats.amountIntIndicesVRAM += intIndices.length;
        }
        stats.amountVerticesDrawn += numVertices;
        stats.amountFacesDrawn += amountBlockFaces;
        if(amountBlockFaces > 0 && !dirty) {
            stats.drawnMeshes += 1;
        }
    }

    public void setMeshData(float[] vertices, int[] indices, int amountFaces, int numVertices, int numIndices, int lodLevel) {
        lock.writeLock().lock();
        try {
            this.vertices = vertices;
            this.intIndices = indices;

            this.amountBlockFaces = amountFaces;
            this.numVertices = numVertices;
            this.numIntIndices = numIndices;
        } finally {
            this.dirty = true;
            this.lodLevel = lodLevel;
            lock.writeLock().unlock();
        }
    }

    public void setInstances(float[] instances, int amountFaces, int amountVertices, int lodLevel) {
        lock.writeLock().lock();
        try {
            this.instances = instances;
            this.amountBlockFaces = amountFaces;
            this.amountInstanceVertices = amountVertices;
        } finally {
            this.dirty = true;
            this.lodLevel = lodLevel;
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
                numIntIndices = 0;

                vertices = null;
                intIndices = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MeshWithBounds getOrGenerateMeshFromFaces(ClientWorld world, int offsetX, int offsetY, int offsetZ) {
        if (calculatedMesh != null && !dirty) {
            return calculatedMesh;
        }
        dirty = false;
        if (amountBlockFaces == 0) {
            return null;
        }

        lock.writeLock().lock();
        try {

            if (this.calculatedMesh != null) {
                this.calculatedMesh.dispose();
            }

            if (instances != null) {
                InstancedTerrainMesh instancedTerrainMesh = new InstancedTerrainMesh(
                        amountInstanceVertices,
                        Shaders.SINGLE_OPAQUE_ATTRIBUTES_ARRAY
                );

                instancedTerrainMesh.setInstances(instances);
                this.calculatedMesh = new MeshWithBounds.InstancedBasedMesh(instancedTerrainMesh, Shaders.SINGLE_INSTANCED_OPAQUE_BLOCK_SHADER, TextureAtlasManager.getInstance().getBlockTextureAtlas(), 0);
            } else if (vertices != null && intIndices != null && numVertices > 0 && numIntIndices > 0) {
                IntMesh mesh = new IntMesh(
                        true,
                        numVertices,
                        numIntIndices,
                        Shaders.SINGLE_OPAQUE_ATTRIBUTES_ARRAY
                );

                mesh.setVertices(vertices);
                mesh.setIndices(intIndices);

                calculatedMesh = new MeshWithBounds.IntRawMeshBased(mesh, Shaders.SINGLE_PER_CORNER_OPAQUE_BLOCK_SHADER, TextureAtlasManager.getInstance().getBlockTextureAtlas(), 0);
                calculatedMesh.setPos(offsetX, offsetY, offsetZ);
            } else {
                return null;
            }

            this.calculatedMesh.setPos(offsetX, offsetY, offsetZ);
            return calculatedMesh;
        } finally {
/*            vertices = null;
            intIndices = null;*/
            lock.writeLock().unlock();
        }
    }
}
