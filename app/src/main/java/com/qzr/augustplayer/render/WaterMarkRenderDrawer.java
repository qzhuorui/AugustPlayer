package com.qzr.augustplayer.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;

import com.qzr.augustplayer.R;
import com.qzr.augustplayer.base.Base;
import com.qzr.augustplayer.utils.AssetsUtils;
import com.qzr.augustplayer.utils.GlesUtil;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: WaterMarkRenderDrawer
 * @Description: 水印渲染管理
 * @Author: qzhuorui
 * @CreateDate: 2020/9/19 11:01
 */
public class WaterMarkRenderDrawer extends BaseRenderDrawer {

    private static final String TAG = "WaterMarkRenderDrawer";

    private int mMarkTextureId;
    private int mInputTextureId;
    private Bitmap mBitmap;
    private int avPosition;
    private int afPosition;
    private int sTexture;

    public WaterMarkRenderDrawer(Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inSampleSize = 2;
        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.watermark, options);
    }

    @Override
    protected void onCreated() {

    }

    @Override
    protected void onChanged(int width, int height) {
        mMarkTextureId = GlesUtil.loadBitmapTexture(mBitmap);
        avPosition = GLES30.glGetAttribLocation(mProgram, "av_Position");
        afPosition = GLES30.glGetAttribLocation(mProgram, "af_Position");
        sTexture = GLES30.glGetUniformLocation(mProgram, "sTexture");
    }

    @Override
    public void draw(long timestamp, float[] transformMatrix) {
        //渲染绘制水印图片，Blend颜色混合
        useProgram();
        viewPort(40, 75, mBitmap.getWidth() * 2, mBitmap.getHeight() * 2);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_COLOR, GLES30.GL_ONE);//源因子，目的因子。源*源因子+目的&目的因子
        onDraw(transformMatrix);
        GLES30.glDisable(GLES30.GL_BLEND);
    }

    @Override
    protected void onDraw(float[] transformMatrix) {
        GLES30.glEnableVertexAttribArray(avPosition);
        GLES30.glEnableVertexAttribArray(afPosition);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glVertexAttribPointer(avPosition, CoordsPerVertexCount, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mFrameTextureBufferId);
        GLES30.glVertexAttribPointer(afPosition, CoordsPerTextureCount, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mMarkTextureId);
        GLES30.glUniform1i(sTexture, 0);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES30.glDisableVertexAttribArray(avPosition);
        GLES30.glDisableVertexAttribArray(afPosition);
    }

    @Override
    public void setInputTextureId(int textureId) {
        this.mInputTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mInputTextureId;
    }

    @Override
    protected String getVertexSource() {
        return AssetsUtils.getVertexStrFromAssert(Base.CURRENT_APP, "vertex_watermark");
    }

    @Override
    protected String getFragmentSource() {
        return AssetsUtils.getFragmentStrFromAssert(Base.CURRENT_APP, "fragment_watermark");
    }
}
