package de.verdox.voxel.server.level.chunk;

import de.verdox.voxel.server.level.ServerWorld;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import lombok.Getter;

@Getter
public class ServerChunk extends ChunkBase<ServerWorld> {
    private final ServerWorld world;
    private final int chunkX;
    private final int chunkY;
    private final int chunkZ;

    public ServerChunk(ServerWorld world, int chunkX, int chunkY, int chunkZ) {
        super(world, chunkX, chunkY, chunkZ);
        this.world = world;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }
}
