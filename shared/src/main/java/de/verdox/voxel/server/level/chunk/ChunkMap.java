package de.verdox.voxel.server.level.chunk;

import de.verdox.voxel.shared.level.world.LevelWorld;
import de.verdox.voxel.shared.level.chunk.Chunk;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Getter
public class ChunkMap {
    private final Long2ObjectMap<Chunk> chunks = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    private final LevelWorld world;


    public ChunkMap(LevelWorld world) {
        this.world = world;
    }

    public CompletableFuture<Chunk> getOrCreateChunkAsync(int chunkX, int chunkY, int chunkZ, Consumer<Chunk> whenDone) {
        return getChunk(chunkX, chunkY, chunkZ)
                .map(chunk -> CompletableFuture.completedFuture(chunk)
                                               .whenComplete((chunk1, throwable) -> whenDone.accept(chunk1)))
                .orElseGet(() -> getWorld().getWorldGenerator()
                                           .requestChunkGeneration(chunkX, chunkY, chunkZ, whenDone));
    }

    public Optional<Chunk> getChunk(int chunkX, int chunkY, int chunkZ) {
        long chunkKey = Chunk.computeChunkKey(chunkX, chunkY, chunkZ);
        if (!chunks.containsKey(chunkKey)) {
            return Optional.empty();
        }
        Chunk chunk = chunks.get(chunkKey);
        return Optional.of(chunk);
    }

    public void saveChunkAfterGeneration(Chunk gameChunk) {
        chunks.put(gameChunk.getChunkKey(), gameChunk);
        world.addChunk(gameChunk);
        synchronized (world.getGrid()) {
            world.getGrid().addOrUpdateChunk(gameChunk);
        }
    }

    public boolean unloadChunk(int chunkX, int chunkY, int chunkZ) {
        long chunkKey = Chunk.computeChunkKey(chunkX, chunkY, chunkZ);
        if (chunks.containsKey(chunkKey)) {
            Chunk removed = chunks.remove(chunkKey);
            world.removeChunk(removed);
            return true;
        }
        return false;
    }
}
