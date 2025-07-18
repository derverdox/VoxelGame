package de.verdox.voxel.shared.network.packet.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public interface NetworkSerializable {
    void write(Kryo kryo, Output output);
    void readAndUpdate(Kryo kryo, Input input);
}
