package de.verdox.voxel.server.level.generator;

import de.verdox.voxel.server.level.chunk.ServerChunk;
import de.verdox.voxel.shared.level.block.BlockBase;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public interface ChunkGenerator {
    Logger LOGGER = Logger.getLogger(ChunkGenerator.class.getSimpleName());

    /**
     * Generates basic noise pattern for the world.
     */
    void generateNoise(ServerChunk gameChunk);

    /**
     * Generates the surface on top of a GameChunk. Only surface chunks are passed into this function
     */
    void generateSurfaceBlocks(ServerChunk gameChunk);

    default void fillChunk(ServerChunk gameChunk, BlockBase blockBase) {
        for (int x = 0; x < gameChunk.getWorld().getChunkSizeX(); x++) {
            for (int y = 0; y < gameChunk.getWorld().getChunkSizeY(); y++) {
                for (int z = 0; z < gameChunk.getWorld().getChunkSizeZ(); z++) {
                    gameChunk.setBlockAt(blockBase, x, y, z);
                }
            }
        }
    }

    default void mostlyFillChunk(ServerChunk gameChunk, BlockBase blockBase) {
        for (int x = 0; x < gameChunk.getWorld().getChunkSizeX(); x++) {
            for (int y = 0; y < gameChunk.getWorld().getChunkSizeY(); y++) {
                for (int z = 0; z < gameChunk.getWorld().getChunkSizeZ(); z++) {
                    if(ThreadLocalRandom.current().nextFloat(1f) > 0.8f) {
                        gameChunk.setBlockAt(blockBase, x, y, z);
                    }
                }
            }
        }
    }
}
