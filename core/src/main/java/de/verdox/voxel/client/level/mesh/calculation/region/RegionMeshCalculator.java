package de.verdox.voxel.client.level.mesh.calculation.region;

import de.verdox.voxel.client.level.mesh.TerrainRegion;
import de.verdox.voxel.client.util.ThroughputBenchmark;

public interface RegionMeshCalculator {
    ThroughputBenchmark regionCalculatorThroughput = new ThroughputBenchmark("RegionMeshes");

    void updateTerrainMesh(TerrainRegion terrainRegion, int lodLevel);
}
