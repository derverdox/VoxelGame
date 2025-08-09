uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform float atlasSize;
uniform float blockTextureSize;

attribute float a_position_and_ao;
attribute float a_texCoord_and_light;

varying vec2 v_texCoord;
varying float v_sky_light;
varying float v_block_light_red;
varying float v_block_light_green;
varying float v_block_light_blue;
varying float v_ambient_occlusion;


void main() {
    uint bits = floatBitsToUint(a_texCoord_and_light);
    uint uIndex = bits & 0x3Fu;
    uint vIndex = (bits >> 6) & 0x3Fu;

    v_sky_light = float((bits >> 12) & 0xFu);
    v_block_light_red = float((bits >> 16) & 0xFu);
    v_block_light_green = float((bits >> 20) & 0xFu);
    v_block_light_blue = float((bits >> 24) & 0xFu);

    float atlasStep = blockTextureSize / atlasSize;
    vec2 tileOffset = vec2(uIndex, vIndex) * atlasStep;

    v_texCoord = tileOffset;

    bits = floatBitsToUint(a_position_and_ao);

    int x = int(bits & 0x3FFu);
    int y = int((bits >> 10) & 0x3FFu);
    int z = int((bits >> 20) & 0x3FFu);
    int ao = int((bits >> 30) & 0x3u);
    v_ambient_occlusion = 1.0 - ao / 3.0;

    gl_Position = u_projViewTrans * u_worldTrans * vec4(vec3(x, y, z), 1.0);
}
