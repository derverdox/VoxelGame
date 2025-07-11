package de.verdox.voxel.server.level.generator;

import de.verdox.voxel.server.level.ServerWorld;
import de.verdox.voxel.server.level.chunk.ServerChunk;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The parent class that handles all tickets for chunk generation and unloading. Also handles parallelism
 */
public class WorldGenerator implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(WorldGenerator.class.getSimpleName());

    private final ServerWorld world;
    private final ChunkGenerator generator;
    private final ExecutorService executor;
    private final ConcurrentMap<Long, CompletableFuture<ServerChunk>> chunkFutures = new ConcurrentHashMap<>();

    /**
     * @param world       die Welt, in der die Chunks erstellt werden
     * @param generator   euer ChunkGenerator (implementiert noise + surface)
     * @param threadCount Anzahl Worker-Threads (z.B. Runtime.getRuntime().availableProcessors())
     */
    public WorldGenerator(ServerWorld world, ChunkGenerator generator, int threadCount) {
        this.world = world;
        this.generator = generator;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * Forder die Erzeugung eines Chunks asynchron an.
     * Duplicate Calls sind nicht vorgesehen, daher kein Check auf vorhandenen Future.
     */
    public CompletableFuture<ServerChunk> requestChunkGeneration(int chunkX, int chunkY, int chunkZ) {
        CompletableFuture<ServerChunk> future = new CompletableFuture<>();
        long chunkKey = ServerChunk.computeChunkKey(chunkX, chunkY, chunkZ);
        if (chunkFutures.containsKey(chunkKey)) {
            return chunkFutures.get(chunkKey);
        }

        chunkFutures.put(chunkKey, future);

        executor.submit(() -> {
            try {
                ServerChunk gameChunk = new ServerChunk(world, chunkX, chunkY, chunkZ);

                generator.generateNoise(gameChunk);
                generator.generateSurfaceBlocks(gameChunk);

                world.getChunkMap().saveChunkAfterGeneration(gameChunk);
                future.complete(gameChunk);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Error generating chunk " + chunkX + ", " + chunkY + ", " + chunkZ, t);
                future.completeExceptionally(t);
            }
            finally {
                chunkFutures.remove(chunkKey);
            }
        });
        return future;
    }

    /**
     * Sauber herunterfahren, z.B. beim Stoppen der Anwendung.
     */
    @Override
    public void close() {
        executor.shutdown();
    }
}

