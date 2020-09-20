package com.qzr.augustplayer.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.qzr.augustplayer.base.Base;
import com.qzr.augustplayer.utils.AssetsUtils;
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
        //水印，录制等需要的是2D纹理，Camera是OES，借助FBO实现
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
        //用GPU中的缓冲数据，不在RAM中取数据，所以后2个参数为0
        GLES30.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES30.GL_FLOAT, false, 0, 0);
        //backCamera
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mBackTextureBufferId);
        GLES30.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        //绘制
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);//激活纹理单元
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mInputTextureId);//绑定OES纹理
        GLES30.glUniform1i(s_Texture, 0);//将纹理设置给Shader

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VertexCount);

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES30.glDisableVertexAttribArray(av_Position);
        GLES30.glDisableVertexAttribArray(af_Position);
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
        return AssetsUtils.getVertexStrFromAssert(Base.CURRENT_APP, "vertex_original");
    }

    @Override
    protected String getFragmentSource() {
        return AssetsUtils.getFragmentStrFromAssert(Base.CURRENT_APP, "fragment_original");
    }
}
