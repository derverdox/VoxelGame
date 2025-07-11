package de.verdox.server.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import de.verdox.voxel.server.VoxelServer;
import de.verdox.voxel.server.level.ServerWorld;
import de.verdox.voxel.server.level.chunk.ServerChunk;
import de.verdox.voxel.shared.network.packet.client.ClientLoadChunkPacket;
import de.verdox.voxel.shared.network.packet.server.ServerSetPlayerWorldPacket;
import de.verdox.voxel.shared.network.packet.server.ServerWorldExistPacket;
import de.verdox.voxel.shared.network.packet.server.level.chunk.ServerChunkPacket;

import java.util.Optional;
import java.util.logging.Logger;

public class ServerConnectionListener extends Listener {
    public static final Logger LOGGER = Logger.getLogger(ServerConnectionListener.class.getSimpleName());

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof ClientLoadChunkPacket packet) {
            receiveClientLoadChunk(connection, packet);
        }
    }

    @Override
    public void connected(Connection connection) {
        VoxelServer.getInstance().getWorlds().forEach(world -> connection.sendTCP(ServerWorldExistPacket.fromWorld(world)));
        connection.sendTCP(new ServerSetPlayerWorldPacket(VoxelServer.getInstance().getStandardWorld().getUuid(), 0, 70, 0));
    }

    @Override
    public void disconnected(Connection connection) {
        connection.close();
    }

    public void receiveClientLoadChunk(Connection connection, ClientLoadChunkPacket e) {
        Optional<ServerWorld> optionalWorld = VoxelServer.getInstance().getWorld(e.getWorld());
        if (optionalWorld.isEmpty()) {
            return;
        }

        Optional<ServerChunk> optionalGameChunk = optionalWorld.get().getChunkMap().getChunk(e.getChunkX(), e.getChunkY(), e.getChunkZ());
        if (optionalGameChunk.isEmpty()) {
            optionalWorld.get().getChunkMap().getOrCreateChunkAsync(e.getChunkX(), e.getChunkY(), e.getChunkZ());
        } else {
            //LOGGER.info("Player " + connection.getID() + " wants to get the chunk at " + e.getChunkX() + ", " + e.getChunkY() + ", " + e.getChunkZ());
            connection.sendTCP(ServerChunkPacket.fromGameChunk(optionalGameChunk.get()));
        }
    }
}
