package de.verdox.voxel.client.level;

import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.chunk.ChunkRequestManager;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.chunk.calculation.BitOcclusionBasedChunkMeshCalculator;
import de.verdox.voxel.client.level.mesh.terrain.TerrainManager;
import de.verdox.voxel.client.level.mesh.terrain.TerrainRegion;
import de.verdox.voxel.client.util.LODUtil;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import it.unimi.dsi.fastutil.longs.*;
import lombok.Getter;

import java.util.*;

@Getter
public class ClientWorld extends World<ClientChunk> {
    private static final int SCALE_FACTOR = 8;

    private final Long2ObjectMap<ClientChunk> chunks = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    private final ChunkRequestManager chunkRequestManager = new ChunkRequestManager(ClientBase.client, this);
    private long minChunk;
    private long maxChunk;
    private final TerrainManager terrainManager;

    private final LongSet queuedForProcessing = LongSets.synchronize(new LongOpenHashSet());

    public ClientWorld(UUID uuid) {
        super(uuid);
        int regionSizeX = 1;
        int regionSizeY = 1;
        int regionSizeZ = 1;

        if (ClientBase.clientSettings != null) {
            regionSizeX = Math.max(regionSizeX, ClientBase.clientSettings.horizontalViewDistance / SCALE_FACTOR);
            regionSizeY = Math.max(regionSizeY, ClientBase.clientSettings.verticalViewDistance / SCALE_FACTOR);
            regionSizeZ = Math.max(regionSizeZ, ClientBase.clientSettings.horizontalViewDistance / SCALE_FACTOR);
        }

        this.terrainManager = new TerrainManager(this, new BitOcclusionBasedChunkMeshCalculator(), regionSizeX, regionSizeY, regionSizeZ);
    }

    public ClientWorld(UUID uuid, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        super(uuid, chunkSizeX, chunkSizeY, chunkSizeZ);

        int regionSizeX = 1;
        int regionSizeY = 1;
        int regionSizeZ = 1;

        if (ClientBase.clientSettings != null) {
            regionSizeX = Math.max(regionSizeX, ClientBase.clientSettings.horizontalViewDistance / SCALE_FACTOR);
            regionSizeY = Math.max(regionSizeY, ClientBase.clientSettings.verticalViewDistance / SCALE_FACTOR);
            regionSizeZ = Math.max(regionSizeZ, ClientBase.clientSettings.horizontalViewDistance / SCALE_FACTOR);
        }

        regionSizeX = 1;
        regionSizeY = 1;
        regionSizeZ = 1;

        this.terrainManager = new TerrainManager(this, new BitOcclusionBasedChunkMeshCalculator(), regionSizeX, regionSizeY, regionSizeZ);
    }

    @Override
    public ClientChunk getChunkNow(int chunkX, int chunkY, int chunkZ) {
        return getChunkNow(ChunkBase.computeChunkKey(chunkX, chunkY, chunkZ));
    }

    @Override
    public ClientChunk getChunkNow(long chunkKey) {
        return chunks.getOrDefault(chunkKey, null);
    }

