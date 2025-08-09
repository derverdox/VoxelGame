package de.verdox.voxel.client.play.multiplayer;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import de.verdox.voxel.shared.VoxelBase;

public class ClientConnectionListener extends Listener {
    @Override
    public void received(Connection connection, Object object) {
        VoxelBase.getInstance().clientInterface(clientInterface -> clientInterface.receive(object));
    }
}
