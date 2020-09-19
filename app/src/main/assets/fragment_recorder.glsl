#version 120
precision mediump float;
varying vec2 v_texPo;
uniform sampler2D s_Texture;

void main() {
    vec4 tc = texture2D(s_Texture, v_texPo);
    gl_FragColor = texture2D(s_Texture, v_texPo);
}
