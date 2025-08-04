package de.verdox.voxel.shared.network.packet.server;

import de.verdox.voxel.shared.network.packet.NetworkInterface;
import de.verdox.voxel.shared.network.packet.client.ClientInterface;
import de.verdox.voxel.shared.network.packet.client.ClientRequestChunkPacket;

import java.util.logging.Logger;

public interface ServerInterface extends NetworkInterface {
    Logger LOGGER = Logger.getLogger(ServerInterface.class.getSimpleName());

    void onConnect(int connectionId);

    <PACKET> void sendToPlayer(PACKET packet, int connectionId);

    <PACKET> void sendToPlayer(PACKET packet, int... connectionIDs);

    <PACKET> void broadcast(PACKET packet);

    default <PACKET> void receive(PACKET packet, int connectionId) {
        if (packet instanceof ClientRequestChunkPacket p) {
            receive(p, connectionId);
        } else {
            LOGGER.warning("Received an unknown packet " + packet.getClass().getSimpleName() + " from " + connectionId);
            return;
        }
        //LOGGER.info("Received packet " + packet.getClass().getSimpleName() + " from " + connectionId);
    }

    void receive(ClientRequestChunkPacket clientRequestChunkPacket, int connectionId);
}
