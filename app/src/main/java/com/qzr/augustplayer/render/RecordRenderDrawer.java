package com.qzr.augustplayer.render;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.qzr.augustplayer.encode.VideoEncodeService;
import com.qzr.augustplayer.utils.EGLHelper;
import com.qzr.augustplayer.utils.GlesUtil;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: RecordRenderDrawer
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/9/5 12:50
 */
public class RecordRenderDrawer extends BaseRenderDrawer implements Runnable {

    private static final String TAG = "RecordRenderDrawer";

    //绘制的纹理ID
    private int mTextureId;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;

    private Handler mMsgHandler;
    private EGLHelper mEglHelper;

    private VideoEncodeService videoEncodeService;
    private boolean isRecording;

    private int av_Position;
    private int af_Position;
    private int s_Texture;


    public RecordRenderDrawer(Context context) {
        this.mTextureId = 0;
        this.mEglHelper = null;
        this.videoEncodeService = null;
        this.isRecording = false;
        new Thread(this).start();
    }

    @Override
    public void run() {
        Looper.prepare();
        mMsgHandler = new MsgHandler();//创建自己的GLThread
        Looper.loop();
    }

    @Override
    public void create() {
        //override method 暂不去create program
        mEglContext = EGL14.eglGetCurrentContext();//创建EGL环境，在GLSurface的GLThread中获取EGLContext（存放在ThreadLocal中）
    }

    @Override
    protected void onCreated() {
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
        initVertexBufferObjects();
        av_Position = GLES30.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES30.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES30.glGetUniformLocation(mProgram, "s_Texture");
        Log.e(TAG, "onCreated: error " + GLES30.glGetError());
    }

    @Override
    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    protected void onChanged(int width, int height) {
    }

    @Override
    public void draw(long timestamp, float[] transformMatrix) {
        if (isRecording) {
            Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_FRAME, timestamp);
            mMsgHandler.sendMessage(msg);
        }
    }

    @Override
    protected void onDraw(float[] transformMatrix) {
        clear();
        useProgram();
        viewPort(0, 0, width, height);

        GLES30.glEnableVertexAttribArray(av_Position);
        GLES30.glEnableVertexAttribArray(af_Position);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES30.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        GLES30.glUniform1i(s_Texture, 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VertexCount);

        GLES30.glDisableVertexAttribArray(av_Position);
        GLES30.glDisableVertexAttribArray(af_Position);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

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
        final String source = "precision mediump float;\n" +
                "varying vec2 v_texPo;\n" +
                "uniform sampler2D s_Texture;\n" +
                "void main() {\n" +
                "   vec4 tc = texture2D(s_Texture, v_texPo);\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                "}";
        return source;
    }

    public void startRecord() {
        Log.i(TAG, "startRecord: ");
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_START_RECORD, width, height, mEglContext);
        mMsgHandler.sendMessage(msg);
        isRecording = true;
    }

    public void stopRecord() {
        Log.i(TAG, "stopRecord: ");
        isRecording = false;
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MsgHandler.MSG_STOP_RECORD));
    }

    @SuppressLint("HandlerLeak")
    private class MsgHandler extends Handler {

        static final int MSG_START_RECORD = 1;
        static final int MSG_STOP_RECORD = 2;
        static final int MSG_FRAME = 3;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD: {
                    prepareVideoEncoder((EGLContext) msg.obj, msg.arg1, msg.arg2);
                    break;
                }
                case MSG_STOP_RECORD: {
                    stopVideoEncoder();
                    break;
                }
                case MSG_FRAME: {
                    drawFrame((long) msg.obj);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void prepareVideoEncoder(EGLContext context, int width, int height) {
        try {
            //根据当前线程的eglContext，创建openGl环境，配置
            mEglHelper = new EGLHelper();
            mEglHelper.createGL(context);//创建EGL上下文环境等

            videoEncodeService = VideoEncodeService.getInstance();
            videoEncodeService.startVideoEncode();//codec.start
            mEglSurface = mEglHelper.createWindowSurface(videoEncodeService.getInputSurface());//根据codec的surface创建eglSurface；创建要使用的渲染surface
            boolean error = mEglHelper.makeCurrent(mEglSurface);//在完成EGL的初始化之后,需要通过eglMakeCurrent()函数来将当前的上下文切换,这样opengl的函数才能启动作用。
            if (!error) {
                Log.e(TAG, "prepareVideoEncoder: make current error");
            }
            onCreated();
        } catch (Exception e) {
            Log.e(TAG, "prepareVideoEncoder: ");
            e.printStackTrace();
        }
    }

    private void stopVideoEncoder() {
        videoEncodeService.drainEncoderData(true);
        if (mEglHelper != null) {
            mEglHelper.destroySurface(mEglSurface);
            mEglHelper.destroyGL();
            mEglSurface = EGL14.EGL_NO_SURFACE;
            mEglHelper = null;
            videoEncodeService = null;
        }
    }

    private void drawFrame(long timeStamp) {
        Log.i(TAG, "drawFrame: ");
        mEglHelper.makeCurrent(mEglSurface);
        videoEncodeService.drainEncoderData(false);
        onDraw(null);
        mEglHelper.setPresentationTime(mEglSurface, timeStamp);
        mEglHelper.swapBuffers(mEglSurface);
    }

}
