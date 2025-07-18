package de.verdox.voxel.client.shader;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;

public class OpaqueBlockShaderProvider extends BaseShaderProvider {
    @Override
    protected Shader createShader(Renderable renderable) {
        return new DefaultShader(renderable, new DefaultShader.Config(Shaders.OPAQUE_BLOCK_SHADER.getVertexShaderSource(), Shaders.OPAQUE_BLOCK_SHADER.getFragmentShaderSource()));
    }
}
