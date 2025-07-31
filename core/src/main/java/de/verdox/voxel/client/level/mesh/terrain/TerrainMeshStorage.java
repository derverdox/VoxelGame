package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.Gdx;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class TerrainMeshStorage {
    private final ExecutorService executor = Executors.newFixedThreadPool(4, ThreadUtil.createFactoryForName("Meshing Thread", true));
    private final Long2ObjectOpenHashMap<TerrainMesh> renderedRegions = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<RegionQueue> queues = new Long2ObjectOpenHashMap<>();
    private final ClientWorld world;
    private final TerrainManager terrainManager;
    protected final int regionSizeX;
    protected final int regionSizeY;
    protected final int regionSizeZ;

    private final AtomicLong meshTimeSumNs = new AtomicLong(0);
    private final AtomicLong meshCount = new AtomicLong(0);

    //TODO:
    // Remake from MeshMaster. Same structure. Long index will always be the min chunk of the region.
    // Chunk Calculation Ticket works the same way as before.
    // Instead of chunkDataChangeVersion we want to use a "forced" variable

    public TerrainMeshStorage(ClientWorld world, TerrainManager terrainManager, int regionSizeX, int regionSizeY, int regionSizeZ) {
        this.world = world;
        this.terrainManager = terrainManager;
        this.regionSizeX = regionSizeX;
        this.regionSizeY = regionSizeY;
        this.regionSizeZ = regionSizeZ;
    }

    public void recalculateMesh(int regionX, int regionY, int regionZ, boolean force) {
        recalculateMeshForLodLevel(regionX, regionY, regionZ, force, -1);
    }

    public void recalculateMeshForLodLevel(int regionX, int regionY, int regionZ, boolean force, int lodLevel) {
        long keyOfRegion = ChunkBase.computeChunkKey(regionX, regionY, regionZ);

        RegionQueue q = queues.computeIfAbsent(keyOfRegion, k -> new RegionQueue());

        MeshCalculationTicket oldTicket = q.getLastTicket(keyOfRegion);
        MeshCalculationTicket newTicket = createRegionTicket(force, regionSizeX, regionSizeY, regionSizeZ);


        if (oldTicket == null || newTicket.isBetterThan(oldTicket)) {
            TerrainMesh mesh = renderedRegions.computeIfAbsent(keyOfRegion, k -> new TerrainMesh());
            q.addTicket(keyOfRegion, newTicket);
            q.enqueue(() -> doRecalculate(keyOfRegion, regionX, regionY, regionZ, mesh, lodLevel == -1 ? mesh.getLodLevel() : lodLevel));
        }
    }

    private void doRecalculate(long regionKey, int regionX, int regionY, int regionZ, TerrainMesh terrainMesh, int lodLevel) {
        long start = System.nanoTime();
        try {
            var result = terrainManager.getMeshPipeline().buildMesh(world, regionX, regionY, regionZ, lodLevel);
            terrainMesh.setRawBlockFaces(result.storage(), result.completeMesh(), lodLevel);
            boolean remove = terrainMesh.getAmountOfBlockFaces() == 0;
            Gdx.app.postRunnable(() -> {
                if (remove) {
                    removeMesh(regionX, regionY, regionZ);
                }
            });
        } catch (Throwable t) {
            Gdx.app.error("MeshMaster", "Error while generating mesh for region " + regionKey + " with lod level " + lodLevel, t);
        } finally {
            long elapsed = System.nanoTime() - start;
            meshTimeSumNs.addAndGet(elapsed);
            meshCount.incrementAndGet();
        }
    }

    public TerrainMesh getRegionMeshIfAvailable(int regionX, int regionY, int regionZ) {
        return renderedRegions.get(ChunkBase.computeChunkKey(regionX, regionY, regionZ));
    }

    public void removeMesh(int regionX, int regionY, int regionZ) {
        renderedRegions.remove(ChunkBase.computeChunkKey(regionX, regionY, regionZ));
    }

    public int getAmountOfQueues() {
        return queues.size();
    }

    private class RegionQueue {
        private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final Long2ObjectMap<MeshCalculationTicket> tickets = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

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

    private MeshCalculationTicket createRegionTicket(boolean forced, int regionSizeX, int regionSizeY, int regionSizeZ) {
        byte counter = 0;
        for (int i = 0; i < Direction.values().length; i++) {
            Direction dir = Direction.values()[i];
            var node = terrainManager.getTerrainGraph().getRegion(regionSizeX + dir.getOffsetX(), regionSizeY + dir.getOffsetY(), regionSizeZ + dir.getOffsetZ());
            if (node != null) {
                counter++;
            }
        }

        return new MeshCalculationTicket(forced, counter);
    }

    private record MeshCalculationTicket(boolean forced, byte presentNeighbors) {
        private boolean isBetterThan(MeshCalculationTicket other) {
            return other.forced || this.presentNeighbors > other.presentNeighbors;
        }
    }
}
