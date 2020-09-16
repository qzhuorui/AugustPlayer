package com.qzr.augustplayer.render;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.qzr.augustplayer.utils.GlesUtil;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: RenderDrawerGroups
 * @Description: 统一管理所有的RenderDrawer 和 FBO,包括创建FBO，控制绘制顺序，是否需要绘制到FBO中。是一个manager
 * @Author: qzhuorui
 * @CreateDate: 2020/9/5 10:57
 */
public class RenderDrawerGroups {

    private static final String TAG = "RenderDrawerGroups";

    private OriginalRenderDrawer mOriginalDrawer;
    private DisplayRenderDrawer mDisplayDrawer;
    private RecordRenderDrawer mRecordDrawer;

    private int mInputTexture;//纹理ID
    private int mFrameBufferObject;//FBO帧缓冲区

    /**
     * @description 初始化被管理类，获取引用句柄
     * @date: 2020/9/5 11:19
     * @author: qzhuorui
     */
    public RenderDrawerGroups(Context context) {
        this.mInputTexture = 0;
        this.mFrameBufferObject = 0;
        this.mOriginalDrawer = new OriginalRenderDrawer();
        this.mDisplayDrawer = new DisplayRenderDrawer();
        this.mRecordDrawer = new RecordRenderDrawer(context);
    }

    /**
     * @description 接收OES纹理Id
     * @date: 2020/9/5 11:17
     * @author: qzhuorui
     */
    public void setInputTexture(int texture) {
        this.mInputTexture = texture;//绑定到OES上的纹理，接收camera原始数据
    }

    /**
     * @description 对应Render的surfaceCreated生命周期方法
     * @date: 2020/9/5 11:20
     * @author: qzhuorui
     */
    public void create() {
        this.mOriginalDrawer.create();
        this.mDisplayDrawer.create();
        this.mRecordDrawer.create();
    }

    /**
     * @description 对应Render的surfaceChanged生命周期方法
     * @date: 2020/9/5 11:23
     * @author: qzhuorui
     */
    public void surfaceChangedSize(int width, int height) {
        mFrameBufferObject = GlesUtil.createFrameBuffer();//创建FBO

        mOriginalDrawer.surfaceChangedSize(width, height);
        mDisplayDrawer.surfaceChangedSize(width, height);
        mRecordDrawer.surfaceChangedSize(width, height);

        this.mOriginalDrawer.setInputTextureId(mInputTexture);//传入OES纹理ID
        int textureId = this.mOriginalDrawer.getOutputTextureId();//获取onCreate()创建的2D纹理

        mDisplayDrawer.setInputTextureId(textureId);//传入2D纹理ID
        mRecordDrawer.setInputTextureId(textureId);//传入2D纹理ID
    }

    /**
     * @description 对应Render的drawFrame生命周期方法
     * @date: 2020/9/5 11:25
     * @author: qzhuorui
     */
    public void draw(long timestamp, float[] transformMatrix) {
        //控制渲染顺序，可以选择绘制到预览或仅绘制到录制层中
        if (mInputTexture == 0 || mFrameBufferObject == 0) {
            Log.d(TAG, "draw: mInputTexture or mFramebuffer or list is zero");
            return;
        }
        //将绑定到FBO中，最后转换成mOriginalDrawer中的sample2D纹理
        drawRender(mOriginalDrawer, true, timestamp, transformMatrix);
        //不绑定FBO，直接绘制到屏幕上
        drawRender(mDisplayDrawer, false, timestamp, transformMatrix);
        drawRender(mRecordDrawer, false, timestamp, transformMatrix);
    }

    /**
     * @description 控制渲染流程
     * @date: 2020/8/29 15:53
     * @author: qzhuorui
     */
    private void drawRender(BaseRenderDrawer drawer, boolean useFrameBuffer, long timestamp, float[] transformMatrix) {
        if (useFrameBuffer) {
            bindFrameBuffer(drawer.getOutputTextureId());//绑定到FBO中
        }
        drawer.draw(timestamp, transformMatrix);
        if (useFrameBuffer) {
            //解除FBO绑定，恢复默认的帧缓冲区
            unBindFrameBuffer();
        }
    }

    /**
     * @description DisplayRenderDrawer的GL_TEXTURE_2D纹理到FBO上；纹理和FBO绑定，后续的绘制动作就会存储FBO上
     * @date: 2020/8/29 15:45
     * @author: qzhuorui
     */
    private void bindFrameBuffer(int textureId) {
        //通过绑定纹理对象来锁定挂接区
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferObject);//绑定一个命名的帧缓冲区对象（FBO），符号常量必须是GL_FRAMEBUFFER
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureId, 0);//将纹理图像添加到FBO，符号常量必须是GL_FRAMEBUFFER。
    }

    /**
     * @description 解除FBO绑定，恢复默认的帧缓冲区
     * @date: 2020/8/29 15:54
     * @author: qzhuorui
     */
    private void unBindFrameBuffer() {
        //告诉OpenGL，我们已经不需要在FBO上操作了
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    public void startRecord() {
        mRecordDrawer.startRecord();
    }

    public void stopRecord() {
        mRecordDrawer.stopRecord();
    }
}
