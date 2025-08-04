package de.verdox.voxel.shared.network.packet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.level.chunk.data.sliced.AbstractSliceMap;
import de.verdox.voxel.shared.level.world.LevelWorld;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.network.packet.client.ClientInputPacket;
import de.verdox.voxel.shared.network.packet.client.ClientRequestChunkPacket;
import de.verdox.voxel.shared.network.packet.serializer.ChunkSerializer;
import de.verdox.voxel.shared.network.packet.serializer.WorldSerializer;
import de.verdox.voxel.shared.network.packet.server.ServerPlayerPositionPacket;
import de.verdox.voxel.shared.network.packet.server.ServerSetPlayerWorldPacket;
import de.verdox.voxel.shared.network.packet.server.ServerWorldExistPacket;
import de.verdox.voxel.shared.network.packet.server.level.chunk.ServerChunkPacket;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;

import java.util.UUID;

public class PacketRegistry {

    public static final Serializer<ResourceLocation> RESOURCE_LOCATION_SERIALIZER = new Serializer<>() {
        @Override
        public void write(Kryo kryo, Output output, ResourceLocation object) {
            output.writeString(object.getNamespace());
            output.writeString(object.getPath());
        }

        @Override
        public ResourceLocation read(Kryo kryo, Input input, Class<? extends ResourceLocation> type) {
            return ResourceLocation.of(input.readString(), input.readString());
        }
    };

    public PacketRegistry(Kryo kryo) {
        kryo.register(ClientInputPacket.class);
        kryo.register(ClientRequestChunkPacket.class);


        kryo.register(ServerPlayerPositionPacket.class);
        kryo.register(ServerChunkPacket.class);
        kryo.register(ServerSetPlayerWorldPacket.class);
        kryo.register(ServerWorldExistPacket.class);
        kryo.register(ChunkLightData.LightState.class, new DefaultSerializers.EnumSerializer(ChunkLightData.LightState.class));
        kryo.register(ThreeDimensionalPalette.State.class, new DefaultSerializers.EnumSerializer(ThreeDimensionalPalette.State.class));
        kryo.register(AbstractSliceMap.State.class, new DefaultSerializers.EnumSerializer(AbstractSliceMap.State.class));
        kryo.register(ChunkBase.class, new ChunkSerializer());
        kryo.register(LevelWorld.class, new WorldSerializer());
        kryo.register(UUID.class, new DefaultSerializers.UUIDSerializer());

        kryo.register(byte[][].class, new Serializer<byte[][]>() {
            @Override
            public void write(Kryo kryo, Output output, byte[][] bytes) {
                // Länge des äußeren Arrays
                output.writeInt(bytes.length);
                for (byte[] inner : bytes) {
                    if (inner != null) {
                        // Markierung, dass das innere Array existiert
                        output.writeBoolean(true);
                        // Länge und Daten
                        output.writeInt(inner.length);
                        output.writeBytes(inner);
                    } else {
                        // Markierung für null
                        output.writeBoolean(false);
                    }
                }
            }

            @Override
            public byte[][] read(Kryo kryo, Input input, Class<? extends byte[][]> type) {
                int outerLength = input.readInt();
                byte[][] result = new byte[outerLength][];
                for (int i = 0; i < outerLength; i++) {
                    boolean notNull = input.readBoolean();
                    if (notNull) {
                        int innerLength = input.readInt();
                        byte[] inner = new byte[innerLength];
                        input.readBytes(inner);
                        result[i] = inner;
                    } else {
                        result[i] = null;
                    }
                }
                return result;
            }
        });

        kryo.register(ResourceLocation.class, RESOURCE_LOCATION_SERIALIZER);
        kryo.register(UUID.class, new Serializer<UUID>() {

            @Override
            public void write(Kryo kryo, Output output, UUID object) {
                output.writeLong(object.getLeastSignificantBits());
                output.writeLong(object.getMostSignificantBits());
            }

            @Override
            public UUID read(Kryo kryo, Input input, Class<? extends UUID> type) {
                long leastBytes = input.readLong();
                long mostBytes = input.readLong();
                return new UUID(mostBytes, leastBytes);
            }
        });
    }
}
