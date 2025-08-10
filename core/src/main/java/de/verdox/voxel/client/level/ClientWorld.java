package de.verdox.voxel.client.level;

import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.GameSession;
import de.verdox.voxel.client.level.mesh.calculation.region.BufferedRegionMeshCalculator;
import de.verdox.voxel.client.play.multiplayer.ChunkRequestManager;
import de.verdox.voxel.client.level.mesh.calculation.chunk.BitOcclusionBasedChunkMeshCalculator;
import de.verdox.voxel.client.level.mesh.TerrainManager;
import de.verdox.voxel.client.level.mesh.TerrainRegion;
import de.verdox.voxel.server.level.chunk.ChunkMap;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.world.DelegateWorld;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import it.unimi.dsi.fastutil.longs.*;
import lombok.Getter;

@Getter
public class ClientWorld extends DelegateWorld {
    private static final int SCALE_FACTOR = 8;
    private long minChunk;
    private long maxChunk;
    private final TerrainManager terrainManager;
    private final ChunkRequestManager chunkRequestManager = new ChunkRequestManager(this);

    public ClientWorld(World owner) {
        super(owner);
        int regionSizeX = 1;
        int regionSizeY = 1;
        int regionSizeZ = 1;

        if (ClientBase.clientSettings != null) {
            regionSizeX = Math.max(regionSizeX, ClientBase.clientSettings.horizontalViewDistance / SCALE_FACTOR);
            regionSizeY = Math.max(regionSizeY, ClientBase.clientSettings.verticalViewDistance / SCALE_FACTOR);
            regionSizeZ = Math.max(regionSizeZ, ClientBase.clientSettings.horizontalViewDistance / SCALE_FACTOR);
        }

        this.terrainManager = new TerrainManager(this, new BufferedRegionMeshCalculator(), new BitOcclusionBasedChunkMeshCalculator(), regionSizeX, regionSizeY, regionSizeZ);
    }

    @Override
    public boolean hasNeighborsToAllSides(Chunk chunk) {
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            int rX = chunk.getChunkX() + direction.getOffsetX();
            int rY = chunk.getChunkY() + direction.getOffsetY();
            int rZ = chunk.getChunkZ() + direction.getOffsetZ();

            int chunkOffsetX = Math.abs(terrainManager.getCenterChunkX() - rX);
            int chunkOffsetY = Math.abs(terrainManager.getCenterChunkY() - rY);
            int chunkOffsetZ = Math.abs(terrainManager.getCenterChunkZ() - rZ);

            if (chunkOffsetX > ClientBase.clientSettings.horizontalViewDistance || chunkOffsetY > ClientBase.clientSettings.verticalViewDistance || chunkOffsetZ > ClientBase.clientSettings.horizontalViewDistance) {
                continue;
            }

            if (getChunkNeighborNow(chunk, direction) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Chunk getChunkNow(int chunkX, int chunkY, int chunkZ) {
        return terrainManager.getChunkNow(chunkX, chunkY, chunkZ);
    }

    @Override
    public Chunk getChunkNow(long chunkKey) {
        return terrainManager.getChunkNow(chunkKey);
    }

    @Override
    public ChunkMap getChunkMap() {
        return getOwner().getChunkMap();
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

        long maxChunk = Chunk.computeChunkKey(newMaxChunkX, newMaxChunkY, newMaxChunkZ);
        long minChunk = Chunk.computeChunkKey(newMinChunkX, newMinChunkY, newMinChunkZ);

        if (this.maxChunk != maxChunk || this.minChunk != minChunk) {
            int oldMinChunkX = Chunk.unpackChunkX(this.minChunk);
            int oldMinChunkY = Chunk.unpackChunkY(this.minChunk);
            int oldMinChunkZ = Chunk.unpackChunkZ(this.minChunk);

            int oldMaxChunkX = Chunk.unpackChunkX(this.maxChunk);
            int oldMaxChunkY = Chunk.unpackChunkY(this.maxChunk);
            int oldMaxChunkZ = Chunk.unpackChunkZ(this.maxChunk);

            LongSet regionsToRebuild = new LongOpenHashSet();

            int chunkCounter = 0;
            int regionCounter = 0;
            for (long x = Math.min(oldMinChunkX, newMinChunkX); x <= Math.max(oldMaxChunkX, newMaxChunkX); x++) {
                for (long y = Math.min(oldMinChunkY, newMinChunkY); y <= Math.max(oldMaxChunkY, newMaxChunkY); y++) {
                    for (long z = Math.min(oldMinChunkZ, newMinChunkZ); z <= Math.max(oldMaxChunkZ, newMaxChunkZ); z++) {

                        if (x < newMinChunkX || x > newMaxChunkX
                                || y < newMinChunkY || y > newMaxChunkY
                                || z < newMinChunkZ || z > newMaxChunkZ) {
                            long chunkOutOfViewDistance = Chunk.computeChunkKey((int) x, (int) y, (int) z);

                            Chunk clientChunk = getChunkNow(chunkOutOfViewDistance);
                            if (clientChunk != null) {
                                chunkCounter++;
                                terrainManager.removeChunk(clientChunk);
                                regionsToRebuild.add(terrainManager.getBounds().getRegionKeyFromChunk((int) x, (int) y, (int) z));
                            }
                        }
                    }
                }
            }

            for (long regionToRebuild : regionsToRebuild) {
                int regionX = Chunk.unpackChunkX(regionToRebuild);
                int regionY = Chunk.unpackChunkY(regionToRebuild);
                int regionZ = Chunk.unpackChunkZ(regionToRebuild);
                TerrainRegion terrainRegion = terrainManager.getRegion(regionX, regionY, regionZ);
                if (terrainRegion != null) {
                    terrainManager.updateMesh(terrainRegion, false);
                    regionCounter++;
                }
            }

            this.maxChunk = maxChunk;
            this.minChunk = minChunk;
        }

    }

    public BlockBase getBlockAt(int globalX, int globalY, int globalZ) {
        int chunkX = Chunk.chunkX(this, globalX);
        int chunkY = Chunk.chunkY(this, globalY);
        int chunkZ = Chunk.chunkZ(this, globalZ);

        Chunk chunk = getChunkNow(chunkX, chunkY, chunkZ);
        if (chunk == null) {
            return Blocks.AIR;
        }

        int localX = chunk.localX(globalX);
        int localY = chunk.localY(globalY);
        int localZ = chunk.localZ(globalZ);

        return chunk.getBlockAt(localX, localY, localZ);
    }

    @Override
    public void notifyAddChunk(Chunk chunk) {
        GameSession.postRunnable(() -> {
            terrainManager.addChunk(chunk);
            chunkRequestManager.notifyChunkReceived(chunk.getChunkKey());
        });
    }

    @Override
    public void notifyRemoveChunk(Chunk chunk) {
        GameSession.postRunnable(() -> {
            terrainManager.removeChunk(chunk);
        });
    }

    @Override
    public void notifyChunkUpdate(Chunk chunk, byte localX, byte localY, byte localZ, boolean wasEmptyBefore) {
        GameSession.postRunnable(() -> {
            terrainManager.afterChunkUpdate(chunk, wasEmptyBefore);
        });
    }
}
