package de.verdox.voxel.client.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.gdx.mesh.VertexBufferObjectInstanced;

import java.nio.FloatBuffer;

public class InstancedTerrainMesh implements Disposable {
    private static final VertexAttributes BLOCK_FACE_ATTRIBUTES = new VertexAttributes(
            new VertexAttribute(0, 2, "a_corner_pos")
    );

    private final VertexBufferObjectInstanced vertices;
    private int instanceCount = 0;

    public InstancedTerrainMesh(int maxFaces, VertexAttribute... instanceAttributes) {
        this.vertices = new VertexBufferObjectInstanced(
                false,
                4, BLOCK_FACE_ATTRIBUTES,
                maxFaces, new VertexAttributes(instanceAttributes)
        );

        float[] baseCorners = {0, 0, 0, 1, 1, 0, 1, 1};
        this.vertices.setVertices(baseCorners, 0, baseCorners.length);
    }

    /**
     * Schreibt eine Face-Instanz mit eurem Packing.
     */
    public void writeInstance(FloatBuffer fb,
                              int worldX, int worldY, int worldZ, // = off + local
                              int dir, int aoPacked,
                              TextureRegion region,
                              int sky, int red, int green, int blue) {
        float u0 = region != null ? region.getU() : 0f;
        float v0 = region != null ? region.getV() : 0f;
        float du = region != null ? (region.getU2() - u0) : 1f;
        float dv = region != null ? (region.getV2() - v0) : 1f;

        // light 4×4-bit in ein int packen und als float transportieren (wie CPU)
        int lpack = (sky & 0xF) | ((red & 0xF) << 4) | ((green & 0xF) << 8) | ((blue & 0xF) << 12);

        fb.put(worldX).put(worldY).put(worldZ)
                .put((float) dir)
                .put(Float.intBitsToFloat(aoPacked & 0xFF))
                .put(u0).put(v0).put(du).put(dv)
                .put(Float.intBitsToFloat(lpack));
        instanceCount++;
    }

    public void draw(ShaderProgram shader, Matrix4 projView, float lodScale) {
        shader.bind();
        shader.setUniformMatrix("u_projView", projView);
        shader.setUniformf("u_lodScale", lodScale);

        vertices.bind(shader);
        // Ohne Indexing: 4 Vertices im Strip, N Instanzen
        Gdx.gl30.glDrawArraysInstanced(GL20.GL_TRIANGLE_STRIP, 0, 4, instanceCount);
        vertices.unbind(shader);
    }

    @Override
    public void dispose() {
        vertices.dispose();
    }
}
