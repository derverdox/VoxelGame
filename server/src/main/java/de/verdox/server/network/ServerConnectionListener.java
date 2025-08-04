package de.verdox.server.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import de.verdox.voxel.shared.VoxelBase;

public class ServerConnectionListener extends Listener {
    @Override
    public void received(Connection connection, Object object) {
        VoxelBase.getInstance().serverInterface(serverInterface -> serverInterface.receive(object, connection.getID()));
    }

    @Override
    public void connected(Connection connection) {
        VoxelBase.getInstance().serverInterface(serverInterface -> serverInterface.onConnect(connection.getID()));
    }

    @Override
    public void disconnected(Connection connection) {
        connection.close();
    }
}
