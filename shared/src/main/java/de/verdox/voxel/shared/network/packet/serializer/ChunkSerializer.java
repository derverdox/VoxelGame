package de.verdox.voxel.shared.network.packet.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.VoxelBase;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.util.palette.ChunkBlockPalette;

import java.util.List;
import java.util.UUID;

public class ChunkSerializer<WORLD extends World<CHUNK>, CHUNK extends ChunkBase<WORLD>> extends Serializer<CHUNK> {

    @Override
    public void write(Kryo kryo, Output output, CHUNK chunk) {
        kryo.writeObject(output, chunk.getWorld().getUuid());
        output.writeInt(chunk.getChunkX());
        output.writeInt(chunk.getChunkY());
        output.writeInt(chunk.getChunkZ());
        output.writeBoolean(chunk.isEmpty());
        chunk.getChunkLightData().write(kryo, output);
        if (chunk.isEmpty()) {
            return;
        }
        writeBlockPalette(kryo, output, chunk);
        kryo.writeObject(output, chunk.getHeightmap());
        kryo.writeObject(output, chunk.getDepthMap());
    }

    @Override
    public CHUNK read(Kryo kryo, Input input, Class<? extends CHUNK> type) {
        UUID worldUUID = kryo.readObject(input, UUID.class);
        WORLD world = getWorld(worldUUID);

        if (world == null) {
            return null;
        }
        int chunkX = input.readInt();
        int chunkY = input.readInt();
        int chunkZ = input.readInt();
        boolean isEmpty = input.readBoolean();

        ChunkLightData chunkLightData = new ChunkLightData(world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ());
        chunkLightData.readAndUpdate(kryo, input);


        ChunkBlockPalette chunkBlockPalette = new ChunkBlockPalette(Blocks.AIR.findKey(), world.getChunkSizeX(), world.getChunkSizeY(), world.getChunkSizeZ());

        byte[][] heightMap;
        byte[][] depthMap;

        if (!isEmpty) {
            readBlockPalette(kryo, input, chunkBlockPalette);
            heightMap = kryo.readObject(input, byte[][].class);
            depthMap = kryo.readObject(input, byte[][].class);

        }
        else {
            heightMap = new byte[world.getChunkSizeX()][world.getChunkSizeZ()];
            depthMap = new byte[world.getChunkSizeX()][world.getChunkSizeZ()];
        }

        return world.constructChunkObject(chunkX, chunkY, chunkZ, chunkBlockPalette, heightMap, depthMap, chunkLightData);
    }

    private WORLD getWorld(UUID worldUUID) {
        return (WORLD) VoxelBase.getINSTANCE().getWorld(worldUUID).orElse(null);
    }

    private void writeBlockPalette(Kryo kryo, Output output, CHUNK chunk) {
        ChunkBlockPalette palette = chunk.getChunkBlockPalette();

        List<ResourceLocation> entries = palette.getPalette();
        output.writeInt(entries.size(), true);
        for (ResourceLocation block : entries) {
            kryo.writeObject(output, block);
        }
        output.writeInt(palette.getBitsPerBlock(), true);
        long[] data = palette.getData();
        output.writeInt(data.length, true);
        for (long word : data) {
            output.writeLong(word);
        }
    }

    private static void readBlockPalette(Kryo kryo, Input input, ChunkBlockPalette chunkBlockPalette) {
        int paletteSize = input.readInt(true);
        for (int i = 0; i < paletteSize; i++) {
            ResourceLocation block = kryo.readObject(input, ResourceLocation.class);
            chunkBlockPalette.setForSerialization(block, i);
        }

        int bitsPerBlock = input.readInt(true);
        int dataLen = input.readInt(true);
        long[] data = new long[dataLen];
        for (int i = 0; i < dataLen; i++) {
            data[i] = input.readLong();
        }
        chunkBlockPalette.setForDeserialization(bitsPerBlock, data);
    }
}
