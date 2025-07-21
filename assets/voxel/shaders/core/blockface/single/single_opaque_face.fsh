#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;

varying vec2 v_texCoord;
varying float v_light;
varying float v_ambient_occlusion;

float getNibble(inout float data, float div) {
    float comp = floor(data / div);
    data -= comp * div;
    return comp;
}

void main() {
    vec2 local = fract(v_texCoord);

    float light = v_light;

    // Sky in Bits 12–15: div = 2^12 = 4096
    float sky = getNibble(light, 4096.0) / 15;
    // Red in Bits 8–11:  div = 2^8  = 256
    float red = getNibble(light, 256.0) / 15;
    // Green in Bits 4–7: div = 2^4  = 16
    float green = getNibble(light, 16.0) / 15;
    // Blue in Bits 0–3:  div = 2^0  = 1
    float blue = getNibble(light, 1.0) / 15;

    vec3 blockLight = vec3(red, green, blue);
    vec4 texture = texture2D(u_texture, local);

    //vec3 lit = texture.rgb * (mix(0.2, 0.8, sky)/** + blockLight*/);
    vec3 lit = texture.rgb * (mix(0.4, 0.8, v_ambient_occlusion));

    gl_FragColor = vec4(lit, texture.a);
}


