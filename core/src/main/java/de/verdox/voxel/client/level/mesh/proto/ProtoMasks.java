package de.verdox.voxel.client.level.mesh.proto;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.List;

public class ProtoMasks {
    @Getter
    private static final List<ProtoMask> MASKS = new ObjectArrayList<>();
    public static final Single SINGLE = register(new Single((byte) 0));

    public static int getAmountMasks() {
        return MASKS.size();
    }
    private static <T extends ProtoMask> T register(T mask) {
        MASKS.add(mask);
        return mask;
    }
}
