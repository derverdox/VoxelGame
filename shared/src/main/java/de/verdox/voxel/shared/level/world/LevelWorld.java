package de.verdox.voxel.shared.level.world;

import de.verdox.voxel.server.level.chunk.ChunkMap;
import de.verdox.voxel.server.level.generator.BenchmarkNoiseChunkGenerator;
import de.verdox.voxel.server.level.generator.WorldGenerator;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class LevelWorld implements World {
    protected final UUID uuid;
    protected final byte chunkSizeX;
    protected final byte chunkSizeY;
    protected final byte chunkSizeZ;

    private final WorldGenerator worldGenerator = new WorldGenerator(this, new BenchmarkNoiseChunkGenerator(), 1);
    private final ChunkMap chunkMap = new ChunkMap(this);

    private final List<DelegateWorld> delegates = new ObjectArrayList<>();

    @Getter
    private final WorldHeightMap worldHeightMap = new WorldHeightMap(getChunkSizeY());

    public LevelWorld(UUID uuid) {
        this.uuid = uuid;

        chunkSizeX = 16;
        chunkSizeY = 16;
        chunkSizeZ = 16;
    }

    public LevelWorld(UUID uuid, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        this.uuid = uuid;
        this.chunkSizeX = chunkSizeX;
        this.chunkSizeY = chunkSizeY;
        this.chunkSizeZ = chunkSizeZ;
    }

    public Chunk getChunkNeighborNow(int chunkX, int chunkY, int chunkZ, Direction direction) {
        return getChunkNow(chunkX + direction.getOffsetX(), chunkY + direction.getOffsetY(), chunkZ + direction.getOffsetZ());
    }

    public Chunk getChunkNeighborNow(Chunk chunk, Direction direction) {
        return getChunkNeighborNow(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), direction);
    }

    public boolean hasNeighborsToAllSides(Chunk chunk) {
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            if (getChunkNeighborNow(chunk, direction) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Chunk getChunkNow(int chunkX, int chunkY, int chunkZ) {
        //TODO:
        return null;
    }

    @Override
    public Chunk getChunkNow(long chunkKey) {
        //TODO:
        return null;
    }
}
