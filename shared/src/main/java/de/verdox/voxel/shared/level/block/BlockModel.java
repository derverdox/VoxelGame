package de.verdox.voxel.shared.level.block;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.physics.bb.AABB;
import lombok.Getter;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BlockModel {
    private final Map<String, ResourceLocation> texturesPerFace = new HashMap<>();
    private BlockModelType blockModelType;
    private AABB boundingBox = new AABB(new Vector3f(0, 0, 0), new Vector3f(1, 1, 1));

    public BlockModel withModelType(BlockModelType blockModelType) {
        this.blockModelType = blockModelType;
        return this;
    }

    public BlockModel withBoundingBox(AABB boundingBox) {
        this.boundingBox = boundingBox;
        return this;
    }

    public BlockModel withTextureMapping(String blockFace, ResourceLocation textureForFace) {
        texturesPerFace.put(blockFace, textureForFace);
        return this;
    }


    public ResourceLocation getTextureOfFace(String name) {
        return texturesPerFace.getOrDefault(name, null);
    }
}
