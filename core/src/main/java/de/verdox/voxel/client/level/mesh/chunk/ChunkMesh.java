package de.verdox.voxel.client.level.mesh.chunk;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import lombok.Getter;

@Getter
@Deprecated
public class ChunkMesh {
    private final ChunkMeshCalculator chunkMeshCalculator;
    private BlockFaceStorage blockFaceStorage;
    private MeshWithBounds calculatedMesh;
    private boolean dirty;

    public ChunkMesh(ChunkMeshCalculator chunkMeshCalculator) {
        this.chunkMeshCalculator = chunkMeshCalculator;
    }

    public int getAmountOfBlockFaces() {
        return blockFaceStorage == null ? 0 : blockFaceStorage.size();
    }

    public void setRawBlockFaces(BlockFaceStorage blockFaceStorage, boolean complete) {
        this.blockFaceStorage = blockFaceStorage;
        dirty = true;
    }

    public boolean isComplete() {
        return true;
    }

    public void setDirty(boolean value) {
        this.dirty = value;
    }

    public MeshWithBounds getOrGenerateMeshFromFaces(TextureAtlas textureAtlas, ClientWorld world, int chunkX, int chunkY, int chunkZ) {
        if (calculatedMesh != null && !dirty) {
            return calculatedMesh;
        }
        if (this.blockFaceStorage == null) {
            return null;
        }
        dirty = false;
        if (this.blockFaceStorage.getSize() == 0) {
            return null;
        }
        calculatedMesh = ChunkMeshCalculator.buildChunkMesh(blockFaceStorage, textureAtlas);
        calculatedMesh.setPos(world.getChunkSizeX() * chunkX, world.getChunkSizeY() * chunkY, world.getChunkSizeZ() * chunkZ);
        return calculatedMesh;
    }
}
