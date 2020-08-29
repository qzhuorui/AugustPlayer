package com.qzr.augustplayer.view;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.qzr.augustplayer.R;
import com.qzr.augustplayer.base.BaseActivity;
import com.qzr.augustplayer.base.MessageWhat;
import com.qzr.augustplayer.manager.QzrCameraManager;
import com.qzr.augustplayer.manager.RecorderManager;
import com.qzr.augustplayer.service.CameraSensor;
import com.qzr.augustplayer.utils.HandlerProcess;
import com.qzr.augustplayer.widget.CameraGLSurfaceView;
import com.qzr.augustplayer.widget.FocusViewWidget;

import butterknife.BindView;

public class RecorderActivity extends BaseActivity implements  View.OnTouchListener, CameraSensor.CameraSensorListener, View.OnClickListener, View.OnLongClickListener, HandlerProcess.HandlerCallback, CameraGLSurfaceView.CameraGLSurfaceViewCallback {

    private static final String TAG = "RecorderActivity";

    @BindView(R.id.sv_cameragl)
    CameraGLSurfaceView cameraGLSurfaceView;
    @BindView(R.id.btn_video)
    Button btnVideo;
    @BindView(R.id.focus_view)
    FocusViewWidget focusViewWidget;

    private boolean isFocusing = false;
    private CameraSensor mCameraSensor;

    private boolean isRecording = false;
    private RecorderManager recorderManager;

    SurfaceTexture mSurfaceTexture;

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        cameraGLSurfaceView.setCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        QzrCameraManager.getInstance().stopPreView();
        focusViewWidget.cancelFocus();
        mCameraSensor.stopCameraSensor();
    }

    @Override
    public void beforeSetContentView() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_recorder;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initView() {
        mCameraSensor = new CameraSensor(this);
        mCameraSensor.setCameraSensorListener(this);

        cameraGLSurfaceView.setOnTouchListener(this);
        btnVideo.setOnClickListener(this);
        btnVideo.setOnLongClickListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            focus((int) event.getX(), (int) event.getY(), false);
            return true;
        }
        return false;
    }

    private void focus(int x, int y, final boolean autoFocus) {
        if (isFocusing) {
            return;
        }
        isFocusing = true;

        Point focusPoint = new Point(x, y);
        Size screenSize = new Size(cameraGLSurfaceView.getWidth(), cameraGLSurfaceView.getHeight());

        if (!autoFocus) {
            focusViewWidget.beginFocus(x, y);
        }

        QzrCameraManager.getInstance().activeCameraFocus(focusPoint, screenSize, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                isFocusing = false;
                if (!autoFocus) {
                    focusViewWidget.endFocus(success);
                }
            }
        });
    }

    @Override
    public void onRock() {
        focus(cameraGLSurfaceView.getWidth() / 2, cameraGLSurfaceView.getHeight() / 2, true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_video: {
                if (isRecording) {
                    QzrCameraManager.getInstance().setTakePic(true);
                } else {
                    RecorderManager.getInstance().takePicture();
                }
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (isRecording) {
            HandlerProcess.getInstance().post(MessageWhat.STOP_RECORDER, 0, this);
        } else {
            HandlerProcess.getInstance().post(MessageWhat.START_RECORDER, 0, this);
        }
        return true;
    }

    @Override
    public void handleMsg(int what, Object o) {
        switch (what) {
            case MessageWhat.STOP_RECORDER: {
                RecorderManager.getInstance().stopRecord();
                isRecording = false;
                break;
            }
            case MessageWhat.START_RECORDER: {
                recorderManager = RecorderManager.getInstance().buildRecorder();
                recorderManager.startRecord();
                isRecording = true;
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onSurfaceViewCreate(SurfaceTexture texture) {
        mSurfaceTexture = texture;
    }

    @Override
    public void onSurfaceViewChange(int width, int height) {
        QzrCameraManager.getInstance().buildCamera(mSurfaceTexture).startPreView();
        mCameraSensor.startCameraSensor();
    }
}
