package de.verdox.voxel.client.play.singleplayer;

import com.esotericsoftware.kryonet.Server;
import de.verdox.voxel.server.network.ServerInterfaceImpl;
import de.verdox.voxel.shared.network.packet.client.ClientInterface;
import it.unimi.dsi.fastutil.ints.IntSet;

public class LocalServerInterface extends ServerInterfaceImpl {
    private final ClientInterface localClientInterface;

    public LocalServerInterface(Server server, ClientInterface localClientInterface) {
        super(server);
        this.localClientInterface = localClientInterface;
    }

    public void localConnect() {
        onConnect(LOCAL_CONNECTION_ID);
    }

    @Override
    public <PACKET> void sendToPlayer(PACKET packet, int connectionId) {
        if (connectionId == LOCAL_CONNECTION_ID) {
            localClientInterface.receive(packet);
        } else {
            super.sendToPlayer(packet, connectionId);
        }
    }

    @Override
    public <PACKET> void broadcast(PACKET packet) {
        this.localClientInterface.receive(packet);
        server.sendToAllTCP(packet);
    }
}
