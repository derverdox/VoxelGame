package de.verdox.voxel.shared.network.packet.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.VoxelBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.level.chunk.ChunkBase;

import java.util.UUID;

public class ChunkSerializer extends Serializer<Chunk> {

    @Override
    public void write(Kryo kryo, Output output, Chunk chunk) {
        kryo.writeObject(output, chunk.getWorld().getUuid());
        output.writeInt(chunk.getChunkX());
        output.writeInt(chunk.getChunkY());
        output.writeInt(chunk.getChunkZ());
        output.writeBoolean(chunk.isEmpty());
        chunk.getChunkLightData().write(kryo, output);
        chunk.getHeightMap().write(kryo, output);
        chunk.getDepthMap().write(kryo, output);
        if (chunk.isEmpty()) {
            return;
        }
        chunk.getChunkBlockPalette().write(kryo, output);
    }

    @Override
    public Chunk read(Kryo kryo, Input input, Class<? extends Chunk> type) {
        UUID worldUUID = kryo.readObject(input, UUID.class);
        World world = getWorld(worldUUID);

        if (world == null) {
            return null;
        }
        int chunkX = input.readInt();
        int chunkY = input.readInt();
        int chunkZ = input.readInt();
        boolean isEmpty = input.readBoolean();

        Chunk chunk = new ChunkBase(world, chunkX, chunkY, chunkZ);

        chunk.getChunkLightData().readAndUpdate(kryo, input);
        chunk.getHeightMap().readAndUpdate(kryo, input);
        chunk.getDepthMap().readAndUpdate(kryo, input);


        if (!isEmpty) {
            chunk.getChunkBlockPalette().readAndUpdate(kryo, input);
        }

        chunk.init();

        return chunk;
    }

    private World getWorld(UUID worldUUID) {
        return VoxelBase.getInstance().getWorld(worldUUID).orElse(null);
    }
}
