package de.verdox.voxel.client.renderer.graph;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.level.ClientWorld;

public interface TerrainRenderGraph {
    void addRegion(int x, int y, int z);

    void removeRegion(int x, int y, int z);

    void renderTerrain(Camera camera, ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ);
}
