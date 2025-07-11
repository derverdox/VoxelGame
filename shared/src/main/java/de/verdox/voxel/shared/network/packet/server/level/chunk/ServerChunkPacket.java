package de.verdox.voxel.shared.network.packet.server.level.chunk;

import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.palette.ChunkBlockPalette;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ServerChunkPacket {
    public UUID worldUUID;
    public int chunkX;
    public int chunkY;
    public int chunkZ;
    public ChunkBlockPalette palette;
    public byte[][] heightmap;
    public byte[][] depthMap;

    public ServerChunkPacket(UUID worldUUID, int chunkX, int chunkY, int chunkZ, ChunkBlockPalette palette, byte[][] heightmap, byte[][] depthMap) {
        this.worldUUID = worldUUID;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.palette = palette;
        this.heightmap = heightmap;
        this.depthMap = depthMap;
    }

    public ServerChunkPacket() {

    }

    public static ServerChunkPacket fromGameChunk(ChunkBase<?> chunkBase) {
        return new ServerChunkPacket(chunkBase.getWorld().getUuid(), chunkBase.getChunkX(), chunkBase.getChunkY(), chunkBase.getChunkZ(), chunkBase.getChunkBlockPalette(), chunkBase.getHeightmap(), chunkBase.getDepthMap());
    }
}
