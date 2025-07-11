package de.verdox.voxel.server.level.generator;

import de.verdox.voxel.server.level.chunk.ServerChunk;
import de.verdox.voxel.shared.data.types.Blocks;

public class DebugChunkGenerator implements ChunkGenerator {
    @Override
    public void generateNoise(ServerChunk gameChunk) {
        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 0 && gameChunk.getChunkZ() == 0) {
            mostlyFillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 1 && gameChunk.getChunkY() == 0 && gameChunk.getChunkZ() == 0) {
            mostlyFillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 0 && gameChunk.getChunkZ() == 1) {
            mostlyFillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 1 && gameChunk.getChunkY() == 0 && gameChunk.getChunkZ() == 1) {
            mostlyFillChunk(gameChunk, Blocks.STONE);
        }
/*
        if (gameChunk.getChunkX() == 1 && gameChunk.getChunkY() == 0 && gameChunk.getChunkZ() == 0) {
            fillChunk(gameChunk, Blocks.STONE);
        }

        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 0 && gameChunk.getChunkZ() == 2) {
            fillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 1 && gameChunk.getChunkZ() == 2) {
            fillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 2 && gameChunk.getChunkZ() == 2) {
            fillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 3 && gameChunk.getChunkZ() == 2) {
            fillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 4 && gameChunk.getChunkZ() == 2) {
            fillChunk(gameChunk, Blocks.STONE);
        }*/
    }

    @Override
    public void generateSurfaceBlocks(ServerChunk gameChunk) {

    }
}
