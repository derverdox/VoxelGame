package de.verdox.voxel.client.network;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.VoxelClient;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.network.packet.server.ServerSetPlayerWorldPacket;
import de.verdox.voxel.shared.network.packet.server.ServerWorldExistPacket;
import de.verdox.voxel.shared.network.packet.server.level.chunk.ServerChunkPacket;
import de.verdox.voxel.client.renderer.ClientRenderer;

public class ClientConnectionListener extends Listener {
    private final ClientRenderer clientRenderer;

    public ClientConnectionListener(ClientRenderer clientRenderer) {
        this.clientRenderer = clientRenderer;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof ServerWorldExistPacket packet) {
            receive(packet);
        } else if (object instanceof ServerSetPlayerWorldPacket packet) {
            receive(packet);
        } else if (object instanceof ServerChunkPacket packet) {
            receive(packet);
        }
    }

    public void receive(ServerWorldExistPacket serverWorldExistPacket) {
        ClientWorld world = new ClientWorld(serverWorldExistPacket.uuid, serverWorldExistPacket.chunkSizeX, serverWorldExistPacket.chunkSizeY, serverWorldExistPacket.chunkSizeZ);
        VoxelClient.getInstance().insertWorld(world);
        Gdx.app.error("ConnectionListener", "Found a new world on server with uuid  " + world.getUuid());
    }

    public void receive(ServerSetPlayerWorldPacket serverSetPlayerWorldPacket) {
        var optionalWorld = VoxelClient.getInstance().getWorld(serverSetPlayerWorldPacket.uuid);
        if (optionalWorld.isPresent()) {
            VoxelClient.getInstance().setCurrentWorld(optionalWorld.get());
            Gdx.app.error("ConnectionListener", "Now rendering world " + serverSetPlayerWorldPacket.uuid);
        } else {
            Gdx.app.error("ConnectionListener", "Received a world that does not exist " + serverSetPlayerWorldPacket.uuid);
        }
    }

    public void receive(ServerChunkPacket serverChunkPacket) {
        ClientChunk clientChunk = (ClientChunk) serverChunkPacket.chunkBase;
        if (clientChunk == null) {
            return;
        }
        Gdx.app.postRunnable(() -> clientChunk.getWorld().queueChunkForProcessing(clientChunk));
    }
}
