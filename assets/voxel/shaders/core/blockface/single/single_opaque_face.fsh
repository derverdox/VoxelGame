#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;

varying vec2 v_texCoord;

varying float v_sky_light;
varying float v_block_light_red;
varying float v_block_light_green;
varying float v_block_light_blue;

varying float v_ambient_occlusion;

void main() {
    vec2 local = fract(v_texCoord);

    vec3 blockLight = vec3(v_block_light_red, v_block_light_green, v_block_light_blue);
    vec4 texture = texture2D(u_texture, v_texCoord);

    vec3 lit = texture.rgb
                * (mix(0.4, 0.8, v_ambient_occlusion));

    gl_FragColor = vec4(lit, texture.a);
}


