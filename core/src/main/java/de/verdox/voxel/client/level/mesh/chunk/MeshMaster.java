package de.verdox.voxel.client.level.mesh.chunk;

import com.badlogic.gdx.Gdx;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.chunk.calculation.ChunkMeshCalculator;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Direction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Deprecated
public class MeshMaster implements DebuggableOnScreen {

    private final Map<Long, ChunkMesh> renderedChunks = new HashMap<>();
    private final Map<Long, ChunkQueue> queues = new HashMap<>();
    private final ExecutorService executor;
    private final ChunkMeshCalculator chunkMeshCalculator;

    // Neue Felder zum Tracken der Zeiten
    private final AtomicLong meshTimeSumNs = new AtomicLong(0);
    private final AtomicLong meshCount = new AtomicLong(0);

    public MeshMaster(ChunkMeshCalculator chunkMeshCalculator) {
        this.chunkMeshCalculator = chunkMeshCalculator;
        this.executor = Executors.newFixedThreadPool(4);
    }

    public void clear() {
        renderedChunks.clear();
        queues.clear();
        meshTimeSumNs.set(0);
        meshCount.set(0);
    }

    public void recalculateMesh(ClientChunk gameChunk) {
        if (gameChunk.isEmpty()) return;
        long key = gameChunk.getChunkKey();
        ChunkQueue q = queues.computeIfAbsent(key, k -> new ChunkQueue());

        ChunkMeshCalculationTicket oldTicket = q.getLastTicket(key);
        ChunkMeshCalculationTicket newTicket = ChunkMeshCalculationTicket.fromChunk(gameChunk);

        if (oldTicket == null || newTicket.isBetterThan(oldTicket)) {
            ChunkMesh chunkMesh = renderedChunks.computeIfAbsent(key, k -> new ChunkMesh(chunkMeshCalculator));
            q.addTicket(key, ChunkMeshCalculationTicket.fromChunk(gameChunk));
            q.enqueue(() -> doRecalculate(key, gameChunk, chunkMesh));
        }
    }

    private void doRecalculate(long chunkKey, ClientChunk gameChunk, ChunkMesh chunkMesh) {
        long start = System.nanoTime();
        try {
            BlockFaceStorage blockFaces = chunkMeshCalculator.calculateChunkMesh(gameChunk);
            blockFaces = blockFaces.applyGreedyMeshing();
            chunkMesh.setRawBlockFaces(blockFaces, true);
            Gdx.app.postRunnable(() -> gameChunk.getWorld().getRenderRegionStrategy().markDirty(gameChunk));
        } catch (Throwable t) {
            Gdx.app.error("MeshMaster", "Error while generating mesh for chunk " + chunkKey, t);
        } finally {
            long elapsed = System.nanoTime() - start;
            meshTimeSumNs.addAndGet(elapsed);
            meshCount.incrementAndGet();
        }
    }

    /**
     * Liefert null, wenn noch nicht fertig — oder das fertige Mesh
     **/
    public ChunkMesh getChunkMeshIfAvailable(int x, int y, int z) {
        return renderedChunks.get(ChunkBase.computeChunkKey(x, y, z));
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        long count = meshCount.get();
        if (count > 0) {
            double avgMs = meshTimeSumNs.get() / (count * 1_000_000.0);
            debugScreen.addDebugTextLine(
                String.format("Mesh avg: %.2f ms  (samples: %d)", avgMs, count)
            );
        } else {
            debugScreen.addDebugTextLine("Mesh avg: –");
        }
    }

    private record ChunkMeshCalculationTicket(long chunkDataChangeVersion, boolean[] neighborPresent, byte presentNeighbors) {
        private static ChunkMeshCalculationTicket fromChunk(ClientChunk clientChunk) {
            byte count = 0;
            boolean[] present = new boolean[Direction.values().length];
            for (int i = 0; i < Direction.values().length; i++) {
                Direction direction = Direction.values()[i];
                var neighbor = clientChunk.getWorld().getChunk(clientChunk.getChunkX() + direction.getOffsetX(), clientChunk.getChunkY() + direction.getOffsetY(), clientChunk.getChunkZ() + direction.getOffsetZ());
                if (neighbor != null) {
                    present[i] = true;
                    count++;
                }

            }
            return new ChunkMeshCalculationTicket(clientChunk.getChunkBlockPalette().getLocalDataChangeVersion(), present, count);
        }

        private boolean isBetterThan(ChunkMeshCalculationTicket other) {
            return this.chunkDataChangeVersion > other.chunkDataChangeVersion || this.presentNeighbors > other.presentNeighbors;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ChunkMeshCalculationTicket that = (ChunkMeshCalculationTicket) o;
            return chunkDataChangeVersion == that.chunkDataChangeVersion && Arrays.equals(neighborPresent, that.neighborPresent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkDataChangeVersion, Arrays.hashCode(neighborPresent));
        }
    }

    /**
     * Serielle Queue, die Tasks pro Chunk nacheinander ausführt
     */
    private class ChunkQueue {
        private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final Map<Long, ChunkMeshCalculationTicket> tickets = new ConcurrentHashMap<>();

        void enqueue(Runnable task) {
            pending.add(task);
            scheduleNext();
        }

        public ChunkMeshCalculationTicket getLastTicket(long chunkKey) {
            return tickets.getOrDefault(chunkKey, null);
        }

        public void addTicket(long chunkKey, ChunkMeshCalculationTicket ticket) {
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
}

