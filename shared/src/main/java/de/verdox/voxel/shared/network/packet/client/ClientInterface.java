package de.verdox.voxel.shared.network.packet.client;

import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.network.packet.NetworkInterface;
import de.verdox.voxel.shared.network.packet.server.ServerSetPlayerWorldPacket;
import de.verdox.voxel.shared.network.packet.server.ServerWorldExistPacket;
import de.verdox.voxel.shared.network.packet.server.level.chunk.ServerChunkPacket;

import java.util.logging.Logger;

public interface ClientInterface extends NetworkInterface {
    Logger LOGGER = Logger.getLogger(ClientInterface.class.getSimpleName());
    <PACKET> void sendToServer(PACKET packet);

    default <PACKET> void receive(PACKET packet) {
        if (packet instanceof ServerWorldExistPacket p) {
            receive(p);
        }
        else if (packet instanceof ServerSetPlayerWorldPacket p) {
            receive(p);
        }
        else if (packet instanceof ServerChunkPacket p) {
            receive(p);
        }
        else {
            LOGGER.warning("Received an unknown packet " + packet.getClass().getSimpleName());
            return;
        }
        //LOGGER.info("Received packet " + packet.getClass().getSimpleName());
    }

    void receive(ServerWorldExistPacket packet);

    void receive(ServerSetPlayerWorldPacket packet);

    void receive(ServerChunkPacket packet);
}
