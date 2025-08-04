package de.verdox.voxel.server.level.generator;

import de.verdox.voxel.server.level.generator.spline.NoiseHeightSpline;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.chunk.Chunk;
import personthecat.fastnoise.FastNoise;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.NoiseType;

public class NoiseChunkGenerator implements ChunkGenerator {
    // Fläche: Kontinente vs. Ozeane
    private final FastNoise continentalness;
    // 3D Volume-Noise für Höhlen
    private final FastNoise caveNoise;

    public static final NoiseHeightSpline erosionSpline = new NoiseHeightSpline.Builder()
            .addPoint(0.2f, 30)
            .addPoint(0.35f, 64)
            .addPoint(0.6f, 80)
            .addPoint(1f, 90)
            .build();

    public NoiseChunkGenerator(int seed) {
        continentalness = FastNoise.builder()
                .seed(seed)
                .type(NoiseType.SIMPLEX)
                .frequency(0.02f)
                .fractal(FractalType.FBM).octaves(4).gain(0.5f).lacunarity(2f)
                .build();

        caveNoise = FastNoise.builder()
                .seed(seed+4)
                .type(NoiseType.SIMPLEX)
                .frequency(0.03f)
                .fractal(FractalType.FBM).octaves(3)
                .build();
    }

    @Override
    public void generateNoise(Chunk chunk) {
        int cx = chunk.getChunkX(), cz = chunk.getChunkZ();
        int sizeX = chunk.getWorld().getChunkSizeX();
        int sizeZ = chunk.getWorld().getChunkSizeZ();
        int seaLevel = 64;

        for (int lx = 0; lx < sizeX; lx++) {
            for (int lz = 0; lz < sizeZ; lz++) {
                int gx = chunk.globalX(lx), gz = chunk.globalZ(lz);

                // 1) Continentalness [0,1]
                float cont = (continentalness.getNoise(gx, gz) + 1f) * .5f;



                int height = (int) erosionSpline.evaluate(cont);

                // 5) Fülle Stein bis Höhe
                int chunkYOfMax = Chunk.chunkY(chunk.getWorld(), height);
                for (int y = 0; y < chunk.getWorld().getChunkSizeY(); y++) {
                    if (chunk.getChunkY() < chunkYOfMax ||
                            (chunk.getChunkY() == chunkYOfMax && y <= chunk.localY(height))) {
                        chunk.setBlockAt(Blocks.STONE, lx, y, lz);
                    }
                }
            }
        }
    }

    @Override
    public void generateSurfaceBlocks(Chunk gameChunk) {

    }
}