attribute vec3 a_position;
attribute vec2 a_texCoord0;
attribute float a_light;
attribute float a_ambient_occlusion;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec2 v_texCoord;
varying float v_light;
varying float v_ambient_occlusion;

void main() {
    v_texCoord = a_texCoord0;
    v_light = a_light;
    v_ambient_occlusion = a_ambient_occlusion;
    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
}
