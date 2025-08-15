package de.verdox.voxel.client.renderer.terrain.regions.mesh;

import de.verdox.voxel.client.renderer.terrain.regions.TerrainRegion;
import de.verdox.voxel.client.util.ThroughputBenchmark;

public interface RegionMeshCalculator {
    ThroughputBenchmark regionCalculatorThroughput = new ThroughputBenchmark("RegionMeshes");

    void updateTerrainMesh(TerrainRegion terrainRegion, int lodLevel);
}
