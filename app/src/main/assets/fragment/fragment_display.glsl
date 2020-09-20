precision mediump float;
varying vec2 v_texPo;
uniform sampler2D s_Texture;

void main() {
    gl_FragColor = texture2D(s_Texture, v_texPo);
}
