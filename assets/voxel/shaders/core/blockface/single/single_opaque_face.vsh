attribute vec3 a_position;
attribute vec2 a_texCoord0;
attribute float a_light;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

// U / V
varying vec2 v_texCoord;

// Light
varying float v_sky_light;
varying float v_block_light_red;
varying float v_block_light_green;
varying float v_block_light_blue;

// AO
varying float v_ambient_occlusion;

float getNibble(inout float data, float div) {
    float comp = floor(data / div);
    data -= comp * div;
    return comp;
}

void main() {
    v_texCoord = a_texCoord0;

    float light = a_light;
    // 1) AO in Bits 16–17: div = 2^16 = 65536
    //    getNibble entfernt die Bits 16–17 aus 'light'
    v_ambient_occlusion = 1.0 - (getNibble(light, 65536.0) / 3.0);
    // Sky in Bits 12–15: div = 2^12 = 4096
    v_sky_light = getNibble(light, 4096.0) / 15;
    // Red in Bits 8–11:  div = 2^8  = 256
    v_block_light_red = getNibble(light, 256.0) / 15;
    // Green in Bits 4–7: div = 2^4  = 16
    v_block_light_green = getNibble(light, 16.0) / 15;
    // Blue in Bits 0–3:  div = 2^0  = 1
    v_block_light_blue = getNibble(light, 1.0) / 15;

    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
}
