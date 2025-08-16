package de.verdox.voxel.shared.level.world;

import de.verdox.voxel.server.level.chunk.grid.SparseOTChunkGrid;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Delegate;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

public abstract class DelegateWorld implements World, Delegate<World> {
    @Getter
    private final World owner;

    public DelegateWorld(World owner) {
        this.owner = owner;
        owner.subscribe(this);
    }

    @Override
    public SparseOTChunkGrid getGrid() {
        return owner.getGrid();
    }

    @Override
    public UUID getUuid() {
        return this.owner.getUuid();
    }

    @Override
    public byte getChunkSizeX() {
        return this.owner.getChunkSizeX();
    }

    @Override
    public byte getChunkSizeY() {
        return this.owner.getChunkSizeY();
    }

    @Override
    public byte getChunkSizeZ() {
        return this.owner.getChunkSizeZ();
    }

    @Override
    public WorldHeightMap getWorldHeightMap() {
        return this.owner.getWorldHeightMap();
    }

    @Override
    public Chunk getChunkNow(int chunkX, int chunkY, int chunkZ) {
        return this.owner.getChunkNow(chunkX, chunkY, chunkZ);
    }

    @Override
    public Chunk getChunkNow(long chunkKey) {
        return this.owner.getChunkNow(chunkKey);
    }

    @Override
    public void subscribe(DelegateWorld delegate) {

    }

    @Override
    public List<DelegateWorld> getDelegates() {
        return List.of();
    }

    public void notifyAddChunk(Chunk chunk) {

    }

    public void notifyRemoveChunk(Chunk chunk) {

    }

    public void notifyChunkUpdate(Chunk chunk, byte localX, byte localY, byte localZ, boolean wasEmptyBefore) {

    }
}
