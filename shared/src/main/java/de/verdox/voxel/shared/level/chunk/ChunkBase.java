package de.verdox.voxel.shared.level.chunk;

import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import de.verdox.voxel.shared.util.Direction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.List;

@Getter
public class ChunkBase implements Box, Chunk {
    private final ChunkBlockPalette chunkBlockPalette;
    private final HeightMap heightMap;
    private final DepthMap depthMap;
    private final ChunkLightData chunkLightData;

    private final World world;
    private final int chunkX, chunkY, chunkZ;

    private final List<DelegateChunk> delegates = new ObjectArrayList<>();

    public ChunkBase(World world, int chunkX, int chunkY, int chunkZ) {
        this(world, chunkX, chunkY, chunkZ, new ChunkBlockPalette(Blocks.AIR.findKey()), new HeightMap(), new DepthMap(), new ChunkLightData());
    }

    private ChunkBase(World world, int chunkX, int chunkY, int chunkZ, ChunkBlockPalette chunkBlockPalette, HeightMap heightMap, DepthMap depthMap, ChunkLightData chunkLightData) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.chunkBlockPalette = chunkBlockPalette;
        this.heightMap = heightMap;
        this.depthMap = depthMap;
        this.chunkLightData = chunkLightData;

        this.chunkBlockPalette.setOwner(this);
        this.heightMap.setOwner(this);
        this.depthMap.setOwner(this);
        this.chunkLightData.setOwner(this);
    }

    @Override
    public void init() {

    }

    @Override
    public boolean isEmpty() {
        return this.chunkBlockPalette.getPaletteSize() == 1;
    }

    @Override
    public boolean hasNeighborsToAllSides() {
        return getWorld().hasNeighborsToAllSides(this);
    }

    @Override
    public <SELF extends Chunk> SELF getNeighborChunk(Direction direction) {
        return (SELF) getWorld().getChunkNeighborNow(this, direction);
    }

    @Override
    public String toString() {
        return "{" + getChunkX() + ", " + getChunkY() + ", " + getChunkZ() + "}";
    }

    @Override
    public int getSizeX() {
        return getWorld().getChunkSizeX();
    }

    @Override
    public int getSizeY() {
        return getWorld().getChunkSizeY();
    }

    @Override
    public int getSizeZ() {
        return getWorld().getChunkSizeZ();
    }

    @Override
    public void subscribe(DelegateChunk delegate) {
        delegates.add(delegate);
    }
}
