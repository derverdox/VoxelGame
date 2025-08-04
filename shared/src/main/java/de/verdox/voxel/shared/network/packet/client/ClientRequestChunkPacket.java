package de.verdox.voxel.shared.network.packet.client;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ClientRequestChunkPacket {
    public UUID world;
    public int chunkX;
    public int chunkY;
    public int chunkZ;

    public ClientRequestChunkPacket(UUID world, int chunkX, int chunkY, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }

    public ClientRequestChunkPacket() {
    }
}
