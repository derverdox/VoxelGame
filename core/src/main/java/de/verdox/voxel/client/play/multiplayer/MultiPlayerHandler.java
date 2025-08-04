package de.verdox.voxel.client.play.multiplayer;

import com.esotericsoftware.kryonet.Client;
import de.verdox.voxel.client.GameSession;
import de.verdox.voxel.client.network.ClientConnectionListener;
import de.verdox.voxel.shared.Bootstrap;
import de.verdox.voxel.shared.VoxelBase;

import java.io.IOException;

public class MultiPlayerHandler {
    public static void joinServer(String host, int port) {
        VoxelBase.createNewVoxelBase();
        int writeBufferSize = 1024 * 1024 * 16;   // z.B. 32 KB
        int objectBufferSize = 1024 * 1024 * 16;   // z.B. 64 KB


        Client client = new Client(writeBufferSize, objectBufferSize);
        client.start();
        Bootstrap.bootstrap(client.getKryo());

        GameSession.startRemoteSession(client);

        try {
            client.connect(5000, "localhost", 54000);
            client.addListener(new ClientConnectionListener());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
