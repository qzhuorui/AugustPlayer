package com.qzr.augustplayer.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.qzr.augustplayer.utils.GlesUtil;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: OriginalRenderDrawer
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/9/5 11:28
 */
public class OriginalRenderDrawer extends BaseRenderDrawer {

    private static final String TAG = "OriginalRenderDrawer";

    private int av_Position;
    private int af_Position;
    private int s_Texture;

    private int mInputTextureId;
    private int mOutputTextureId;

    /**
     * @description 父类中创建了program，缓冲区句柄后的callback
     * @date: 2020/9/13 11:27
     * @author: qzhuorui
     */
    @Override
    protected void onCreated() {
    }

    @Override
    protected void onChanged(int width, int height) {
        mOutputTextureId = GlesUtil.createFrameTexture(width, height);//创建2D纹理ID

        //拿到GL SL中声明的变量的对应引用
        av_Position = GLES30.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES30.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES30.glGetUniformLocation(mProgram, "s_Texture");
    }

    @Override
    protected void onDraw(float[] transformMatrix) {
        if (mInputTextureId == 0 || mOutputTextureId == 0) {
            return;
        }
        GLES30.glEnableVertexAttribArray(av_Position);
        GLES30.glEnableVertexAttribArray(af_Position);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES30.GL_FLOAT, false, 0, 0);
        //backCamera
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mBackTextureBufferId);
        GLES30.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        //绘制
        bindTexture(mInputTextureId);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VertexCount);
        unBindTexure();

        GLES30.glDisableVertexAttribArray(av_Position);
        GLES30.glDisableVertexAttribArray(af_Position);
    }

    private void bindTexture(int textureId) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);//激活纹理单元
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);//绑定IES纹理
        GLES30.glUniform1i(s_Texture, 0);//将纹理设置给Shader
    }

    private void unBindTexure() {
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    @Override
    public void setInputTextureId(int textureId) {
        //传入OES纹理ID
        mInputTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        //返回创建的2D纹理ID，绑定到FBO上
        return mOutputTextureId;
    }

    @Override
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_texPo; " +
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    @Override
    protected String getFragmentSource() {
        final String source = "#extension GL_OES_EGL_image_external : require \n" +
                "precision mediump float; " +
                "varying vec2 v_texPo; " +
                "uniform samplerExternalOES s_Texture; " +
                "void main() { " +
                "   gl_FragColor = texture2D(s_Texture, v_texPo); " +
                "} ";
        return source;
    }
}
