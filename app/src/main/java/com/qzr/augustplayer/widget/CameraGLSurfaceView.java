package com.qzr.augustplayer.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.qzr.augustplayer.render.CameraGLSurfaceRender;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.widget
 * @ClassName: CameraGLSurfaceView
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/8/23 9:59
 */
public class CameraGLSurfaceView extends GLSurfaceView implements CameraGLSurfaceRender.CameraGLSufaceRenderCallback {
    private static final String TAG = "CameraGLSurfaceView";

    private CameraGLSurfaceRender mRender;
    private CameraGLSurfaceViewCallback mCallback;

    public CameraGLSurfaceView(Context context) {
        super(context);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        mRender = new CameraGLSurfaceRender(this);
        setRenderer(mRender);
        //dity脏模式，按需渲染
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRender.getmSurfaceTexture();
    }

    @Override
    public void onRequestRender() {
        requestRender();
    }

    @Override
    public void onCreate(SurfaceTexture texture) {
        if (mCallback != null) {
            mCallback.onSurfaceViewCreate(texture);
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

    public void setCallback(CameraGLSurfaceViewCallback mCallback) {
        this.mCallback = mCallback;
    }

    public interface CameraGLSurfaceViewCallback {
        void onSurfaceViewCreate(SurfaceTexture texture);
        void onSurfaceViewChange(int width, int height);
    }
}
