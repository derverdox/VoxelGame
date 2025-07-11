package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.BlockFace;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.shared.level.chunk.ChunkBase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TerrainMeshPipeline {
    private final ChunkMeshCalculator chunkMeshCalculator;

    public TerrainMeshPipeline(ChunkMeshCalculator chunkMeshCalculator) {
        this.chunkMeshCalculator = chunkMeshCalculator;
    }

    public MeshResult buildMesh(ClientWorld world, int minChunkX, int minChunkY, int minChunkZ, int regionSizeX, int regionSizeY, int regionSizeZ) throws ExecutionException, InterruptedException {
        Map<Long, Future<BlockFaceStorage>> meshes = new HashMap<>();

        boolean complete = true;
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int cx = minChunkX; cx < minChunkX + regionSizeX; cx++) {
                for (int cy = minChunkY; cy < minChunkY + regionSizeY; cy++) {
                    for (int cz = minChunkZ; cz < minChunkZ + regionSizeZ; cz++) {

                        ClientChunk chunk = world.getChunk(minChunkX, minChunkY, minChunkZ);
                        if (chunk == null) {
                            complete = false;
                            continue;
                        }
                        meshes.put(ChunkBase.computeChunkKey(cx, cy, cz), service.submit(() -> chunkMeshCalculator.calculateChunkMesh(chunk)));
                    }
                }
            }
        }

        BlockFaceStorage storageOfRegion;

        if (regionSizeX > 1 || regionSizeY > 1 || regionSizeZ > 1) {
            storageOfRegion = buildMeshForRegion(world, minChunkX, minChunkY, minChunkZ, regionSizeX, regionSizeY, regionSizeZ, meshes);
        } else {
            storageOfRegion = meshes.get(ChunkBase.computeChunkKey(minChunkX, minChunkY, minChunkZ)).get();
        }

        storageOfRegion = applyGreedyMeshing(storageOfRegion);
        return new MeshResult(complete, storageOfRegion);
    }

    /**
     * Used to merge independent chunks into larger mesh regions
     */
    private BlockFaceStorage buildMeshForRegion(ClientWorld world, int minChunkX, int minChunkY, int minChunkZ, int regionSizeX, int regionSizeY, int regionSizeZ, Map<Long, Future<BlockFaceStorage>> chunkMeshCalculations) throws ExecutionException, InterruptedException {
        BlockFaceStorage merged = new BlockFaceStorage(world.getChunkSizeX() * regionSizeX, world.getChunkSizeY() * regionSizeY, world.getChunkSizeZ() * regionSizeZ);

        for (int x = 0; x < regionSizeX; x++) {
            for (int y = 0; y < regionSizeY; y++) {
                for (int z = 0; z < regionSizeZ; z++) {

                    int cx = minChunkX + x;
                    int cy = minChunkY + y;
                    int cz = minChunkZ + z;
                    long key = ChunkBase.computeChunkKey(cx, cy, cz);

                    BlockFaceStorage storageOfChunk = chunkMeshCalculations.get(key).get();

                    //TODO: Maybe parallelize for each face since they are stored in different maps
                    for (BlockFace blockFace : storageOfChunk) {
                        merged.addFace(blockFace.addOffset(x, y, z));
                    }
                }
            }
        }

        return merged;
    }

    /**
     * Applies greedy meshing to a region
     */
    private BlockFaceStorage applyGreedyMeshing(BlockFaceStorage regionStorage) {
        return regionStorage.applyGreedyMeshing();
    }

    public record MeshResult(boolean completeMesh, BlockFaceStorage faces) {}
}
