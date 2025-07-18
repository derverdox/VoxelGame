package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.BlockFace;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Benchmark;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TerrainMeshPipeline implements DebuggableOnScreen {

    private final ChunkMeshCalculator chunkMeshCalculator;

    @Getter
    private final RegionBounds regionBounds;



    //private static final ExecutorService service = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Chunk Mesh Calculator Job - %d", 0).factory());
    private static final ExecutorService service = Executors.newFixedThreadPool(8, ThreadUtil.createFactoryForName("Chunk Mesh Calculator Job", true));
    private final AtomicInteger currentlyCalculating = new AtomicInteger();
    private final AtomicInteger alreadyCalculated = new AtomicInteger();
    private final AtomicLong lastTook = new AtomicLong();

    public TerrainMeshPipeline(ClientWorld world, ChunkMeshCalculator chunkMeshCalculator, int regionSizeX, int regionSizeY, int regionSizeZ) {
        this.chunkMeshCalculator = chunkMeshCalculator;
        this.regionBounds = new RegionBounds(regionSizeX, regionSizeY, regionSizeZ);
        if (ClientBase.clientRenderer != null) {
            ClientBase.clientRenderer.getDebugScreen().attach(this);
        }
    }

    public MeshResult buildMesh(ClientWorld world, ClientChunk chunk) throws ExecutionException, InterruptedException {
        int regionX = getRegionBounds().getRegionX(chunk.getChunkX());
        int regionY = getRegionBounds().getRegionY(chunk.getChunkY());
        int regionZ = getRegionBounds().getRegionZ(chunk.getChunkZ());
        return buildMesh(world, regionX, regionY, regionZ);
    }

    public MeshResult buildMesh(ClientWorld world, int regionX, int regionY, int regionZ) throws ExecutionException, InterruptedException {
        Benchmark benchmark = new Benchmark(1);
        benchmark.start();
        currentlyCalculating.addAndGet(1);
        Long2ObjectOpenHashMap<Future<BlockFaceStorage>> meshes = new Long2ObjectOpenHashMap<>();
        long start = System.currentTimeMillis();

        int minChunkX = regionBounds.getMinChunkX(regionX);
        int minChunkY = regionBounds.getMinChunkY(regionY);
        int minChunkZ = regionBounds.getMinChunkZ(regionZ);

        int maxChunkX = regionBounds.getMaxChunkX(regionX);
        int maxChunkY = regionBounds.getMaxChunkY(regionY);
        int maxChunkZ = regionBounds.getMaxChunkZ(regionZ);

        boolean complete = true;
        benchmark.startSection("Compute Chunk meshes");
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cy = minChunkY; cy <= maxChunkY; cy++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {

                    ClientChunk chunk = world.getChunk(cx, cy, cz);
                    if (chunk == null) {
                        complete = false;
                        continue;
                    }
                    meshes.put(ChunkBase.computeChunkKey(cx, cy, cz), CompletableFuture.completedFuture(chunkMeshCalculator.calculateChunkMesh(chunk)));
                }
            }
        }
        benchmark.endSection();

        BlockFaceStorage storageOfRegion;

        benchmark.startSection("Build region mesh");
        if (regionBounds.regionSizeX() > 1 || regionBounds.regionSizeY() > 1 || regionBounds.regionSizeZ() > 1) {
            storageOfRegion = buildMeshForRegion(world, minChunkX, minChunkY, minChunkZ, meshes);
        } else {
            storageOfRegion = meshes.get(ChunkBase.computeChunkKey(minChunkX, minChunkY, minChunkZ)).get();
        }
        benchmark.endSection();

        benchmark.startSection("Greedy meshing");
        storageOfRegion = applyGreedyMeshing(storageOfRegion);
        benchmark.endSection();
        long end = System.currentTimeMillis() - start;
        lastTook.set(end);
        currentlyCalculating.addAndGet(-1);
        if (storageOfRegion.getSize() != 0) {
            alreadyCalculated.addAndGet(1);
        }
        benchmark.end();

        benchmark.printToLines("Took").forEach(System.out::println);
        return new MeshResult(complete, storageOfRegion);
    }

    /**
     * Used to merge independent chunks into larger mesh regions
     */
    private BlockFaceStorage buildMeshForRegion(ClientWorld world, int minChunkX, int minChunkY, int minChunkZ, Long2ObjectOpenHashMap<Future<BlockFaceStorage>> chunkMeshCalculations) throws ExecutionException, InterruptedException {
        BlockFaceStorage merged = new BlockFaceStorage(world.getChunkSizeX() * regionBounds.regionSizeX(), world.getChunkSizeY() * regionBounds.regionSizeY(), world.getChunkSizeZ() * regionBounds.regionSizeZ());

        for (Long key : chunkMeshCalculations.keySet()) {
            Future<BlockFaceStorage> future = chunkMeshCalculations.get(key);

            int cx = ChunkBase.unpackChunkX(key);
            int cy = ChunkBase.unpackChunkY(key);
            int cz = ChunkBase.unpackChunkZ(key);

            int offsetX = (cx - minChunkX) * world.getChunkSizeX();
            int offsetY = (cy - minChunkY) * world.getChunkSizeY();
            int offsetZ = (cz - minChunkZ) * world.getChunkSizeZ();

            BlockFaceStorage storage = future.get();
            if (storage == null) {
                continue;
            }

            for (BlockFace blockFace : storage) {
                merged.addFace(blockFace.addOffset(offsetX, offsetY, offsetZ));
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

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine("Mesh Pipeline calculating: " + currentlyCalculating.get());
        debugScreen.addDebugTextLine("Already calculated meshes: " + alreadyCalculated.get());
    }

    public record MeshResult(boolean completeMesh, BlockFaceStorage faces) {
    }
}
