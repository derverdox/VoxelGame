package de.verdox.voxel.shared.network.packet.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.level.world.LevelWorld;
import de.verdox.voxel.shared.level.world.World;

import java.util.UUID;

public class WorldSerializer extends Serializer<World> {

    @Override
    public void write(Kryo kryo, Output output, World world) {
        kryo.writeObject(output, world.getUuid());
        output.writeByte(world.getChunkSizeX());
        output.writeByte(world.getChunkSizeY());
        output.writeByte(world.getChunkSizeZ());
    }

    @Override
    public World read(Kryo kryo, Input input, Class<? extends World> aClass) {
        UUID worldUUID = kryo.readObject(input, UUID.class);
        byte chunkSizeX = input.readByte();
        byte chunkSizeY = input.readByte();
        byte chunkSizeZ = input.readByte();

        return new LevelWorld(worldUUID, chunkSizeX, chunkSizeY, chunkSizeZ);
    }
}
