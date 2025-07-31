package de.verdox.voxel.server.level.generator;

import de.verdox.voxel.server.level.chunk.ServerChunk;
import de.verdox.voxel.shared.data.types.Blocks;

public class DebugChunkGenerator implements ChunkGenerator {
    @Override
    public void generateNoise(ServerChunk gameChunk) {

        if (gameChunk.getChunkX() == 1 && gameChunk.getChunkY() == 4 && gameChunk.getChunkZ() == 0) {
            fillChunk(gameChunk, Blocks.STONE);
        }

/*        if(gameChunk.getChunkX() % 2 == 0 && gameChunk.getChunkY() % 4 == 0 && gameChunk.getChunkZ() % 2 == 0) {
            fillChunk(gameChunk, Blocks.STONE);
        }

        if(gameChunk.getChunkX() % 3 == 0 && gameChunk.getChunkY() % 4 == 0 && gameChunk.getChunkZ() % 3 == 0) {
            fillChunk(gameChunk, Blocks.STONE);
        }*/

        //gameChunk.setBlockAt(Blocks.STONE, 0, 0, 1);
/*
        if(gameChunk.getChunkX() % 3 == 0 && gameChunk.getChunkY() % 1 == 0 && gameChunk.getChunkZ() % 3 == 0) {
            gameChunk.setBlockAt(Blocks.STONE, 1, 1, 0);
            gameChunk.setBlockAt(Blocks.STONE, 2, 1, 0);
            gameChunk.setBlockAt(Blocks.STONE, 3, 1, 0);
            gameChunk.setBlockAt(Blocks.STONE, 4, 1, 0);
            gameChunk.setBlockAt(Blocks.STONE, 5, 1, 0);


            gameChunk.setBlockAt(Blocks.STONE, 1, 1, 1);
            gameChunk.setBlockAt(Blocks.STONE, 1, 1, 2);
            gameChunk.setBlockAt(Blocks.STONE, 1, 1, 3);
            gameChunk.setBlockAt(Blocks.STONE, 1, 1, 4);
            gameChunk.setBlockAt(Blocks.STONE, 1, 1, 5);
        }*/


/*        if (gameChunk.getChunkX() == 1 && gameChunk.getChunkY() == 4 && gameChunk.getChunkZ() == 0) {
            fillChunk(gameChunk, Blocks.STONE);
        }*/
/*        if (gameChunk.getChunkX() == 0 && gameChunk.getChunkY() == 4 && gameChunk.getChunkZ() == 1) {
            mostlyFillChunk(gameChunk, Blocks.STONE);
        }
        if (gameChunk.getChunkX() == 1 && gameChunk.getChunkY() == 4 && gameChunk.getChunkZ() == 1) {
            mostlyFillChunk(gameChunk, Blocks.STONE);
        }*/
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
