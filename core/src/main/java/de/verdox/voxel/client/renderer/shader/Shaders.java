package de.verdox.voxel.client.renderer.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import de.verdox.voxel.client.assets.TextureAtlasManager;

public class Shaders {
    public static final VertexAttribute[] SINGLE_OPAQUE_ATTRIBUTES_ARRAY = new VertexAttribute[]{
            new VertexAttribute(VertexAttributes.Usage.Position, 1, "a_position_and_ao"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 1, "a_texCoord_and_light")
    };

    public static final VertexAttributes SINGLE_OPAQUE_ATTRIBUTES = new VertexAttributes(SINGLE_OPAQUE_ATTRIBUTES_ARRAY);

    public static final ShaderProgram SINGLE_PER_CORNER_OPAQUE_BLOCK_SHADER = new ShaderProgram(
            Gdx.files.internal("voxel/shaders/core/blockface/single/single_opaque_face.vsh"),
            Gdx.files.internal("voxel/shaders/core/blockface/single/single_opaque_face.fsh")
    );

    public static final ShaderProgram SINGLE_INSTANCED_OPAQUE_BLOCK_SHADER = new ShaderProgram(
            Gdx.files.internal("voxel/shaders/core/blockface/single/instanced_single_opaque_face.vsh"),
            Gdx.files.internal("voxel/shaders/core/blockface/single/single_opaque_face.fsh")
    );

    public static void initShaders() {
        initShader(SINGLE_PER_CORNER_OPAQUE_BLOCK_SHADER);
        initShader(SINGLE_INSTANCED_OPAQUE_BLOCK_SHADER);
    }

    private static void initShader(ShaderProgram shaderProgram) {
        if (!shaderProgram.isCompiled()) {
            throw new GdxRuntimeException("Shader [" + shaderProgram.getHandle() + "] compiling error:\n" + shaderProgram.getLog());
        }
    }

    private static int currentBoundShaderHandle = -1;

    public static void resetCurrentShader() {
        currentBoundShaderHandle = -1;
    }

    public static void loadShaderForBlockRendering(ShaderProgram shaderProgram) {
        if (currentBoundShaderHandle != shaderProgram.getHandle()) {
            shaderProgram.bind();
            currentBoundShaderHandle = shaderProgram.getHandle();
        }

        shaderProgram.setUniformf("atlasSize", TextureAtlasManager.getInstance().getBlockTextureAtlasSize());
        shaderProgram.setUniformf("blockTextureSize", TextureAtlasManager.getInstance().getBlockTextureSize());
    }
}
