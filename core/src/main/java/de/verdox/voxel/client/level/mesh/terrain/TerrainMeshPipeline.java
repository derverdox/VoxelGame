package de.verdox.voxel.client.level.mesh.terrain;

import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorageImpl;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import de.verdox.voxel.shared.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TerrainMeshPipeline implements DebuggableOnScreen {
    // Summierte Verarbeitungszeit in Nanosekunden (ohne Leerlauf)
    private static final AtomicLong totalProcessingTimeNs = new AtomicLong();

    private final ClientWorld world;
    private final ChunkMeshCalculator chunkMeshCalculator;
    private final Long2ObjectMap<TerrainMeshJob> jobs = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    @Getter
    private final RegionBounds regionBounds;

    private static final ExecutorService service = Executors.newFixedThreadPool(8, ThreadUtil.createFactoryForName("Chunk Mesh Calculator Job", true));
    private final AtomicInteger currentlyCalculating = new AtomicInteger();
    private final AtomicInteger alreadyCalculated = new AtomicInteger();
    private final AtomicLong lastTook = new AtomicLong();

    public TerrainMeshPipeline(ClientWorld world, ChunkMeshCalculator chunkMeshCalculator, int regionSizeX, int regionSizeY, int regionSizeZ) {
        this.world = world;
        this.chunkMeshCalculator = chunkMeshCalculator;
        this.regionBounds = new RegionBounds(regionSizeX, regionSizeY, regionSizeZ);
        if (ClientBase.clientRenderer != null) {
            ClientBase.clientRenderer.getDebugScreen().attach(this);
        }
    }

    public MeshResult buildMesh(ClientWorld world, int regionX, int regionY, int regionZ, int lodLevel) throws ExecutionException, InterruptedException {
/*        Benchmark benchmark = new Benchmark(1);
        benchmark.start();*/

        long regionKey = ChunkBase.computeChunkKey(regionX, regionY, regionZ);

        if (!jobs.containsKey(regionKey)) {
            jobs.put(regionKey, new TerrainMeshJob(lodLevel, regionX, regionY, regionZ));
        }

        long startProc = -1;

        TerrainMeshJob meshJob = jobs.get(regionKey);

        currentlyCalculating.addAndGet(1);
        long start = System.currentTimeMillis();

        int minChunkX = regionBounds.getMinChunkX(regionX);
        int minChunkY = regionBounds.getMinChunkY(regionY);
        int minChunkZ = regionBounds.getMinChunkZ(regionZ);

        int maxChunkX = regionBounds.getMaxChunkX(regionX);
        int maxChunkY = regionBounds.getMaxChunkY(regionY);
        int maxChunkZ = regionBounds.getMaxChunkZ(regionZ);

        boolean complete = true;


        //benchmark.startSection("Compute Chunk meshes");

        CompletableFuture[] calculations = new CompletableFuture[(maxChunkX - minChunkX + 1) * (maxChunkY - minChunkY + 1) * (maxChunkZ - minChunkZ + 1)];

        int counter = 0;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cy = minChunkY; cy <= maxChunkY; cy++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {

                    ClientChunk chunk = world.getChunk(cx, cy, cz);
                    if (chunk == null) {
                        complete = false;
                        continue;
                    }

                    if (startProc == -1) {
                        startProc = System.nanoTime();
                    }

                    boolean canGenerate = true;
                    for (int i = 0; i < Direction.values().length; i++) {
                        Direction direction = Direction.values()[i];

                        ClientChunk neighbor = world.getChunkNow(cx + direction.getOffsetX(), cy + direction.getOffsetY(), cz + direction.getOffsetZ());
                        if (neighbor == null) {
                            canGenerate = false;
                            break;
                        }
                    }

                    if (!canGenerate) {
                        continue;
                    }

                    CompletableFuture<?> future = CompletableFuture.runAsync(() -> meshJob.recomputeChunk(chunk, true, true));

/*                    meshJob.recomputeChunk(chunk, true, true);
                    CompletableFuture<?> future = CompletableFuture.completedFuture(null);*/
                    calculations[counter++] = future;
                }
            }
        }

        //benchmark.endSection();

        for (int i = 0; i < calculations.length; i++) {
            CompletableFuture<?> future = calculations[i];
            if (future == null) {
                continue;
            }
            future.join();
        }


        //benchmark.startSection("Greedy meshing");
        //benchmark.endSection();

        long duration = System.nanoTime() - startProc;
        totalProcessingTimeNs.addAndGet(duration);
        lastTook.set(TimeUnit.NANOSECONDS.toMillis(duration));
        currentlyCalculating.addAndGet(-1);
        if (meshJob.getStorageOfJob().getSize() != 0) {
            alreadyCalculated.addAndGet(1);
        }
        //benchmark.end();

        if (meshJob.isDone()) {
            jobs.remove(regionKey);
        }

        return new MeshResult(complete, meshJob.getStorageOfJob());
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine("Mesh Pipeline calculating: " + currentlyCalculating.get());

        long processed = alreadyCalculated.get();
        double totalSec = totalProcessingTimeNs.get() / 1_000_000_000.0;
        double throughput = (totalSec > 0) ? processed / totalSec : 0;

        String line = String.format(
                "Meshes: %d, Processing Time: %.2fs, Throughput: %.1f meshes/s, Last: %dms",
                processed, totalSec, throughput, lastTook.get()
        );
        debugScreen.addDebugTextLine(line);
    }

    public record MeshResult(boolean completeMesh, TerrainFaceStorage storage) {
    }

    private TerrainFaceStorage createStorageForLodLevel(int lodLevel) {
        return new TerrainFaceStorageImpl(world, (byte) lodLevel);
    }

    @Getter
    private class TerrainMeshJob {
        private final TerrainFaceStorage storageOfJob;
        private final int lodLevel;
        private final int regionX;
        private final int regionY;
        private final int regionZ;
        private AtomicLong computedChunks = new AtomicLong();

        public TerrainMeshJob(int lodLevel, int regionX, int regionY, int regionZ) {
            this.lodLevel = lodLevel;
            this.regionX = regionX;
            this.regionY = regionY;
            this.regionZ = regionZ;
            this.storageOfJob = createStorageForLodLevel(lodLevel);
        }

        public boolean isDone() {
            return computedChunks.get() >= (long) regionBounds.regionSizeX() * regionBounds.regionSizeY() * regionBounds.regionSizeZ();
        }

        public void recomputeChunk(ClientChunk clientChunk, boolean generateInitial, boolean notifyNeighbors) {
            recomputeChunk(clientChunk, generateInitial, notifyNeighbors, false);
        }

        public void recomputeChunk(ClientChunk clientChunk, boolean generateInitial, boolean notifyNeighbors, boolean isRecursive) {
            int minChunkX = regionBounds.getMinChunkX(regionX);
            int minChunkY = regionBounds.getMinChunkY(regionY);
            int minChunkZ = regionBounds.getMinChunkZ(regionZ);

            int offsetX = (byte) (clientChunk.getChunkX() - minChunkX);
            int offsetY = (byte) (clientChunk.getChunkY() - minChunkY);
            int offsetZ = (byte) (clientChunk.getChunkZ() - minChunkZ);

            boolean hasFaces = storageOfJob.hasFacesForChunk(offsetX, offsetY, offsetZ);

            if (generateInitial && hasFaces) {
                return;
            }

            if (!hasFaces) {
                computedChunks.getAndIncrement();
            }

            if (clientChunk.isEmpty()) {
                return;
            }

            if (!isRecursive) {
                storageOfJob.getRegionalLock().withLock(offsetX, offsetY, offsetZ, notifyNeighbors ? 1 : 0, () -> recalculate(clientChunk, notifyNeighbors, offsetX, offsetY, offsetZ));
            } else {
                recalculate(clientChunk, notifyNeighbors, offsetX, offsetY, offsetZ);
            }
        }

        private void recalculate(ClientChunk clientChunk, boolean notifyNeighbors, int offsetX, int offsetY, int offsetZ) {
            TerrainFaceStorage.ChunkFaceStorage chunkFaceStorage = storageOfJob.getOrCreateChunkFaces(offsetX, offsetY, offsetZ);
            chunkMeshCalculator.calculateChunkMesh(chunkFaceStorage, clientChunk, lodLevel);


            if (!notifyNeighbors) {
                return;
            }

            for (int i = 0; i < Direction.values().length; i++) {
                Direction direction = Direction.values()[i];

                ClientChunk neighborChunk = clientChunk.getWorld().getChunkNow(clientChunk.getChunkX() + direction.getOffsetX(), clientChunk.getChunkY() + direction.getOffsetY(), clientChunk.getChunkZ() + direction.getOffsetZ());

                if (neighborChunk == null) {
                    continue;
                }

                recomputeChunk(neighborChunk, false, false, true);
            }
        }
    }
}
