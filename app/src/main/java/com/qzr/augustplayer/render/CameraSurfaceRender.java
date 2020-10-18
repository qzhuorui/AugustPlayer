package com.qzr.augustplayer.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.qzr.augustplayer.utils.GlesUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: CameraSurfaceRender
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/9/5 10:55
 */
public class CameraSurfaceRender implements GLSurfaceView.Renderer {

    private static final String TAG = "CameraSurfaceRender";

    private CameraSufaceRenderCallback mCallback;
    private RenderDrawerGroups renderDrawerGroups;

    private int mCameraTextureId;//OES纹理ID
    private SurfaceTexture mCameraTexture;

    private float[] mTransformMatrix;
    private long timestamp;

    public CameraSurfaceRender(Context context) {
        this.renderDrawerGroups = new RenderDrawerGroups(context);
        mTransformMatrix = new float[16];
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraTextureId = GlesUtil.createCameraTexture();//生成纹理ID，绑定到OES上，接收camera原始数据
        renderDrawerGroups.setInputTexture(mCameraTextureId);//传入OES纹理Id；感觉还是和设置水印那里一样，其实是数据的传递，纹理上承载着数据，纹理传递
        renderDrawerGroups.create();
        //根据mCameraTextureId创建对应Surface
        initCameraTexture();
        if (mCallback != null) {
            mCallback.onCreate();
        }
    }

     /**
      * @description 创建textureView && set callback
      * @date: 2020/9/5 11:05
      * @author: qzhuorui
      */
    private void initCameraTexture() {
        /**
         * 可以这样理解，
         * 就是我们通过这个SurfaceTexture从Camera接取预览帧的图像流，
         * 然后我们就可以通过其绑定的GL纹理对象，直接按照纹理使用绘制了
         * 通过textureId数据互通了
         */
        mCameraTexture = new SurfaceTexture(mCameraTextureId);//根据生成的纹理，新建surfaceTexture
        //用于让SurfaceTexture的使用者知道有新数据到来
        mCameraTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mCallback != null) {
                    //刷新控件，可以触发onDrawFrame
                    mCallback.onRequestRender();
                }
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "currentEGLContext: " + EGL14.eglGetCurrentContext().toString());
        renderDrawerGroups.surfaceChangedSize(width, height);
        if (mCallback != null) {
            mCallback.onChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mCameraTexture!=null){
            //更新预览上的图像，由onRequestRender通知触发
            mCameraTexture.updateTexImage();//根据内容流中最近的图像更新SurfaceTexture对应的GL纹理对象，也是告诉camera我已经使用这帧图像了

            timestamp = mCameraTexture.getTimestamp();//获取最近updateTexImage的时间戳
            mCameraTexture.getTransformMatrix(mTransformMatrix);//获取最近updateTexImage导致的4X4纹理坐标变化矩阵

            renderDrawerGroups.draw(timestamp, mTransformMatrix);
        }
        if (mCallback != null) {
            mCallback.onDraw();
        }
    }

    public SurfaceTexture getCameraSurfaceTexture() {
        return mCameraTexture;
    }

    public void setCallback(CameraSufaceRenderCallback mCallback) {
        this.mCallback = mCallback;
    }

    public void releaseSurfaceTexture() {
        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
    }

    public void resumeSurfaceTexture() {
        //重新根据纹理Id生成textureView
        initCameraTexture();
    }

    public void startRecord() {
        renderDrawerGroups.startRecord();
    }

    public void stopRecord() {
        renderDrawerGroups.stopRecord();
    }

    public interface CameraSufaceRenderCallback {
        void onRequestRender();

        void onCreate();

        void onChanged(int width, int height);

        void onDraw();
    }

}
