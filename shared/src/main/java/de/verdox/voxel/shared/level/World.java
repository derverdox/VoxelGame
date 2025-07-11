package de.verdox.voxel.shared.level;

import de.verdox.voxel.shared.level.chunk.ChunkBase;
import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class World<CHUNK extends ChunkBase<? extends World<CHUNK>>> {
    public static final int MAX_CHUNK_SIZE = 64;

    protected final UUID uuid;
    protected final int minChunkY;
    protected final int maxChunkY;
    protected final byte chunkSizeX;
    protected final byte chunkSizeY;
    protected final byte chunkSizeZ;

    @Getter
    private final WorldHeightMap<CHUNK> worldHeightMap = new WorldHeightMap<>(getChunkSizeY());

    public World(UUID uuid) {
        this.uuid = uuid;

        minChunkY = 0;
        maxChunkY = 16;
        chunkSizeX = 16;
        chunkSizeY = 16;
        chunkSizeZ = 16;
    }

    // For packets only
    public World(UUID uuid, int minChunkY, int maxChunkY, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        this.uuid = uuid;
        this.minChunkY = minChunkY;
        this.maxChunkY = maxChunkY;
        this.chunkSizeX = chunkSizeX;
        this.chunkSizeY = chunkSizeY;
        this.chunkSizeZ = chunkSizeZ;
    }

    public final void addChunk(CHUNK chunk) {
        onAddChunk(chunk);
        worldHeightMap.addChunk(chunk);
    }

    public final void removeChunk(CHUNK chunk) {
        onRemoveChunk(chunk);
        worldHeightMap.removeChunk(chunk);
    }

    public final void chunkUpdate(CHUNK chunk, byte localX, byte localY, byte localZ) {
        onChunkUpdate(chunk, localX, localY, localZ);
        worldHeightMap.blockUpdate(chunk);
    }

    protected abstract void onAddChunk(CHUNK chunk);

    protected abstract void onRemoveChunk(CHUNK chunk);

    protected abstract void onChunkUpdate(CHUNK chunk, byte localX, byte localY, byte localZ);
}
