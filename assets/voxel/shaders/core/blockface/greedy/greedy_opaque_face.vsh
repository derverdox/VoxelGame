attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;
attribute vec2 a_greedy_start;
attribute vec2 a_greedy_end;
attribute float a_light;
attribute float a_ambient_occlusion;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec2 v_texCoord;
varying vec2 v_greedy_start;
varying vec2 v_greedy_end;
varying float v_light;
varying float v_ambient_occlusion;

void main() {
    v_texCoord = a_texCoord0;
    v_greedy_start = a_greedy_start;
    v_greedy_end = a_greedy_end;
    v_light = a_light;
    v_ambient_occlusion = a_ambient_occlusion;
    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
}
