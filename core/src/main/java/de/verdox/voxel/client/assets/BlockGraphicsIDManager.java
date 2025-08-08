package de.verdox.voxel.client.assets;

import de.verdox.voxel.shared.data.types.Registries;

import java.util.concurrent.atomic.AtomicInteger;

public class BlockGraphicsIDManager {
    private static final AtomicInteger counter = new AtomicInteger();

    public static void bootstrap() {
        Registries.BLOCKS.streamEntries().forEach(blockBase -> blockBase.setGraphicsBlockId((short) counter.getAndAdd(1)));
    }
}
