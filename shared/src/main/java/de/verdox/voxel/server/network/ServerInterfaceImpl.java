package de.verdox.voxel.server.network;

import com.esotericsoftware.kryonet.Server;
import de.verdox.voxel.shared.VoxelBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.network.packet.client.ClientRequestChunkPacket;
import de.verdox.voxel.shared.network.packet.server.ServerInterface;
import de.verdox.voxel.shared.network.packet.server.ServerSetPlayerWorldPacket;
import de.verdox.voxel.shared.network.packet.server.ServerWorldExistPacket;
import de.verdox.voxel.shared.network.packet.server.level.chunk.ServerChunkPacket;

import java.util.Optional;

public class ServerInterfaceImpl implements ServerInterface {
    protected final Server server;

    public ServerInterfaceImpl(Server server) {
        this.server = server;
    }

    @Override
    public void onConnect(int connectionId) {
        VoxelBase.getInstance().getWorlds().forEach(world -> sendToPlayer(ServerWorldExistPacket.fromWorld(world), connectionId));
        sendToPlayer(new ServerSetPlayerWorldPacket(VoxelBase.getInstance().getStandardWorld().getUuid(), 0, 70, 0), connectionId);



    }

    @Override
    public <PACKET> void sendToPlayer(PACKET packet, int connectionId) {
        server.sendToTCP(connectionId, packet);
    }

    @Override
    public <PACKET> void sendToPlayer(PACKET packet, int... connectionIDs) {
        for (int connectionId : connectionIDs) {
            sendToPlayer(packet, connectionId);
        }
    }

    @Override
    public <PACKET> void broadcast(PACKET packet) {
        server.sendToAllTCP(packet);
    }

    @Override
    public void receive(ClientRequestChunkPacket packet, int connectionId) {
        Optional<World> optionalWorld = VoxelBase.getInstance().getWorld(packet.getWorld());
        if (optionalWorld.isEmpty()) {
            return;
        }

        optionalWorld.get().getChunkMap().getOrCreateChunkAsync(packet.getChunkX(), packet.getChunkY(), packet.getChunkZ(), serverChunk -> {
            sendToPlayer(ServerChunkPacket.fromGameChunk((ChunkBase) serverChunk), connectionId);
        });
    }
}
