package de.verdox.voxel.client.util;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

public class InstancedTerrainMesh implements Disposable {
    private static final VertexAttributes BLOCK_FACE_ATTRIBUTES = new VertexAttributes(
            new VertexAttribute(0, 2, "a_corner_pos")
    );

    private final Mesh mesh;

    public InstancedTerrainMesh(int maxFaces, VertexAttribute... instanceAttributes) {
        float[] baseCorners = {
                0, 0,
                1, 0,
                0, 1,
                1, 1
        };

        this.mesh = new Mesh(true, 4, 0, BLOCK_FACE_ATTRIBUTES);
        this.mesh.enableInstancedRendering(false, maxFaces, instanceAttributes);
        this.mesh.setVertices(baseCorners);
    }

    public void setInstances(float[] instances, int instanceOffset, int numInstances) {
        this.mesh.setInstanceData(instances, instanceOffset, numInstances);
    }

    public void setInstances(float[] instances) {
        this.mesh.setInstanceData(instances);
    }

    public void render(ShaderProgram shader) {
        this.mesh.render(shader, GL20.GL_TRIANGLE_STRIP);
    }

    @Override
    public void dispose() {
        mesh.dispose();
    }
}
