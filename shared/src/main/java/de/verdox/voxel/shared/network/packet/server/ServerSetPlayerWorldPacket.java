package de.verdox.voxel.shared.network.packet.server;

import java.util.UUID;

public class ServerSetPlayerWorldPacket {
    public UUID uuid;
    public float x, y, z;

    public ServerSetPlayerWorldPacket(UUID uuid, float x, float y, float z) {
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ServerSetPlayerWorldPacket() {
    }
}
