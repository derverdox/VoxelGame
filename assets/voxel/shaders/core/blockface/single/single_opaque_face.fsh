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

vec2 saturate(vec2 x) {
    return clamp(x, vec2(0.0), vec2(1.0));
}

vec2 magnify(vec2 uv, vec2 resolution) {
    uv *= resolution;
    return (saturate(fract(uv) / saturate(fwidth(uv))) + floor(uv) - 0.5) / resolution;
}


void main() {
    vec2 resolution = vec2(textureSize(u_texture, 0));


    vec2 local = fract(v_texCoord);

    vec3 blockLight = vec3(v_block_light_red, v_block_light_green, v_block_light_blue);
    //vec4 texture = texture2D(u_texture, magnify(v_texCoord, resolution));
    vec4 texture = texture2D(u_texture, v_texCoord);

    vec3 lit = texture.rgb * (mix(0.4, 0.8, v_ambient_occlusion));

    gl_FragColor = vec4(lit, texture.a);

}


