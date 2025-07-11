package de.verdox.voxel.server.level.chunk;

import de.verdox.voxel.server.level.ServerWorld;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.palette.ChunkBlockPalette;

public class ServerChunk extends ChunkBase<ServerWorld> {
    public ServerChunk(ServerWorld world, int chunkX, int chunkY, int chunkZ) {
        super(world, chunkX, chunkY, chunkZ);
    }

    public ServerChunk(ServerWorld world, int chunkX, int chunkY, int chunkZ, ChunkBlockPalette chunkBlockPalette, byte[][] heightmap, byte[][] depthMap) {
        super(world, chunkX, chunkY, chunkZ, chunkBlockPalette, heightmap, depthMap);
    }
}
