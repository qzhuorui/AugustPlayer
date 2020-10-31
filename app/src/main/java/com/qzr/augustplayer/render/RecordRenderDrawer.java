package com.qzr.augustplayer.render;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.qzr.augustplayer.base.Base;
import com.qzr.augustplayer.encode.VideoEncodeService;
import com.qzr.augustplayer.manager.RecorderManager;
import com.qzr.augustplayer.utils.AssetsUtils;
import com.qzr.augustplayer.utils.EGLHelper;
import com.qzr.augustplayer.utils.GlesUtil;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: RecordRenderDrawer
 * @Description: 要在自定义的线程中编解码所以需要使用EGL，因为没在GLThread所以需要自己创建EGL环境
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

    private RecorderManager recorderManager;
    private boolean isRecording;

    private int av_Position;
    private int af_Position;
    private int s_Texture;


    public RecordRenderDrawer(Context context) {
        this.mTextureId = 0;
        this.mEglHelper = null;
        this.recorderManager = null;
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
        //这里其实是：利用共享GLSurfaceview的EGLContext创建EGL环境。给我们自己的线程提供使用
        mEglContext = EGL14.eglGetCurrentContext();//在GLThread中，获取和当前线程绑定的EGLContext（存放于ThreadLocal中）
    }

    /**
     * @description 父类中创建了program，缓冲区句柄后的callback
     * @date: 2020/9/13 11:27
     * @author: qzhuorui
     */
    @Override
    protected void onCreated() {
        //必须要在其Context环境中，才能执行OpenGL方法
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
        initVertexBufferObjects();
        av_Position = GLES30.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES30.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES30.glGetUniformLocation(mProgram, "s_Texture");
        Log.e(TAG, "onCreated: error " + GLES30.glGetError());
    }

    @Override
    public void surfaceChangedSize(int width, int height) {
        Base.SV.setVideoWidth(width);
        Base.SV.setVideoHeight(height);
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
        return AssetsUtils.getVertexStrFromAssert(Base.CURRENT_APP, "vertex_recorder");
    }

    @Override
    protected String getFragmentSource() {
        return AssetsUtils.getFragmentStrFromAssert(Base.CURRENT_APP, "fragment_recorder");
    }

    public void startRecord() {
        Log.i(TAG, "startRecord: ");
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_START_RECORD, mEglContext);
        mMsgHandler.sendMessage(msg);
        isRecording = true;
    }

    public void stopRecord() {
        Log.i(TAG, "stopRecord: ");
        isRecording = false;
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MsgHandler.MSG_STOP_RECORD));
    }

    private class MsgHandler extends Handler {

        static final int MSG_START_RECORD = 1;
        static final int MSG_STOP_RECORD = 2;
        static final int MSG_FRAME = 3;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD: {
                    prepareVideoEncoder((EGLContext) msg.obj);
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

    private void prepareVideoEncoder(EGLContext context) {
        try {
            //根据当前线程的eglContext，创建openGl环境，配置
            mEglHelper = new EGLHelper();
            mEglHelper.createGL(context);//创建EGL上下文环境等

            recorderManager = RecorderManager.getInstance().buildRecorder();
            boolean result = recorderManager.startRecord(false);
            if (!result) {
                Log.e(TAG, "prepareVideoEncoder: startMediaModule failure");
                return;
            }
            mEglSurface = mEglHelper.createWindowSurface(recorderManager.getCodecInputSurface());//根据codec的surface创建eglSurface；创建要使用的渲染surface
            //注意：OpenGL.ES的渲染必须新开一个线程，并为该线程绑定显示设备和context；OpenGL的指令必须在其Context环境中才能执行，必须makeCurrent
            boolean error = mEglHelper.makeCurrent(mEglSurface);//在完成EGL的初始化之后,需要通过eglMakeCurrent()函数来将当前的上下文切换,这样opengl的函数才能启动作用。
            if (!error) {
                Log.e(TAG, "prepareVideoEncoder: make current error");
            }
            onCreated();//切换环境后，才能使用OpenGL函数
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopVideoEncoder() {
        VideoEncodeService.getInstance().drainEncoderData(true);
        recorderManager.stopRecord(false);
        if (mEglHelper != null) {
            mEglHelper.destroySurface(mEglSurface);
            mEglHelper.destroyGL();
            mEglSurface = EGL14.EGL_NO_SURFACE;
            mEglHelper = null;
            recorderManager = null;
            //防止内存泄漏
            mMsgHandler.removeCallbacksAndMessages(null);
        }
    }

    private void drawFrame(long timeStamp) {
        mEglHelper.makeCurrent(mEglSurface);
        VideoEncodeService.getInstance().drainEncoderData(false);
        onDraw(null);//draw到mEglSurface，也就是draw到编码器，codec也就得到了数据!!!
        mEglHelper.setPresentationTime(mEglSurface, timeStamp);
        mEglHelper.swapBuffers(mEglSurface);
    }

}
