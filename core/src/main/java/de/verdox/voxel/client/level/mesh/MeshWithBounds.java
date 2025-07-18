package de.verdox.voxel.client.level.mesh;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.shader.Shaders;

public interface MeshWithBounds {
    void setPos(float x, float y, float z);

    void addToModelCache(ModelCache cache);

    void render(Camera camera, ModelBatch batch);

    record ModelInstanceBased(ModelInstance instance, BoundingBox bounds) implements MeshWithBounds {
        @Override
        public void setPos(float x, float y, float z) {
            instance.transform.setToTranslation(x, y, z);
            bounds.set(new BoundingBox(bounds).mul(instance.transform));
        }

        @Override
        public void addToModelCache(ModelCache cache) {
            cache.add(instance);
        }

        @Override
        public void render(Camera camera, ModelBatch batch) {
            batch.render(instance);
        }
    }

    class RawMeshBased implements MeshWithBounds {
        private final Mesh mesh;
        private final ShaderProgram shader;
        private final TextureAtlas textureAtlas;
        private final int atlasPage;
        private final Matrix4 worldTransform = new Matrix4();

        public RawMeshBased(Mesh mesh, ShaderProgram shader, TextureAtlas textureAtlas, int atlasPage) {
            this.mesh = mesh;
            this.shader = shader;
            this.textureAtlas = textureAtlas;
            this.atlasPage = atlasPage;
        }

        @Override
        public void setPos(float x, float y, float z) {
            worldTransform.setToTranslation(x, y, z);
        }

        @Override
        public void addToModelCache(ModelCache cache) {

        }

        @Override
        public void render(Camera camera, ModelBatch batch) {
            shader.setUniformMatrix("u_worldTrans", worldTransform);
            shader.setUniformMatrix("u_projViewTrans", camera.combined);
            textureAtlas.getTextures().first().bind(0);
            mesh.render(shader, GL20.GL_TRIANGLES);
        }
    }
}
