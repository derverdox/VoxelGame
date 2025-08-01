package de.verdox.voxel.shared.level;

import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class World<CHUNK extends ChunkBase<? extends World<CHUNK>>> {
    public static final int MAX_CHUNK_SIZE = 64;

    protected final UUID uuid;
    protected final byte chunkSizeX;
    protected final byte chunkSizeY;
    protected final byte chunkSizeZ;

    @Getter
    private final WorldHeightMap<CHUNK> worldHeightMap = new WorldHeightMap<>(getChunkSizeY());

    public World(UUID uuid) {
        this.uuid = uuid;

        chunkSizeX = 16;
        chunkSizeY = 16;
        chunkSizeZ = 16;
    }

    // For packets only
    public World(UUID uuid, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        this.uuid = uuid;
        this.chunkSizeX = chunkSizeX;
        this.chunkSizeY = chunkSizeY;
        this.chunkSizeZ = chunkSizeZ;
    }

    public abstract CHUNK getChunkNow(int chunkX, int chunkY, int chunkZ);

    public abstract CHUNK getChunkNow(long chunkKey);

    public CHUNK getChunkNeighborNow(int chunkX, int chunkY, int chunkZ, Direction direction) {
        return getChunkNow(chunkX + direction.getOffsetX(), chunkY + direction.getOffsetY(), chunkZ + direction.getOffsetZ());
    }

    public CHUNK getChunkNeighborNow(CHUNK chunk, Direction direction) {
        return getChunkNeighborNow(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), direction);
    }

    public boolean hasNeighborsToAllSides(CHUNK chunk) {
        boolean result = true;
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            if(getChunkNeighborNow(chunk, direction) == null) {
                return false;
            }
        }
        return true;
    }

    public final void addChunk(CHUNK chunk) {
        onAddChunk(chunk);
        worldHeightMap.addChunk(chunk);
    }

    public final void removeChunk(CHUNK chunk) {
        onRemoveChunk(chunk);
        worldHeightMap.removeChunk(chunk);
    }

    public final void chunkUpdate(CHUNK chunk, byte localX, byte localY, byte localZ, boolean wasEmptyBefore) {
        onChunkUpdate(chunk, localX, localY, localZ, wasEmptyBefore);
        worldHeightMap.blockUpdate(chunk);
    }

    protected abstract void onAddChunk(CHUNK chunk);

    protected abstract void onRemoveChunk(CHUNK chunk);

    protected abstract void onChunkUpdate(CHUNK chunk, byte localX, byte localY, byte localZ, boolean wasEmptyBefore);

    public abstract CHUNK constructChunkObject(int chunkX, int chunkY, int chunkZ);
}
