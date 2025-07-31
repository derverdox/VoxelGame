package de.verdox.voxel.client.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Shaders {
    VertexAttribute aoAttr = new VertexAttribute(
            VertexAttributes.Usage.TextureCoordinates, // oder ein beliebiger Usage‑Flag, alias zählt
            1,                        // 1 Komponente
            GL20.GL_UNSIGNED_BYTE,    // byte
            true,                     // normalized → im Shader [0..1]
            "a_ao"
    );


    public static final VertexAttribute[] GREEDY_OPAQUE_ATTRIBUTES = new VertexAttribute[]{
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 3, "normal_id"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_greedy_start"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 2, "a_greedy_end"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_light"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_ambient_occlusion")
    };

    public static final VertexAttribute[] SINGLE_OPAQUE_ATTRIBUTES = new VertexAttribute[]{
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_light")
    };

    public static final ShaderProgram GREEDY_OPAQUE_BLOCK_SHADER = new ShaderProgram(
            Gdx.files.internal("voxel/shaders/core/blockface/greedy/greedy_opaque_face.vsh"),
            Gdx.files.internal("voxel/shaders/core/blockface/greedy/greedy_opaque_face.fsh")
    );

    public static final ShaderProgram SINGLE_OPAQUE_BLOCK_SHADER = new ShaderProgram(
            Gdx.files.internal("voxel/shaders/core/blockface/single/single_opaque_face.vsh"),
            Gdx.files.internal("voxel/shaders/core/blockface/single/single_opaque_face.fsh")
    );

    public static void initShaders() {
        if (!GREEDY_OPAQUE_BLOCK_SHADER.isCompiled()) {
            throw new GdxRuntimeException("Shader-Fehler:\n" + GREEDY_OPAQUE_BLOCK_SHADER.getLog());
        }
    }
}
