package de.verdox.voxel.shared.level.chunk.data.sliced;

import de.verdox.voxel.shared.level.chunk.BoxSlice;
import de.verdox.voxel.shared.network.packet.serializer.NetworkSerializable;

public interface SliceMap extends NetworkSerializable, BoxSlice {
    byte get(int x, int z);

    void set(int x, int z, byte value);

    boolean isEmpty();

    boolean isUniform();

    interface Strategy {
        byte get(int x, int z);

        Strategy set(int x, int z, byte value);

        boolean isEmpty();

        boolean isUniform();
    }
}
