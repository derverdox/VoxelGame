package de.verdox.voxel.client.play;

import com.esotericsoftware.kryonet.Client;
import de.verdox.voxel.client.GameSession;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.VoxelBase;
import de.verdox.voxel.shared.network.packet.client.ClientInterface;
import de.verdox.voxel.shared.network.packet.serializer.ChunkSerializer;
import de.verdox.voxel.shared.network.packet.server.ServerSetPlayerWorldPacket;
import de.verdox.voxel.shared.network.packet.server.ServerWorldExistPacket;
import de.verdox.voxel.shared.network.packet.server.level.chunk.ServerChunkPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ClientInterfaceImpl implements ClientInterface {
    private final Map<UUID, ClientWorld> worlds = new ConcurrentHashMap<>();
    private final Client client;
    public ClientWorld currentWorld;

    public ClientInterfaceImpl(Client client) {
        this.client = client;
    }

    private boolean isLocalHost() {
        return client == null;
    }

    @Override
    public <PACKET> void sendToServer(PACKET packet) {
        if (!isLocalHost()) {
            client.sendTCP(packet);
        } else {
            VoxelBase.getInstance().serverInterface(serverInterface -> serverInterface.receive(packet, LOCAL_CONNECTION_ID));
        }
    }

    @Override
    public void receive(ServerWorldExistPacket packet) {
        GameSession.getInstance().addWorld(packet.world);
        LOGGER.warning("Discovered server world: " + packet.world.getUuid());
    }

    @Override
    public void receive(ServerSetPlayerWorldPacket packet) {
        GameSession.getInstance().selectCurrentWorld(packet.uuid);
        LOGGER.warning("Server set player world: " + packet.uuid);
    }

    @Override
    public void receive(ServerChunkPacket packet) {
        if (packet.chunkBase.getWorld() == null) {
            LOGGER.warning("Received chunk with unknown world");
            return;
        }
        if (!packet.getChunkBase().getWorld().getUuid().equals(GameSession.getInstance().getCurrentWorld().getUuid())) {
            LOGGER.warning("Received chunk with for world " + packet.getChunkBase().getWorld().getUuid() + " that is not the current world");
            return;
        }
        ClientWorld clientWorld = GameSession.getInstance().getCurrentWorld();
        if (!isLocalHost()) {
            clientWorld.notifyAddChunk(packet.chunkBase);
        } else {
            clientWorld.getChunkRequestManager().notifyChunkReceived(packet.chunkBase.getChunkKey());
        }
    }
}
