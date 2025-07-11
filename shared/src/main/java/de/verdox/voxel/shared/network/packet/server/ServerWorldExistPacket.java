package de.verdox.voxel.shared.network.packet.server;

import de.verdox.voxel.shared.level.World;

import java.util.UUID;

public class ServerWorldExistPacket {
    public UUID uuid;
    public int minChunkY;
    public int maxChunkY;
    public byte chunkSizeX;
    public byte chunkSizeY;
    public byte chunkSizeZ;

    public ServerWorldExistPacket(UUID uuid, int minChunkY, int maxChunkY, byte chunkSizeX, byte chunkSizeY, byte chunkSizeZ) {
        this.uuid = uuid;
        this.minChunkY = minChunkY;
        this.maxChunkY = maxChunkY;
        this.chunkSizeX = chunkSizeX;
        this.chunkSizeY = chunkSizeY;
        this.chunkSizeZ = chunkSizeZ;
    }

    public ServerWorldExistPacket() {
    }

    public static ServerWorldExistPacket fromWorld(World world) {
        return new ServerWorldExistPacket(world.getUuid(), world.getMinChunkY(), world.getMaxChunkY(), world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ());
    }
}
