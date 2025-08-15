package de.verdox.voxel.client.level;

import com.badlogic.gdx.graphics.Camera;
import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.TerrainRenderStats;

public interface TerrainManager {
    void setCameraChunk(int chunkX, int chunkY, int chunkZ);

    TerrainChunk getChunkNow(int chunkX, int chunkY, int chunkZ);

    default TerrainChunk getChunkNow(long chunkKey) {
        return getChunkNow(Chunk.unpackChunkX(chunkKey), Chunk.unpackChunkY(chunkKey), Chunk.unpackChunkZ(chunkKey));
    }

    void addChunk(Chunk chunk);

    void removeChunk(Chunk chunk);

    void afterChunkUpdate(Chunk chunk, boolean wasEmptyBefore);

    int getCenterChunkX();

    int getCenterChunkY();

    int getCenterChunkZ();

    ClientWorld getWorld();

    int renderTerrain(Camera camera, ClientWorld world, int viewDistanceX, int viewDistanceY, int viewDistanceZ, TerrainRenderStats renderStats);
}
