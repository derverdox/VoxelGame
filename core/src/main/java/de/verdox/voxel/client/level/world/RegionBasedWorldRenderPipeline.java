package de.verdox.voxel.client.level.world;

import de.verdox.voxel.client.level.ClientWorld;

public class RegionBasedWorldRenderPipeline extends WorldRenderPipeline {
    @Override
    public void onChangeChunk(ClientWorld world, int newChunkX, int newChunkY, int newChunkZ) {
        world.getRenderRegionStrategy().onChangeCameraCenter(world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ(), newChunkX, newChunkY, newChunkZ);
    }
}
