package de.verdox.voxel.client.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Shaders {
    public static final ShaderProgram OPAQUE_BLOCK_SHADER = new ShaderProgram(
        Gdx.files.internal("voxel/shaders/core/opaque_block.vsh"),
            Gdx.files.internal("voxel/shaders/core/opaque_block.fsh")
                );
        public static void initShaders() {
            if (!OPAQUE_BLOCK_SHADER.isCompiled()) {
                throw new GdxRuntimeException("Shader-Fehler:\n" + OPAQUE_BLOCK_SHADER.getLog());
            }
        }
}
