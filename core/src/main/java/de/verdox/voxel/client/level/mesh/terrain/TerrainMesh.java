package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.MeshWithBounds;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.shared.lighting.LightAccessor;
import lombok.Getter;
import lombok.Setter;

public class TerrainMesh {
    private BlockFaceStorage blockFaceStorage;
    @Getter
    private MeshWithBounds calculatedMesh;
    @Getter
    @Setter
    private boolean dirty;
    @Getter
    private boolean complete;


    public int getAmountOfBlockFaces() {
        return blockFaceStorage == null ? 0 : blockFaceStorage.size();
    }

    public void setRawBlockFaces(BlockFaceStorage blockFaceStorage, boolean complete) {
        this.blockFaceStorage = blockFaceStorage;
        this.complete = complete;
    }

    public MeshWithBounds getOrGenerateMeshFromFaces(TextureAtlas textureAtlas, ClientWorld world, int regionX, int regionY, int regionZ, LightAccessor lightAccessor) {
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
        int minChunkX = world.getTerrainManager().getMeshPipeline().getRegionBounds().getMinChunkX(regionX);
        int minChunkY = world.getTerrainManager().getMeshPipeline().getRegionBounds().getMinChunkY(regionY);
        int minChunkZ = world.getTerrainManager().getMeshPipeline().getRegionBounds().getMinChunkZ(regionZ);

        calculatedMesh = ChunkMeshCalculator.buildRawMesh(blockFaceStorage, textureAtlas, lightAccessor);
        calculatedMesh.setPos(world.getChunkSizeX() * minChunkX, world.getChunkSizeY() * minChunkY, world.getChunkSizeZ() * minChunkZ);
        return calculatedMesh;
    }
}
