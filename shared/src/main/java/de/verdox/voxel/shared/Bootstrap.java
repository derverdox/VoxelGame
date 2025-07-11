package de.verdox.voxel.shared;

import com.esotericsoftware.kryo.Kryo;
import de.verdox.voxel.shared.data.types.Registries;
import de.verdox.voxel.shared.network.packet.PacketRegistry;

public class Bootstrap {
    public static PacketRegistry packetRegistry;

    public static void bootstrap(Kryo kryo) {
        packetRegistry = new PacketRegistry(kryo);
        Registries.bootstrap();
    }
}
