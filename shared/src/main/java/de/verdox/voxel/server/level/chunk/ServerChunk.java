package de.verdox.voxel.server.level.chunk;

import de.verdox.voxel.server.level.ServerWorld;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.DepthMap;
import de.verdox.voxel.shared.level.chunk.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.util.palette.ChunkBlockPalette;

public class ServerChunk extends ChunkBase<ServerWorld> {
    public ServerChunk(ServerWorld world, int chunkX, int chunkY, int chunkZ) {
        super(world, chunkX, chunkY, chunkZ);
    }

    public ServerChunk(ServerWorld world, int chunkX, int chunkY, int chunkZ, ChunkBlockPalette chunkBlockPalette, HeightMap heightMap, DepthMap depthMap, ChunkLightData chunkLightData) {
        super(world, chunkX, chunkY, chunkZ, chunkBlockPalette, heightMap, depthMap, chunkLightData);
    }
}
