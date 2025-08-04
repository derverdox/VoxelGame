package de.verdox.voxel.shared.network.packet.server;

import de.verdox.voxel.shared.level.world.World;

import java.util.UUID;

public class ServerWorldExistPacket {
    public World world;

    public ServerWorldExistPacket(World world) {
        this.world = world;
    }

    public ServerWorldExistPacket() {
    }

    public static ServerWorldExistPacket fromWorld(World world) {
        return new ServerWorldExistPacket(world);
    }
}
