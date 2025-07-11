package de.verdox.voxel.client.level.mesh;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.collision.BoundingBox;

public record MeshWithBounds(ModelInstance instance, BoundingBox bounds) {
    public BoundingBox setPos(float x, float y, float z) {

        instance.transform.setToTranslation(x, y, z);
        return new BoundingBox(bounds).mul(instance.transform);
    }
}
