package de.verdox.voxel.shared.level.chunk;

import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.util.Delegate;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

import java.util.List;

public abstract class DelegateChunk implements Chunk, Delegate<Chunk> {
    @Getter
    protected final Chunk owner;

    public DelegateChunk(Chunk owner) {
        this.owner = owner;
        owner.subscribe(this);
    }

    @Override
    public void init() {
        owner.init();
    }

    @Override
    public int getChunkX() {
        return owner.getChunkX();
    }

    @Override
    public int getChunkY() {
        return owner.getChunkY();
    }

    @Override
    public int getChunkZ() {
        return owner.getChunkZ();
    }

    @Override
    public ChunkBlockPalette getChunkBlockPalette() {
        return owner.getChunkBlockPalette();
    }

    @Override
    public HeightMap getHeightMap() {
        return owner.getHeightMap();
    }

    @Override
    public DepthMap getDepthMap() {
        return owner.getDepthMap();
    }

    @Override
    public ChunkLightData getChunkLightData() {
        return owner.getChunkLightData();
    }

    @Override
    public World getWorld() {
        return owner.getWorld();
    }

    @Override
    public boolean isEmpty() {
        return owner.isEmpty();
    }

    @Override
    public boolean hasNeighborsToAllSides() {
        return owner.hasNeighborsToAllSides();
    }

    @Override
    public <SELF extends Chunk> SELF getNeighborChunk(Direction direction) {
        return owner.getNeighborChunk(direction);
    }

    @Override
    public int getSizeX() {
        return owner.getSizeX();
    }

    @Override
    public int getSizeY() {
        return owner.getSizeY();
    }

    @Override
    public int getSizeZ() {
        return owner.getSizeZ();
    }

    public void notifySetBlock(BlockBase newBlock, int localX, int localY, int localZ) {

    }

    @Override
    public void subscribe(DelegateChunk delegate) {

    }

    @Override
    public List<DelegateChunk> getDelegates() {
        return List.of();
    }
}
