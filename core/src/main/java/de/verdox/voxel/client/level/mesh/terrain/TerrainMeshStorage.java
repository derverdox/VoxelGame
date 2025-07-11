package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.Gdx;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.shared.level.chunk.ChunkBase;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class TerrainMeshStorage {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<Long, TerrainMesh> renderedChunks = new HashMap<>();
    private final Map<Long, RegionQueue> queues = new HashMap<>();
    private final ClientWorld world;
    private final TerrainMeshPipeline terrainMeshPipeline;
    protected final int regionSizeX;
    protected final int regionSizeY;
    protected final int regionSizeZ;

    private final AtomicLong meshTimeSumNs = new AtomicLong(0);
    private final AtomicLong meshCount = new AtomicLong(0);

    //TODO:
    // Remake from MeshMaster. Same structure. Long index will always be the min chunk of the region.
    // Chunk Calculation Ticket works the same way as before.
    // Instead of chunkDataChangeVersion we want to use a "forced" variable

    public TerrainMeshStorage(ClientWorld world, TerrainMeshPipeline terrainMeshPipeline, int regionSizeX, int regionSizeY, int regionSizeZ) {
        this.world = world;
        this.terrainMeshPipeline = terrainMeshPipeline;
        this.regionSizeX = regionSizeX;
        this.regionSizeY = regionSizeY;
        this.regionSizeZ = regionSizeZ;
    }

    public void recalculateMesh(ClientChunk gameChunk) {
        if (gameChunk.isEmpty()) return;

        int regionX = gameChunk.getChunkX() / regionSizeX;
        int regionY = gameChunk.getChunkY() / regionSizeY;
        int regionZ = gameChunk.getChunkZ() / regionSizeZ;

        int minChunkXOfRegion = regionX * regionSizeX;
        int minChunkYOfRegion = regionY * regionSizeY;
        int minChunkZOfRegion = regionZ * regionSizeZ;

        long keyOfMinChunkX = ChunkBase.computeChunkKey(minChunkXOfRegion, minChunkYOfRegion, minChunkZOfRegion);

        RegionQueue q = queues.computeIfAbsent(keyOfMinChunkX, k -> new RegionQueue());

        MeshCalculationTicket oldTicket = q.getLastTicket(keyOfMinChunkX);
        MeshCalculationTicket newTicket = MeshCalculationTicket.forRegion(minChunkXOfRegion, minChunkYOfRegion, minChunkZOfRegion, regionSizeX, regionSizeY, regionSizeZ);

        if (oldTicket == null || newTicket.isBetterThan(oldTicket)) {
            TerrainMesh mesh = renderedChunks.computeIfAbsent(keyOfMinChunkX, k -> new TerrainMesh());
            q.addTicket(keyOfMinChunkX, newTicket);
            q.enqueue(() -> doRecalculate(keyOfMinChunkX, gameChunk, mesh));
        }
    }

    private void doRecalculate(long chunkKey, ClientChunk gameChunk, TerrainMesh terrainMesh) {
        long start = System.nanoTime();
        try {
            int regionX = gameChunk.getChunkX() / regionSizeX;
            int regionY = gameChunk.getChunkY() / regionSizeY;
            int regionZ = gameChunk.getChunkZ() / regionSizeZ;

            int minChunkXOfRegion = regionX * regionSizeX;
            int minChunkYOfRegion = regionY * regionSizeY;
            int minChunkZOfRegion = regionZ * regionSizeZ;

            var result = terrainMeshPipeline.buildMesh(world, minChunkXOfRegion, minChunkYOfRegion, minChunkZOfRegion, regionSizeX, regionSizeY, regionSizeZ);
            terrainMesh.setRawBlockFaces(result.faces(), result.completeMesh());
            Gdx.app.postRunnable(() -> gameChunk.getWorld().getRenderRegionStrategy().markDirty(gameChunk));
        } catch (Throwable t) {
            Gdx.app.error("MeshMaster", "Error while generating mesh for chunk " + chunkKey, t);
        } finally {
            long elapsed = System.nanoTime() - start;
            meshTimeSumNs.addAndGet(elapsed);
            meshCount.incrementAndGet();
        }
    }

    public TerrainMesh getChunkMeshIfAvailable(int x, int y, int z) {
        return renderedChunks.get(ChunkBase.computeChunkKey(x, y, z));
    }

    private class RegionQueue {
        private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final Map<Long, MeshCalculationTicket> tickets = new ConcurrentHashMap<>();

        void enqueue(Runnable task) {
            pending.add(task);
            scheduleNext();
        }

        public MeshCalculationTicket getLastTicket(long chunkKey) {
            return tickets.getOrDefault(chunkKey, null);
        }

        public void addTicket(long chunkKey, MeshCalculationTicket ticket) {
            tickets.put(chunkKey, ticket);
        }

        private void scheduleNext() {
            if (running.compareAndSet(false, true)) {
                executor.submit(this::runAll);
            }
        }

        private void runAll() {
            try {
                Runnable next;
                while ((next = pending.poll()) != null) {
                    next.run();
                }
            } finally {
                running.set(false);
                if (!pending.isEmpty()) {
                    scheduleNext();
                }
            }
        }
    }

    private record MeshCalculationTicket(boolean forced, byte presentNeighbors) {

        private static MeshCalculationTicket forRegion(int minChunkX, int minChunkY, int minChunkZ, int regionSizeX, int regionSizeY, int regionSizeZ) {
            int maxX = minChunkX + regionSizeX - 1;
            int maxY = minChunkY + regionSizeY - 1;
            int maxZ = minChunkZ + regionSizeZ - 1;

            for (int y = minChunkY; y <= maxY; y++) {
                for (int z = minChunkZ; z <= maxZ; z++) {
                    // Left
                    processChunk(minChunkX, y, z);
                    // Right
                    processChunk(maxX, y, z);
                }
            }

            for (int x = minChunkX + 1; x <= maxX - 1; x++) {
                for (int z = minChunkZ; z <= maxZ; z++) {
                    // DOWN
                    processChunk(x, minChunkY, z);
                    // UP
                    processChunk(x, maxY, z);
                }
            }

            for (int x = minChunkX + 1; x <= maxX - 1; x++) {
                for (int y = minChunkY + 1; y <= maxY - 1; y++) {
                    // BACKWARD
                    processChunk(x, y, minChunkZ);
                    // FORWARD
                    processChunk(x, y, maxZ);
                }
            }
        }

        private boolean isBetterThan(MeshCalculationTicket other) {
            return other.forced || this.presentNeighbors > other.presentNeighbors;
        }
    }
}
