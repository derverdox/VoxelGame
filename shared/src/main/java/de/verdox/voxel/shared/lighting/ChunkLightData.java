package de.verdox.voxel.shared.lighting;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.verdox.voxel.shared.network.packet.serializer.NetworkSerializable;
import lombok.Getter;

import java.util.Arrays;

public class ChunkLightData implements NetworkSerializable {
    @Override
    public void write(Kryo kryo, Output output) {
        kryo.writeObject(output, state);
        output.writeShort(uniformPacked);

        if (data != null) {
            output.writeInt(data.length);
            output.writeShorts(data, 0, data.length);
        } else {
            output.writeInt(-1);
        }
    }

    @Override
    public void readAndUpdate(Kryo kryo, Input input) {
        state = kryo.readObject(input, LightState.class);
        uniformPacked = input.readShort();
        int length = input.readInt();
        if (length != -1) {
            data = input.readShorts(length);
        }
    }

    public enum LightState {UNINITIALIZED, UNIFORM, DETAILED}

    private final int sizeX, sizeY, sizeZ, totalSize;
    @Getter
    private LightState state = LightState.UNINITIALIZED;
    private short uniformPacked = 0;
    private short[] data;

    public ChunkLightData(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.totalSize = sizeX * sizeY * sizeZ;
    }

    private int idx(int x, int y, int z) {
        return x + sizeX * (y + sizeY * z);
    }

    private short pack(int sky, int r, int g, int b) {
        return (short) (
            ((sky & 0xF) << 12) |
                ((r & 0xF) << 8) |
                ((g & 0xF) << 4) |
                (b & 0xF)
        );
    }

    private int unpackSky(short packed) {
        return (packed >>> 12) & 0xF;
    }

    private int unpackRed(short packed) {
        return (packed >>> 8) & 0xF;
    }

    private int unpackGreen(short packed) {
        return (packed >>> 4) & 0xF;
    }

    private int unpackBlue(short packed) {
        return packed & 0xF;
    }

    // --- State-Handling ---
    private void ensureDetailed() {
        if (state == LightState.DETAILED) return;
        // von UNIFORM → DETAILED: Daten allokieren und füllen
        data = new short[totalSize];
        Arrays.fill(data, uniformPacked);
        state = LightState.DETAILED;
    }

    private short getPacked(int x, int y, int z) {
        return switch (state) {
            case DETAILED -> data[idx(x, y, z)];
            case UNIFORM -> uniformPacked;
            default -> 0;
        };
    }

    private void setPacked(int x, int y, int z, short packed) {
        switch (state) {
            case UNINITIALIZED:
                uniformPacked = packed;
                state = LightState.UNIFORM;
                break;
            case UNIFORM:
                if (packed != uniformPacked) {
                    ensureDetailed();
                    data[idx(x, y, z)] = packed;
                }
                break;
            case DETAILED:
                data[idx(x, y, z)] = packed;
                break;
        }
    }

    public void setSkyLight(int x, int y, int z, byte sky) {
        short old = getPacked(x, y, z);
        short nw = pack(sky, unpackRed(old), unpackGreen(old), unpackBlue(old));
        setPacked(x, y, z, nw);
    }

    public byte getSkyLight(int x, int y, int z) {
        return (byte) unpackSky(getPacked(x, y, z));
    }

    public void setBlockLight(int x, int y, int z, byte red, byte green, byte blue) {
        short old = getPacked(x, y, z);
        short nw = pack(unpackSky(old), red, green, blue);
        setPacked(x, y, z, nw);
    }

    public byte getBlockRed(int x, int y, int z) {
        return (byte) unpackRed(getPacked(x, y, z));
    }

    public byte getBlockGreen(int x, int y, int z) {
        return (byte) unpackGreen(getPacked(x, y, z));
    }

    public byte getBlockBlue(int x, int y, int z) {
        return (byte) unpackBlue(getPacked(x, y, z));
    }
}

