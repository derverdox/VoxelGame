package de.verdox.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import de.verdox.server.heart.TickThread;
import de.verdox.server.network.ServerConnectionListener;
import de.verdox.voxel.server.VoxelServer;
import de.verdox.voxel.server.level.chunk.ServerChunk;
import de.verdox.voxel.shared.Bootstrap;
import de.verdox.voxel.shared.network.packet.PacketRegistry;
import de.verdox.voxel.shared.network.packet.serializer.ChunkSerializer;

import java.io.IOException;
import java.util.UUID;

/**
 * Launches the server application.
 */
public class ServerLauncher {
    public static void main(String[] args) {
        int writeBufferSize  = 1024 * 1024 * 16;
        int objectBufferSize = 1024 * 1024 * 16;

        Server server = new Server(writeBufferSize, objectBufferSize);
        server.start();

        Bootstrap.bootstrap(server.getKryo());
        server.getKryo().register(ServerChunk.class, new ChunkSerializer<>());

        try {
            server.bind(54555, 54777);
        } catch (IOException e) {
            e.printStackTrace();
        }
        TickThread tickThread = new TickThread(20);
        tickThread.start();

        VoxelServer.getInstance().createWorld(UUID.randomUUID());

        server.addListener(new ServerConnectionListener());
    }
}