    public void onCenterChange(int chunkX, int chunkY, int chunkZ) {
        this.terrainManager.setCenterChunk(chunkX, chunkY, chunkZ);
        int renderDistanceX = ClientBase.clientSettings.horizontalViewDistance;
        int renderDistanceY = ClientBase.clientSettings.verticalViewDistance;
        int renderDistanceZ = ClientBase.clientSettings.horizontalViewDistance;

        int newMinChunkX = chunkX - renderDistanceX;
        int newMinChunkY = chunkY - renderDistanceY;
        int newMinChunkZ = chunkZ - renderDistanceZ;

        int newMaxChunkX = chunkX + renderDistanceX;
        int newMaxChunkY = chunkY + renderDistanceY;
        int newMaxChunkZ = chunkZ + renderDistanceZ;

        long maxChunk = ChunkBase.computeChunkKey(newMaxChunkX, newMaxChunkY, newMaxChunkZ);
        long minChunk = ChunkBase.computeChunkKey(newMinChunkX, newMinChunkY, newMinChunkZ);

        if (this.maxChunk != maxChunk || this.minChunk != minChunk) {
            int oldMinChunkX = ChunkBase.unpackChunkX(this.minChunk);
            int oldMinChunkY = ChunkBase.unpackChunkY(this.minChunk);
            int oldMinChunkZ = ChunkBase.unpackChunkZ(this.minChunk);

            int oldMaxChunkX = ChunkBase.unpackChunkX(this.maxChunk);
            int oldMaxChunkY = ChunkBase.unpackChunkY(this.maxChunk);
            int oldMaxChunkZ = ChunkBase.unpackChunkZ(this.maxChunk);

            LongSet regionsToRebuild = new LongOpenHashSet();

            int chunkCounter = 0;
            int regionCounter = 0;
            for (long x = Math.min(oldMinChunkX, newMinChunkX); x <= Math.max(oldMaxChunkX, newMaxChunkX); x++) {
                for (long y = Math.min(oldMinChunkY, newMinChunkY); y <= Math.max(oldMaxChunkY, newMaxChunkY); y++) {
                    for (long z = Math.min(oldMinChunkZ, newMinChunkZ); z <= Math.max(oldMaxChunkZ, newMaxChunkZ); z++) {

                        if (x < newMinChunkX || x > newMaxChunkX
                                || y < newMinChunkY || y > newMaxChunkY
                                || z < newMinChunkZ || z > newMaxChunkZ) {
                            long chunkOutOfViewDistance = ChunkBase.computeChunkKey((int) x, (int) y, (int) z);
                            ClientChunk clientChunk = chunks.get(chunkOutOfViewDistance);
                            if (clientChunk != null) {
                                chunkCounter++;
                                chunks.remove(chunkOutOfViewDistance);

                                regionsToRebuild.add(terrainManager.getMeshPipeline().getRegionBounds()
                                                                   .getRegionKeyFromChunk((int) x, (int) y, (int) z));
                            }
                        }
                    }
                }
            }

            for (long regionToRebuild : regionsToRebuild) {
                int regionX = ChunkBase.unpackChunkX(regionToRebuild);
                int regionY = ChunkBase.unpackChunkY(regionToRebuild);
                int regionZ = ChunkBase.unpackChunkZ(regionToRebuild);
                TerrainRegion terrainRegion = terrainManager.getRegion(regionX, regionY, regionZ);
                if (terrainRegion != null) {
                    terrainManager.updateMesh(terrainRegion, false);
                    regionCounter++;
                }
            }

            this.maxChunk = maxChunk;
            this.minChunk = minChunk;
            System.out.println("Removed " + chunkCounter + " chunks in " + regionCounter + " regions.");
        }

    }

    public boolean isQueuedForProcessing(int chunkX, int chunkY, int chunkZ) {
        return queuedForProcessing.contains(ChunkBase.computeChunkKey(chunkX, chunkY, chunkZ));
    }

    public void queueChunkForProcessing(ClientChunk chunk) {
        queuedForProcessing.add(ChunkBase.computeChunkKey(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ()));
        addChunk(chunk);
        chunkRequestManager.notifyChunkReceived(chunk.getChunkKey());
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
        terrainManager.addChunk(chunk);
    }

    @Override
    protected void onRemoveChunk(ClientChunk chunk) {
        chunks.remove(chunk.getChunkKey());
        terrainManager.removeChunk(chunk);
    }

    @Override
    protected void onChunkUpdate(ClientChunk chunk, byte localX, byte localY, byte localZ, boolean wasEmptyBefore) {
        terrainManager.afterChunkUpdate(chunk, wasEmptyBefore);
    }

    @Override
    public ClientChunk constructChunkObject(int chunkX, int chunkY, int chunkZ) {
        return new ClientChunk(this, chunkX, chunkY, chunkZ);
    }

    public Collection<ClientChunk> getLoadedChunks() {
        return chunks.values();
    }

    public ClientChunk getChunk(int chunkX, int chunkY, int chunkZ) {
        return chunks.getOrDefault(ChunkBase.computeChunkKey(chunkX, chunkY, chunkZ), null);
    }

    public int computeLodLevel(
            int centerRegionX, int centerRegionY, int centerRegionZ,
            int targetRegionX, int targetRegionY, int targetRegionZ
    ) {


        return LODUtil.computeLodLevel(
                terrainManager.getMeshPipeline().getRegionBounds(),
                ClientBase.clientSettings.horizontalViewDistance, ClientBase.clientSettings.verticalViewDistance, ClientBase.clientSettings.horizontalViewDistance,
                getChunkSizeX(),
                getChunkSizeY(),
                getChunkSizeZ(),
                centerRegionX, centerRegionY, centerRegionZ,
                targetRegionX, targetRegionY, targetRegionZ,
                LODUtil.getMaxLod(this)
        );
    }
}
