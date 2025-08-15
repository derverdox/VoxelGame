package de.verdox.voxel.client.renderer.terrain.regions.graph;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.util.TerrainRenderStats;

public interface RegionBasedTerrainRenderGraph {
    void addRegion(int x, int y, int z);

    void removeRegion(int x, int y, int z);

    int renderTerrain(Camera camera, ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ, TerrainRenderStats renderStats);
}
