package de.verdox.voxel.shared.network.packet.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.VoxelBase;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;

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
        chunk.getHeightmap().write(kryo, output);
        chunk.getDepthMap().write(kryo, output);
        if (chunk.isEmpty()) {
            return;
        }
        chunk.getChunkBlockPalette().write(kryo, output);
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

        CHUNK chunk = world.constructChunkObject(chunkX, chunkY, chunkZ);

        chunk.getChunkLightData().readAndUpdate(kryo, input);
        chunk.getHeightmap().readAndUpdate(kryo, input);
        chunk.getDepthMap().readAndUpdate(kryo, input);


        if (!isEmpty) {
            chunk.getChunkBlockPalette().readAndUpdate(kryo, input);
        }

        chunk.init();

        return chunk;
    }

    private WORLD getWorld(UUID worldUUID) {
        return (WORLD) VoxelBase.getINSTANCE().getWorld(worldUUID).orElse(null);
    }
}
