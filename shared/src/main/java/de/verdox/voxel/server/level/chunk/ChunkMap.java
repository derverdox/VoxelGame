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
    private final Long2ObjectMap<MinMaxY> columnHeights = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    private final LevelWorld world;

    public ChunkMap(LevelWorld world) {
        this.world = world;
    }

    public CompletableFuture<Chunk> getOrCreateChunkAsync(int chunkX, int chunkY, int chunkZ, Consumer<Chunk> whenDone) {
        return getChunk(chunkX, chunkY, chunkZ)
                .map(chunk -> CompletableFuture.completedFuture(chunk).whenComplete((chunk1, throwable) -> whenDone.accept(chunk1)))
                .orElseGet(() -> getWorld().getWorldGenerator().requestChunkGeneration(chunkX, chunkY, chunkZ, whenDone));
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

        long columnKey = computeColumnKey(gameChunk.getChunkX(), gameChunk.getChunkZ());
        columnHeights.compute(columnKey, (key, minMax) -> {
            if (minMax == null) {
                MinMaxY m = new MinMaxY();
                m.minY = gameChunk.getChunkY();
                m.maxY = gameChunk.getChunkY();
                return m;
            } else {
                if (gameChunk.getChunkY() < minMax.minY) minMax.minY = gameChunk.getChunkY();
                if (gameChunk.getChunkY() > minMax.maxY) minMax.maxY = gameChunk.getChunkY();
                return minMax;
            }
        });
        world.addChunk(gameChunk);
    }

    public boolean unloadChunk(int chunkX, int chunkY, int chunkZ) {
        long chunkKey = Chunk.computeChunkKey(chunkX, chunkY, chunkZ);
        if (chunks.containsKey(chunkKey)) {
            Chunk removed = chunks.remove(chunkKey);

            long columnKey = computeColumnKey(chunkX, chunkZ);
            MinMaxY recalculated = recomputeColumnHeight(chunkX, chunkZ);
            if (recalculated != null) {
                columnHeights.put(columnKey, recalculated);
            } else {
                columnHeights.remove(columnKey);
            }
            world.removeChunk(removed);
            return true;
        }
        return false;
    }

    /**
     * Wird von einem Chunk aufgerufen, wenn sich dessen Heightmap geändert hat.
     * Hier kannst du Logik einfügen, falls du darauf reagieren willst.
     */
    public void notifyHeightmapChange(Chunk gameChunk) {
        if (gameChunk.isEmpty()) {
            unloadChunk(gameChunk.getChunkX(), gameChunk.getChunkY(), gameChunk.getChunkZ());
        } else {
            long columnKey = computeColumnKey(gameChunk.getChunkX(), gameChunk.getChunkZ());
            columnHeights.compute(columnKey, (key, minMax) -> {
                if (minMax == null) {
                    MinMaxY m = new MinMaxY();
                    m.minY = gameChunk.getChunkY();
                    m.maxY = gameChunk.getChunkY();
                    return m;
                } else {
                    if (gameChunk.getChunkY() < minMax.minY) minMax.minY = gameChunk.getChunkY();
                    if (gameChunk.getChunkY() > minMax.maxY) minMax.maxY = gameChunk.getChunkY();
                    return minMax;
                }
            });
        }
    }

    /**
     * Hilfsmethode: Liefert das Min/Max Y einer Chunk-Säule.
     */
    public Optional<MinMaxY> getMinMaxChunkYInChunkColumn(int chunkX, int chunkZ) {
        long columnKey = computeColumnKey(chunkX, chunkZ);
        return Optional.ofNullable(columnHeights.get(columnKey));
    }

    /**
     * Erneutes Berechnen der minY und maxY, falls ein Chunk entladen wurde.
     */
    private MinMaxY recomputeColumnHeight(int chunkX, int chunkZ) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
            Chunk chunk = entry.getValue();
            if (chunk.getChunkX() == chunkX && chunk.getChunkZ() == chunkZ) {
                int y = chunk.getChunkY();
                if (y < min) min = y;
                if (y > max) max = y;
            }
        }
        if (min == Integer.MAX_VALUE) {
            return null; // Keine Chunks mehr in dieser Säule
        }
        MinMaxY m = new MinMaxY();
        m.minY = min;
        m.maxY = max;
        return m;
    }

    /**
     * Liefert den Schlüssel für eine Chunk-Säule (X,Z).
     */
    public static long computeColumnKey(int chunkX, int chunkZ) {
        return (((long) chunkX) & 0xFFFFFFFFL) | ((((long) chunkZ) & 0xFFFFFFFFL) << 32);
    }

    /**
     * Hilfsklasse für minY und maxY.
     */
    public static class MinMaxY {
        public int minY;
        public int maxY;

        public MinMaxY(int minY, int maxY) {
            this.minY = minY;
            this.maxY = maxY;
        }

        public MinMaxY() {
        }
    }
}
