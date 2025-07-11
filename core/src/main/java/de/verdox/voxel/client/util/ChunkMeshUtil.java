package de.verdox.voxel.client.util;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.util.Direction;

public class ChunkMeshUtil {
    public static boolean isAllowedToGenerateMesh(ClientChunk clientChunk, int centerChunkX, int centerChunkY, int centerChunkZ, int viewDistanceX, int viewDistanceY, int viewDistanceZ) {
        ClientWorld world = clientChunk.getWorld();
        int chunkX = clientChunk.getChunkX();
        int chunkY = clientChunk.getChunkY();
        int chunkZ = clientChunk.getChunkZ();

        int minViewableChunkX = centerChunkX - viewDistanceX;
        int minViewableChunkY = centerChunkY - viewDistanceY;
        int minViewableChunkZ = centerChunkZ - viewDistanceZ;

        int maxViewableChunkX = centerChunkX + viewDistanceX;
        int maxViewableChunkY = centerChunkY + viewDistanceY;
        int maxViewableChunkZ = centerChunkZ + viewDistanceZ;

        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            int relX = chunkX + direction.getOffsetX();
            int relY = chunkY + direction.getOffsetY();
            int relZ = chunkZ + direction.getOffsetZ();

            if (
                relX < minViewableChunkX || relX > maxViewableChunkX ||
                    relY < minViewableChunkY || relY > maxViewableChunkY ||
                    relZ < minViewableChunkZ || relZ > maxViewableChunkZ
            ) {
                // This relative does not exist for the client because of render distance.
                continue;
            }
            ClientChunk relative = world.getChunk(relX, relY, relZ);
            if (relative == null) {
                return false;
            }
        }
        return true;
    }
}
