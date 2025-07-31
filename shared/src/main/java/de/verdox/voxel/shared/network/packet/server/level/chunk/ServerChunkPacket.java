package de.verdox.voxel.shared.network.packet.server.level.chunk;

import de.verdox.voxel.shared.level.chunk.ChunkBase;
import lombok.Getter;

@Getter
public class ServerChunkPacket {
    public ChunkBase<?> chunkBase;

    public ServerChunkPacket() {

    }

    public static ServerChunkPacket fromGameChunk(ChunkBase<?> chunkBase) {
        ServerChunkPacket serverChunkPacket = new ServerChunkPacket();
        serverChunkPacket.chunkBase = chunkBase;
        return serverChunkPacket;
    }
}
