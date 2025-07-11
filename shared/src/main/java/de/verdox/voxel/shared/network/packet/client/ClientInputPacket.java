package de.verdox.voxel.shared.network.packet.client;

public class ClientInputPacket {
    public float moveX, moveZ;
    public boolean jump;
    public float yaw, pitch;

    public ClientInputPacket() {
    }
}
