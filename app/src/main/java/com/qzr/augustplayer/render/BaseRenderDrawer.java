package com.qzr.augustplayer.render;

import android.opengl.GLES30;

import com.qzr.augustplayer.utils.GlesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: BaseRenderDrawer
 * @Description: 加载着色器通用类
 * @Author: qzhuorui
 * @CreateDate: 2020/9/5 11:36
 */
public abstract class BaseRenderDrawer {

    private static final String TAG = "BaseRenderDrawer";

    private float vertexData[] = {
            -1f, -1f,// 左下角
            1f, -1f, // 右下角
            -1f, 1f, // 左上角
            1f, 1f,  // 右上角
    };
    private float backTextureData[] = {
            0f, 1f, // 左上角
            0f, 0f, //  左下角
            1f, 1f, // 右上角
            1f, 0f  // 右下角
    };
    private float frontTextureData[] = {
            1f, 1f, // 右上角
            1f, 0f, // 右下角
            0f, 1f, // 左上角
            0f, 0f //  左下角
    };
    private float displayTextureData[] = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
    };
    private float frameBufferData[] = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
    };

    //顶点坐标 Buffer
    private FloatBuffer mVertexBuffer;
    protected int mVertexBufferId;
    //纹理坐标 Buffer
    private FloatBuffer mBackTextureBuffer;
    protected int mBackTextureBufferId;
    //纹理坐标 Buffer
    private FloatBuffer mFrontTextureBuffer;
    protected int mFrontTextureBufferId;
    //纹理坐标 Buffer
    private FloatBuffer mDisplayTextureBuffer;
    protected int mDisplayTextureBufferId;
    //纹理坐标 Buffer
    private FloatBuffer mFrameTextureBuffer;
    protected int mFrameTextureBufferId;

    protected final int CoordsPerVertexCount = 2;
    protected final int VertexCount = vertexData.length / CoordsPerVertexCount;
    protected final int VertexStride = CoordsPerVertexCount * 4;

    protected final int CoordsPerTextureCount = 2;
    protected final int TextureStride = CoordsPerTextureCount * 4;

    protected int mProgram;

    protected int width;
    protected int height;

    /**
     * @description 对应Render的surfaceCreated生命周期方法
     * @date: 2020/9/5 11:56
     * @author: qzhuorui
     */
    public void create() {
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());//创建并链接program
        initVertexBufferObjects();
        onCreated();
    }

    protected void initVertexBufferObjects() {
        int[] vbo = new int[5];
        GLES30.glGenBuffers(5, vbo, 0);//生成缓冲区对象

        /**
         * 顶点坐标Buffer
         */
        mVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)//为顶点数据分配本地内存，float
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);//存放顶点数据
        mVertexBuffer.position(0);
        mVertexBufferId = vbo[0];
        //缓冲区对象绑定的目标：ARRAY_BUFFER 将使用 Float*Array 而 ELEMENT_ARRAY_BUFFER 必须使用 Uint*Array
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);//绑定一个命名（ID）的缓冲区对象
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.length * 4, mVertexBuffer, GLES30.GL_STATIC_DRAW);//为当前绑定到target的缓冲区对象创建一个新的数据存储

        /**
         * 纹理坐标Buffer
         */
        mBackTextureBuffer = ByteBuffer.allocateDirect(backTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(backTextureData);
        mBackTextureBuffer.position(0);
        mBackTextureBufferId = vbo[1];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mBackTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, backTextureData.length * 4, mBackTextureBuffer, GLES30.GL_STATIC_DRAW);

        /**
         * 纹理坐标Buffer
         */
        mFrontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frontTextureData);
        mFrontTextureBuffer.position(0);
        mFrontTextureBufferId = vbo[2];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mFrontTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, frontTextureData.length * 4, mFrontTextureBuffer, GLES30.GL_STATIC_DRAW);

        /**
         * 纹理坐标Buffer
         */
        mDisplayTextureBuffer = ByteBuffer.allocateDirect(displayTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(displayTextureData);
        mDisplayTextureBuffer.position(0);
        mDisplayTextureBufferId = vbo[3];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, displayTextureData.length * 4, mDisplayTextureBuffer, GLES30.GL_STATIC_DRAW);

        mFrameTextureBuffer = ByteBuffer.allocateDirect(frameBufferData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frameBufferData);
        mFrameTextureBuffer.position(0);
        mFrameTextureBufferId = vbo[4];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mFrameTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, frameBufferData.length * 4, mFrameTextureBuffer, GLES30.GL_STATIC_DRAW);

        //取消绑定
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    /**
     * @description 对应Render的surfaceChanged生命周期方法
     * @date: 2020/9/5 11:57
     * @author: qzhuorui
     */
    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
        onChanged(width, height);
    }

    /**
     * @description 对应Render的drawFrame生命周期方法
     * @date: 2020/9/5 11:57
     * @author: qzhuorui
     */
    public void draw(long timestamp, float[] transformMatrix) {
        clear();
        useProgram();
        viewPort(0, 0, width, height);
        onDraw(transformMatrix);
    }

    protected void clear() {
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

    protected void useProgram() {
        GLES30.glUseProgram(mProgram);
    }

    protected void viewPort(int x, int y, int width, int height) {
        GLES30.glViewport(x, y, width, height);
    }

    /**
     * ************************************Abstract method**********************************************
     */

    protected abstract void onCreated();

    protected abstract void onChanged(int width, int height);

    protected abstract void onDraw(float[] transformMatrix);

    public abstract void setInputTextureId(int textureId);

    public abstract int getOutputTextureId();

    protected abstract String getVertexSource();

    protected abstract String getFragmentSource();
}
