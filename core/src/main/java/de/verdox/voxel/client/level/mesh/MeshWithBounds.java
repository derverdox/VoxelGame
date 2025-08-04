package de.verdox.voxel.client.level.mesh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.shader.Shaders;
import gaiasky.util.gdx.mesh.IntMesh;

public interface MeshWithBounds {
    void setPos(float x, float y, float z);

    void render(Camera camera);

    void dispose();

    abstract class RawMeshBased<MESH> implements MeshWithBounds {
        protected final MESH mesh;
        protected final ShaderProgram shader;
        protected final TextureAtlas textureAtlas;
        protected final int atlasPage;
        protected final Matrix4 worldTransform = new Matrix4();

        public RawMeshBased(MESH mesh, ShaderProgram shader, TextureAtlas textureAtlas, int atlasPage) {
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
        public void render(Camera camera) {
            shader.setUniformMatrix("u_worldTrans", worldTransform);
            shader.setUniformMatrix("u_projViewTrans", camera.combined);
            shader.setUniformi("u_texture", atlasPage);
            renderMesh(mesh, shader);
        }

        public abstract void renderMesh(MESH mesh, ShaderProgram shader);
    }

    class ShortRawMeshBased extends RawMeshBased<Mesh> {
        public ShortRawMeshBased(Mesh mesh, ShaderProgram shader, TextureAtlas textureAtlas, int atlasPage) {
            super(mesh, shader, textureAtlas, atlasPage);
        }

        @Override
        public void renderMesh(Mesh mesh, ShaderProgram shader) {
            mesh.render(shader, GL20.GL_TRIANGLES);
        }

        @Override
        public void dispose() {
            mesh.dispose();
        }
    }

    class IntRawMeshBased extends RawMeshBased<IntMesh> {
        public IntRawMeshBased(IntMesh mesh, ShaderProgram shader, TextureAtlas textureAtlas, int atlasPage) {
            super(mesh, shader, textureAtlas, atlasPage);
        }

        @Override
        public void renderMesh(IntMesh mesh, ShaderProgram shader) {
            mesh.render(shader, GL20.GL_TRIANGLES);
        }

        @Override
        public void dispose() {
            mesh.dispose();
        }
    }
}
