package com.qzr.augustplayer.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.qzr.augustplayer.render.CameraSurfaceRender;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.widget
 * @ClassName: CameraGLSurfaceView
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/8/23 9:59
 */
public class CameraGLSurfaceView extends GLSurfaceView implements CameraSurfaceRender.CameraSufaceRenderCallback {
    private static final String TAG = "CameraGLSurfaceView";

    private CameraSurfaceRender mRender;
    private CameraGLSurfaceViewCallback mCallback;

    public CameraGLSurfaceView(Context context) {
        super(context);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(3);
        setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);//激活log或错误检测
        mRender = new CameraSurfaceRender(context);
        mRender.setCallback(this);
        setRenderer(mRender);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onRequestRender() {
        requestRender();
    }

    @Override
    public void onCreate() {
        if (mCallback != null) {
            mCallback.onSurfaceViewCreate(getSurfaceTexture());
        }
    }

    @Override
    public void onChanged(int width, int height) {
        if (mCallback != null) {
            mCallback.onSurfaceViewChange(width, height);
        }
    }

    @Override
    public void onDraw() {

    }

    public SurfaceTexture getSurfaceTexture() {
        return mRender.getCameraSurfaceTexture();
    }

    public void releaseSurfaceTexture() {
        mRender.releaseSurfaceTexture();
    }

    public void resumeSurfaceTexture() {
        mRender.resumeSurfaceTexture();
    }

    public void startRecord() {
        mRender.startRecord();
    }

    public void stopRecord() {
        mRender.stopRecord();
    }

    public void setCallback(CameraGLSurfaceViewCallback mCallback) {
        this.mCallback = mCallback;
    }

    public interface CameraGLSurfaceViewCallback {
        void onSurfaceViewCreate(SurfaceTexture texture);

        void onSurfaceViewChange(int width, int height);
    }
}
