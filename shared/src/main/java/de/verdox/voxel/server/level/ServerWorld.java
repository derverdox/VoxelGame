package de.verdox.voxel.server.level;

import de.verdox.voxel.server.level.chunk.ChunkMap;
import de.verdox.voxel.server.level.chunk.ServerChunk;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.server.level.generator.NoiseChunkGenerator;
import de.verdox.voxel.server.level.generator.WorldGenerator;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ServerWorld extends World<ServerChunk> {

    private final ChunkMap chunkMap = new ChunkMap(this);
    private final WorldGenerator worldGenerator = new WorldGenerator(this, new NoiseChunkGenerator(), 4);

    public ServerWorld(UUID uuid) {
        super(uuid);
    }

    public ServerWorld(UUID uuid, int minChunkY, int maxChunkY, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        super(uuid, minChunkY, maxChunkY, chunkSizeX, chunkSizeY, chunkSizeZ);
    }

    @Override
    protected void onAddChunk(ServerChunk serverChunk) {

    }

    @Override
    protected void onRemoveChunk(ServerChunk serverChunk) {

    }

    @Override
    protected void onChunkUpdate(ServerChunk serverChunk, byte localX, byte localY, byte localZ) {

    }
}
