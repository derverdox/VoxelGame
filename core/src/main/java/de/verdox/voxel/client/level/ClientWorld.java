package de.verdox.voxel.client.level;

import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.chunk.ChunkRequestManager;
import de.verdox.voxel.client.level.chunk.ChunkVisibilityGraph;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.region.strategy.ChunkRenderRegionStrategy;
import de.verdox.voxel.client.level.mesh.region.strategy.WorldCenteredRegionStrategy;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ClientWorld extends World<ClientChunk> {
    private final Map<Long, ClientChunk> chunks = new ConcurrentHashMap<>();
    private final ChunkVisibilityGraph chunkVisibilityGraph = new ChunkVisibilityGraph(this);
    private final ChunkRequestManager chunkRequestManager = new ChunkRequestManager(ClientBase.client, this);
    private final ChunkRenderRegionStrategy renderRegionStrategy = new WorldCenteredRegionStrategy(this, ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance);

    public ClientWorld(UUID uuid) {
        super(uuid);
    }

    public ClientWorld(UUID uuid, int minChunkY, int maxChunkY, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        super(uuid, minChunkY, maxChunkY, chunkSizeX, chunkSizeY, chunkSizeZ);

        //TODO: Unload chunks that are not in range of the player anymore.
    }

    public void queueChunkForProcessing(ClientChunk chunk) {
        addChunk(chunk);
        chunkRequestManager.notifyChunkReceived(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
    }

    public BlockBase getBlockAt(int globalX, int globalY, int globalZ) {
        int chunkX = ChunkBase.chunkX(this, globalX);
        int chunkY = ChunkBase.chunkY(this, globalY);
        int chunkZ = ChunkBase.chunkZ(this, globalZ);

        ClientChunk clientChunk = getChunk(chunkX, chunkY, chunkZ);
        if (clientChunk == null) {
            return Blocks.AIR;
        }

        int localX = clientChunk.localX(globalX);
        int localY = clientChunk.localY(globalY);
        int localZ = clientChunk.localZ(globalZ);

        return clientChunk.getBlockAt(localX, localY, localZ);
    }

    @Override
    protected void onAddChunk(ClientChunk chunk) {
        chunks.put(chunk.getChunkKey(), chunk);
        chunkVisibilityGraph.chunkLoaded(chunk);
        getRenderRegionStrategy().chunkLoad(chunk);
    }

    @Override
    protected void onRemoveChunk(ClientChunk chunk) {
        chunks.remove(chunk.getChunkKey());
        chunkVisibilityGraph.chunkUnloaded(chunk);
        getRenderRegionStrategy().chunkUnload(chunk);
    }

    @Override
    protected void onChunkUpdate(ClientChunk chunk, byte localX, byte localY, byte localZ) {
        chunkVisibilityGraph.blockUpdateInChunk(chunk);
        getRenderRegionStrategy().markDirty(chunk);
    }

    public Collection<ClientChunk> getLoadedChunks() {
        return chunks.values();
    }

    public ClientChunk getChunk(int chunkX, int chunkY, int chunkZ) {
        return chunks.getOrDefault(ChunkBase.computeChunkKey(chunkX, chunkY, chunkZ), null);
    }

    public ClientChunk getChunkForPosition(Vector3 position) {
        return chunks.getOrDefault(ChunkBase.computeChunkKey(
                ChunkBase.chunkX(this, (int) position.x),
                ChunkBase.chunkY(this, (int) position.y),
                ChunkBase.chunkZ(this, (int) position.z)
            )
            , null);
    }
}
