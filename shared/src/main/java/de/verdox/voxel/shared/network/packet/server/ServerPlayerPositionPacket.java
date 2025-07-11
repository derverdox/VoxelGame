package de.verdox.voxel.shared.network.packet.server;

public class ServerPlayerPositionPacket {
    public int entityID;
    public float x, y, z;
    public float yaw, pitch;

    public ServerPlayerPositionPacket() {
    }
}
