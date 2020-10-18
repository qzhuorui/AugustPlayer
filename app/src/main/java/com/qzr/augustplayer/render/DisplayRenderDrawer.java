package com.qzr.augustplayer.render;

import android.opengl.GLES30;

import com.qzr.augustplayer.base.Base;
import com.qzr.augustplayer.utils.AssetsUtils;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: DisplayRenderDrawer
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/9/5 12:37
 */
public class DisplayRenderDrawer extends BaseRenderDrawer {

    private static final String TAG = "DisplayRenderDrawer";

    private int av_Position;
    private int af_Position;
    private int s_Texture;

    private int mTextureId;

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
        av_Position = GLES30.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES30.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES30.glGetUniformLocation(mProgram, "s_Texture");
    }

    @Override
    protected void onDraw(float[] transformMatrix) {
        GLES30.glEnableVertexAttribArray(av_Position);
        GLES30.glEnableVertexAttribArray(af_Position);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES30.GL_FLOAT, false, 0, 0);
        //用GPU中的缓冲数据，不在RAM中取数据，所以后2个参数为0
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES30.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        GLES30.glUniform1i(s_Texture, 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        GLES30.glDisableVertexAttribArray(af_Position);
        GLES30.glDisableVertexAttribArray(av_Position);
    }

    /**
     * @description 传入的2D纹理
     * @date: 2020/9/13 11:43
     * @author: qzhuorui
     */
    @Override
    public void setInputTextureId(int textureId) {
        this.mTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mTextureId;
    }

    @Override
    protected String getVertexSource() {
        return AssetsUtils.getVertexStrFromAssert(Base.CURRENT_APP, "vertex_display");
    }

    @Override
    protected String getFragmentSource() {
        return AssetsUtils.getFragmentStrFromAssert(Base.CURRENT_APP, "fragment_display");
    }
}
