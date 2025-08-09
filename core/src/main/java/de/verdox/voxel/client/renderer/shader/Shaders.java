package de.verdox.voxel.client.renderer.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Shaders {
    public static final VertexAttribute[] SINGLE_OPAQUE_ATTRIBUTES_ARRAY = new VertexAttribute[]{
            new VertexAttribute(VertexAttributes.Usage.Position, 1, "a_position_and_ao"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 1, "a_texCoord_and_light")
    };

    public static final VertexAttributes SINGLE_OPAQUE_ATTRIBUTES = new VertexAttributes(SINGLE_OPAQUE_ATTRIBUTES_ARRAY);

    public static final ShaderProgram SINGLE_OPAQUE_BLOCK_SHADER = new ShaderProgram(
            Gdx.files.internal("voxel/shaders/core/blockface/single/single_opaque_face.vsh"),
            Gdx.files.internal("voxel/shaders/core/blockface/single/single_opaque_face.fsh")
    );

    public static void initShaders() {
        if (!SINGLE_OPAQUE_BLOCK_SHADER.isCompiled()) {
            throw new GdxRuntimeException("Shader-Fehler:\n" + SINGLE_OPAQUE_BLOCK_SHADER.getLog());
        }
    }
}
