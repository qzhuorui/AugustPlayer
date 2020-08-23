package com.qzr.augustplayer.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: CameraGLSurfaceRender
 * @Description: render
 * @Author: qzhuorui
 * @CreateDate: 2020/8/23 10:34
 */
public class CameraGLSurfaceRender extends GLAbstractRender {
    private static final String TAG = "CameraGLSurfaceRender";

    private int mTexture;
    private SurfaceTexture mSurfaceTexture;

    private FloatBuffer vertexBuffer;
    private FloatBuffer backTextureBuffer;
    private FloatBuffer frontTextureBuffer;
    private CameraGLSufaceRenderCallback mRenderCallback;

    //创建顶点坐标
    private float vertexData[] = {   // in counterclockwise order:
            -1f, -1f, 0.0f, // 左下角
            1f, -1f, 0.0f, // 右下角
            -1f, 1f, 0.0f, // 左上角
            1f, 1f, 0.0f,  // 右上角
    };

    // 纹理坐标对应顶点坐标与后置摄像头映射
    private float backTextureData[] = {
            1f, 1f, // 右上角
            1f, 0f, // 右下角
            0f, 1f, // 左上角
            0f, 0f //  左下角
    };

    // 纹理坐标对应顶点坐标与前置摄像头映射
    private float frontTextureData[] = {
            0f, 1f, // 左上角
            0f, 0f, //  左下角
            1f, 1f, // 右上角
            1f, 0f // 右下角
    };

    // 每次取点的数量，必须为1,2,3,4
    private final int CoordsPerVertexCount = 3;
    // 顶点坐标数量
    private final int VertexCount = vertexData.length / CoordsPerVertexCount;
    // 一次取出的大小
    private final int VertexStride = CoordsPerVertexCount * 4;

    // 每次取点的数量，必须为1,2,3,4
    private final int CoordsPerTextureCount = 2;
    // 一次取出的大小
    private final int TextureStride = CoordsPerTextureCount * 4;

    private int av_Position;
    private int af_Position;
    private int s_Texture;

    public CameraGLSurfaceRender(CameraGLSufaceRenderCallback mRenderCallback) {
        this.mRenderCallback = mRenderCallback;

        //坐标数据转化为FloatBuffer
        this.vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        this.vertexBuffer.position(0);

        this.backTextureBuffer = ByteBuffer.allocateDirect(backTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(backTextureData);
        this.backTextureBuffer.position(0);

        this.frontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frontTextureData);
        this.frontTextureBuffer.position(0);
    }

    @Override
    protected void onCreate() {
        //创建外部纹理
        mTexture = loadExternelTexture();

        av_Position = GLES20.glGetAttribLocation(mProgramId, "av_Position");//返回属性变量的位置，顶点
        af_Position = GLES20.glGetAttribLocation(mProgramId, "af_Position");//片元

        s_Texture = GLES20.glGetUniformLocation(mProgramId, "s_Texture");//返回统一变量的位置

        // get surface
        mSurfaceTexture = new SurfaceTexture(mTexture);
        //当mSurfaceTexture发生数据变化，也就是camera数据变化时候
        //会通过onFrameAvailable回调进来，我们通知CameraGLSurfaceView重新绘制onRequestRender就可以了
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mRenderCallback != null) {
                    mRenderCallback.onRequestRender();
                }
            }
        });
        if (mRenderCallback != null) {
            //surface传递出去
            mRenderCallback.onCreate(mSurfaceTexture);
        }
    }

    @Override
    protected void onChanged() {
        if (mRenderCallback != null) {
            mRenderCallback.onChanged(width,height);
        }
    }

    @Override
    protected void onDraw() {
        //执行绘制工作
        if (mSurfaceTexture != null) {
            //将最新更新的图像转成GL中的纹理
            mSurfaceTexture.updateTexImage();
        }

        GLES20.glEnableVertexAttribArray(av_Position);//启用index指定的通用顶点属性数组
        GLES20.glEnableVertexAttribArray(af_Position);

        //设置顶点位置值
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, vertexBuffer);
        //设置纹理位置值
        GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, backTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//激活纹理单元
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,mTexture);//将一个纹理ID，绑定到一个纹理目标(target)上
        GLES20.glUniform1i(s_Texture, 0);
        //绘制GLES20.GL_TRIANGLE_STRIP：复用坐标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,VertexCount);//从数组数据中渲染图元

        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);

        if (mRenderCallback != null) {
            mRenderCallback.onDraw();
        }
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
        //使用的外部的camera的外部纹理，所以这里用的不是sampler2D 而是samplerExternalOES
        final String source = "#extension GL_OES_EGL_image_external : require \n" +
                "precision mediump float; " +
                "varying vec2 v_texPo; " +
                "uniform samplerExternalOES s_Texture; " +
                "void main() { " +
                "   gl_FragColor = texture2D(s_Texture, v_texPo); " +
                "} ";
        return source;
    }

    public SurfaceTexture getmSurfaceTexture() {
        return mSurfaceTexture;
    }

    public interface CameraGLSufaceRenderCallback {
        void onRequestRender();

        void onCreate(SurfaceTexture texture);

        void onChanged(int width, int height);

        void onDraw();
    }

}
