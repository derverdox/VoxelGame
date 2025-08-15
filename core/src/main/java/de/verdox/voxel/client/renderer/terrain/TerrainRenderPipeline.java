package de.verdox.voxel.client.renderer.terrain;

import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.level.chunk.proto.ProtoMask;
import de.verdox.voxel.client.renderer.terrain.regions.TerrainRegion;
import de.verdox.voxel.client.renderer.debug.DebuggableOnScreen;
import de.verdox.voxel.shared.util.TerrainRenderStats;

public interface TerrainRenderPipeline extends DebuggableOnScreen {

    MeshService getMeshService();

    void drawMesh(
            ProtoMask.FaceType faceType,
            int cameraX, int cameraY, int cameraZ,
            int meshX, int meshY, int meshZ,
            int transformX, int transformY, int transformZ,
            TerrainRenderStats terrainRenderStats
    );

    interface MeshService {
        void createChunkMesh(TerrainRegion region, TerrainChunk chunk);

        void removeChunkMesh(TerrainRegion region, TerrainChunk chunk);

        default boolean skip(TerrainChunk chunk) {
            return chunk.isEmpty() || !chunk.hasNeighborsToAllSides();
        }
    }
}
