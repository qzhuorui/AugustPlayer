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

    //顶点位置缓存
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

    //一个Float占用4Byte
    private final int BYTES_PER_FLOAT = 4;

    //每次取点的数量，必须为1,2,3,4；三个顶点，一个顶点需要3个来描述其位置，需要3个偏移量
    private final int CoordsPerVertexCount = 3;
    //一次取出的大小；一个点需要的byte偏移量
    private final int VertexStride = CoordsPerVertexCount * BYTES_PER_FLOAT;
    //顶点坐标数量
    private final int VertexCount = vertexData.length / CoordsPerVertexCount;

    //每次取点的数量，必须为1,2,3,4
    private final int CoordsPerTextureCount = 2;
    //一次取出的大小
    private final int TextureStride = CoordsPerTextureCount * BYTES_PER_FLOAT;

    private int av_Position;
    private int af_Position;
    private int s_Texture;

    public CameraGLSurfaceRender(CameraGLSufaceRenderCallback mRenderCallback) {
        this.mRenderCallback = mRenderCallback;

        //为顶点位置申请本地内存，每个浮点占4字节空间，坐标数据转化为FloatBuffer，用以传入openGL es program
        this.vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * BYTES_PER_FLOAT)//分配本地内存空间
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        this.vertexBuffer.position(0);

        this.backTextureBuffer = ByteBuffer.allocateDirect(backTextureData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(backTextureData);
        this.backTextureBuffer.position(0);

        this.frontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frontTextureData);
        this.frontTextureBuffer.position(0);
    }

    @Override
    protected void onCreate() {
        //创建OES外部纹理
        mTexture = loadExternelTexture();

        //获取着色器program内成员变量的id（句柄，指针）
        av_Position = GLES20.glGetAttribLocation(mProgramId, "av_Position");//获取顶点着色器的av_Position成员句柄
        af_Position = GLES20.glGetAttribLocation(mProgramId, "af_Position");//
        s_Texture = GLES20.glGetUniformLocation(mProgramId, "s_Texture");//获取着色器程序中，指定为uniform类型变量的id

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
            mRenderCallback.onChanged(width, height);
        }
    }

    @Override
    protected void onDraw() {
        //执行绘制工作
        if (mSurfaceTexture != null) {
            //SurfaceTexture从图像流（来自Camera预览，视频解码，GL绘制场景等）中获得帧数据，
            //当调用updateTexImage()时，根据内容流中最近的图像更新SurfaceTexture对应的GL纹理对象
            mSurfaceTexture.updateTexImage();//将最新更新的图像转成GL中的纹理
        }

        GLES20.glEnableVertexAttribArray(av_Position);//启用index指定的通用顶点属性数组，启用定点位置句柄
        GLES20.glEnableVertexAttribArray(af_Position);//启用顶点颜色句柄
        //av_Position；通过获取指向着色器相应数据成员的各个id，就能将自定义的顶点数据，颜色数据等传递到着色器中
        //1.指定要修改的顶点着色器中顶点变量id；2.指定每个顶点属性的组件数量，position是由3个(x,y,z)组成，颜色是4个(r,g,b,a)
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, vertexBuffer);//将顶点位置数据传入着色器，将坐标数据放入
        GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, backTextureBuffer);//顶点坐标传递到顶点着色器

        //激活指定纹理单元，一般默认是第一个
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//严谨点应该是选择当前活跃的纹理单元，并不是激活和启用
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);//绑定纹理ID到纹理单元
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(s_Texture, 0);//设置纹理的坐标；将纹理设置给Shader；将纹理传入Shader（告诉Shader，采样器是哪个）

        //图形绘制；顶点法：绘制三角形；复杂图形建议使用索引法；顶点的数量
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);//从数组数据中渲染图元

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
                //根据纹理坐标，从纹理单元中取色
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
