package de.verdox.voxel.server.level.generator;

import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.chunk.Chunk;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.NoiseType;

public class BenchmarkNoiseChunkGenerator implements ChunkGenerator {
    FastNoise noise = FastNoise.builder()
        .type(NoiseType.PERLIN)
        .fractal(FractalType.FBM)
        .frequency(0.01f)
        .build();

    @Override
    public void generateNoise(Chunk gameChunk) {
        int minHeight = 40;
        int noiseMaxHeight = 145;

        int maxHeightGenerated = 40 + 145;
        int maxChunkYToGenerateNoiseIn = Chunk.chunkY(gameChunk.getWorld(), maxHeightGenerated);

        if (gameChunk.getChunkY() > maxChunkYToGenerateNoiseIn) {
            return;
        }


        for (int x = 0; x < gameChunk.getWorld().getChunkSizeX(); x++) {
            for (int z = 0; z < gameChunk.getWorld().getChunkSizeZ(); z++) {
                int globalX = gameChunk.globalX(x);
                int globalZ = gameChunk.globalZ(z);
                int heightAtPos = (int) (noise.getNoise(globalX, globalZ) * noiseMaxHeight) + minHeight;

                int chunkYOfMaxHeight = Chunk.chunkY(gameChunk.getWorld(), heightAtPos);

                if (chunkYOfMaxHeight == gameChunk.getChunkY()) {
                    for (int y = 0; y < gameChunk.localY(heightAtPos); y++) {
                        gameChunk.setBlockAt(Blocks.STONE, x, y, z);
                    }
                } else if (chunkYOfMaxHeight > gameChunk.getChunkY()) {
                    for (int y = 0; y < gameChunk.getWorld().getChunkSizeY(); y++) {
                        gameChunk.setBlockAt(Blocks.STONE, x, y, z);
                    }
                }
            }
        }
    }

    @Override
    public void generateSurfaceBlocks(Chunk gameChunk) {

    }
}
