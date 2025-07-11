package de.verdox.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import de.verdox.server.heart.TickThread;
import de.verdox.server.network.ServerConnectionListener;
import de.verdox.voxel.server.VoxelServer;
import de.verdox.voxel.shared.Bootstrap;
import de.verdox.voxel.shared.network.packet.client.ClientInputPacket;
import de.verdox.voxel.shared.network.packet.server.ServerPlayerPositionPacket;

import java.io.IOException;
import java.util.UUID;

/**
 * Launches the server application.
 */
public class ServerLauncher {
    public static void main(String[] args) {
        int writeBufferSize  = 1024 * 1024 * 1024;   // z.B. 32 KB
        int objectBufferSize = 1024 * 1024 * 1024;   // z.B. 64 KB

        Server server = new Server(writeBufferSize, objectBufferSize);
        server.start();

        Bootstrap.bootstrap(server.getKryo());

        try {
            server.bind(54555, 54777);
        } catch (IOException e) {
            e.printStackTrace();
        }
        TickThread tickThread = new TickThread(20);
        tickThread.start();

        VoxelServer.getInstance().createWorld(UUID.randomUUID());

        // Listener für eingehende Daten
        server.addListener(new ServerConnectionListener());
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof ClientInputPacket input) {

                    // Berechnung der Bewegung
                    float moveSpeed = 1f; // Geschwindigkeit auf dem Server
                    float newX = input.moveX * moveSpeed;
                    float newZ = input.moveZ * moveSpeed;

                    // Neue Position des Spielers
                    ServerPlayerPositionPacket position = new ServerPlayerPositionPacket();
                    position.entityID = 0;
                    position.x += newX;
                    position.y = 0f;  // Höhe (optional)
                    position.z += newZ;

                    // Sende die neue Position zurück an den Client
                    connection.sendTCP(position);
                }
            }
        });
    }
}
