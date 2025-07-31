package de.verdox.voxel.server.level;

import de.verdox.voxel.server.level.chunk.ChunkMap;
import de.verdox.voxel.server.level.chunk.ServerChunk;
import de.verdox.voxel.server.level.generator.BenchmarkNoiseChunkGenerator;
import de.verdox.voxel.server.level.generator.DebugChunkGenerator;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.server.level.generator.WorldGenerator;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ServerWorld extends World<ServerChunk> {

    private final ChunkMap chunkMap = new ChunkMap(this);
    private final WorldGenerator worldGenerator = new WorldGenerator(this, new BenchmarkNoiseChunkGenerator(), 4);

    public ServerWorld(UUID uuid) {
        super(uuid);
    }

    public ServerWorld(UUID uuid, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        super(uuid, chunkSizeX, chunkSizeY, chunkSizeZ);
    }

    @Override
    public ServerChunk getChunkNow(int chunkX, int chunkY, int chunkZ) {
        return chunkMap.getChunk(chunkX, chunkY, chunkZ).orElse(null);
    }

    @Override
    public ServerChunk getChunkNow(long chunkKey) {
        return getChunkNow(ChunkBase.unpackChunkX(chunkKey), ChunkBase.unpackChunkY(chunkKey), ChunkBase.unpackChunkZ(chunkKey));
    }

    @Override
    protected void onAddChunk(ServerChunk serverChunk) {

    }

    @Override
    protected void onRemoveChunk(ServerChunk serverChunk) {

    }

    @Override
    protected void onChunkUpdate(ServerChunk serverChunk, byte localX, byte localY, byte localZ, boolean wasEmptyBefore) {

    }

    @Override
    public ServerChunk constructChunkObject(int chunkX, int chunkY, int chunkZ) {
        return new ServerChunk(this, chunkX, chunkY, chunkZ);
    }
}
