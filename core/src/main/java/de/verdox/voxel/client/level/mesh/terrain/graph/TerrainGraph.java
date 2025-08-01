package de.verdox.voxel.client.level.mesh.terrain.graph;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.level.ClientWorld;

public interface TerrainGraph {
    void addRegion(int x, int y, int z);

    void removeRegion(int x, int y, int z);

    void renderTerrain(Camera camera, ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ);
}
