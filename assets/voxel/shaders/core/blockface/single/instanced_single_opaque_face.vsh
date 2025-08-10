#version 330 core

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform float atlasSize;
uniform float blockTextureSize;

layout (location = 0) in vec2 a_corner_pos;
layout (location = 1) in float a_position_and_ao;
layout (location = 2) in float a_texCoord_and_light;

out vec2 v_texCoord;
out float v_sky_light;
out float v_block_light_red;
out float v_block_light_green;
out float v_block_light_blue;
out float v_ambient_occlusion;

const vec3 POS_X = vec3(1.0, 0.0, 0.0);
const vec3 NEG_X = vec3(-1.0, 0.0, 0.0);

const vec3 POS_Y = vec3(0.0, 1.0, 0.0);
const vec3 NEG_Y = vec3(0.0, -1.0, 0.0);

const vec3 POS_Z = vec3(0.0, 0.0, 1.0);
const vec3 NEG_Z = vec3(0.0, 0.0, -1.0);

const vec3 NORMALS[6] = vec3[6](NEG_X, POS_X, NEG_Y, POS_Y, NEG_Z, POS_Z);
const vec3 UDIRS[6] = vec3[6](POS_Y, POS_Y, POS_X, POS_X, NEG_X, POS_X);
const vec3 VDIRS[6] = vec3[6](NEG_Z, POS_Z, POS_Z, NEG_Z, POS_Y, POS_Y);

const float CUBE_BOUNDING_BOX_HALF = 0.5f;
const float LOD_SCALE = 1.0f;

float unpackCornerAO(uint aopacked, int cornerId) {
    uint s = (aopacked >> uint((cornerId) * 2)) & 0x3u;
    return 1.0 - (float(s) / 3.0);
}

vec3 computeCornerLocal(
    int dir, // 0..5
    vec3 blockIndex, // (x,y,z) aus dem Packing (Ganzzahl als float)
    float uGrowth, // meist 1.0
    float vGrowth // meist 1.0
) {
    vec3 n = NORMALS[dir];
    vec3 u = UDIRS[dir];
    vec3 v = VDIRS[dir];

    vec3 c0 = CUBE_BOUNDING_BOX_HALF * n - CUBE_BOUNDING_BOX_HALF * u - CUBE_BOUNDING_BOX_HALF * v;
    vec3 corner = c0 + u * (uGrowth * a_corner_pos.x) + v * (vGrowth * a_corner_pos.y);

    vec3 blockCenter = blockIndex + vec3(CUBE_BOUNDING_BOX_HALF);
    return (blockCenter + corner) * LOD_SCALE;
}

/** Variante, die direkt Weltkoordinaten (vor MVP) liefert */
vec4 computeCornerWorld(
    int dir, vec3 blockIndex,
    float uGrowth, float vGrowth
) {
    vec3 local = computeCornerLocal(dir, blockIndex, uGrowth, vGrowth);
    return u_worldTrans * vec4(local, 1.0);
}

void computeUV(uint packedBits, vec2 corner, float atlasStep) {
    uint uIdx = (packedBits >> 3) & 0x3Fu; // 6 bits
    uint vIdx = (packedBits >> 9) & 0x3Fu; // 6 bits

    vec2 tileOffset = vec2(uIdx, vIdx) * atlasStep;
    v_texCoord = tileOffset + corner * atlasStep;
}

void setCornerAmbientOcclusion(uint packedBits, int cId) {
    uint aoByte = (packedBits >> 24) & 0xFFu;
    v_ambient_occlusion = unpackCornerAO(aoByte, cId);
}

void setFaceLighting(uint packedBits) {
    uint sky = (packedBits >> 15) & 0xFu;
    uint red = (packedBits >> 19) & 0xFu;
    uint green = (packedBits >> 23) & 0xFu;
    uint blue = (packedBits >> 27) & 0xFu;

    v_sky_light = float(sky);
    v_block_light_red = float(red);
    v_block_light_green = float(green);
    v_block_light_blue = float(blue);
}

void main() {
    // Float1: x (8bits), y (8bits), z (8bits), aoPacked (8bits)
    // Float2: direction (3bits), u (6bits), v(6bits), sky (4bits), red (4bits), green (4bits), blue (4bits),

    int cid = gl_VertexID; // 0,1,2,3  entspricht (0,0),(0,1),(1,0),(1,1)
    vec2 c = a_corner_pos;

    float atlasStep = blockTextureSize / atlasSize;

    uint bits1 = floatBitsToUint(a_position_and_ao);
    uint bits2 = floatBitsToUint(a_texCoord_and_light);
    int dir = int(bits2 & 0x7u);

    int blockX = int(bits1 & 0xFFu);
    int blockY = int((bits1 >> 8) & 0xFFu);
    int blockZ = int((bits1 >> 16) & 0xFFu);

    setCornerAmbientOcclusion(bits1, cid);
    setFaceLighting(bits2);
    computeUV(bits2, c, atlasStep);


    vec3 blockIdx = vec3(float(blockX), float(blockY), float(blockZ));
    vec4 worldPos = computeCornerWorld(dir, blockIdx, 1.0, 1.0);

    gl_Position = u_projViewTrans * worldPos;
}
