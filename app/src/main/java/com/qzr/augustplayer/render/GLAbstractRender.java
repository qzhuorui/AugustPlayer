package com.qzr.augustplayer.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.render
 * @ClassName: GLAbstractRender
 * @Description: 用来封装一些GL ES常用的方法，例如加载连链接着色器
 * @Author: qzhuorui
 * @CreateDate: 2020/8/23 10:04
 */
public abstract class GLAbstractRender implements GLSurfaceView.Renderer {
    private static final String TAG = "GLAbstractRender";

    protected int mVertexShader;
    protected int mFragmentShader;
    protected int mProgramId;

    protected int width;
    protected int height;

    /*
        1.note: EGL context lost 的情况：onSurfaceCreated need to recreate
            Since this method is called at the beginning of rendering, as well as
         every time the EGL context is lost, this method is a convenient place to put
         code to create resources that need to be created when the rendering
         starts, and that need to be recreated when the EGL context is lost.
        2.docs上对Render的描述如下：
            The renderer is responsible for making OpenGL calls to render a frame.
     */

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //创建program，链接顶点，片元着色器
        createProgram();
        onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //Called after the surface is created and whenever the OpenGL ES surface size changes
        GLES20.glViewport(0, 0, width, height);//设置绘制窗口/视图窗口
        this.height = height;
        this.width = width;
        onChanged();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //surface的绘制工作（最主要，最需要关注的部分）
        GLES20.glUseProgram(mProgramId);//将program加入到openGL环境；使program对象作为当前渲染的一部分。param指program对象的句柄，该程序对象的可执行文件将用作当前渲染状态的一部分
        onDraw();
    }

    /**
     * @description 创建program，链接着色器
     * @date: 2020/8/23 10:29
     * @author: qzhuorui
     */
    private void createProgram() {
        mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexSource());
        mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentSource());

        mProgramId = GLES20.glCreateProgram();//创建一个空program，并返回一个可以被引用的非零值。program是可以附加着色器对象的对象
        GLES20.glAttachShader(mProgramId, mVertexShader);//将着色器对象附加到program对象
        GLES20.glAttachShader(mProgramId, mFragmentShader);
        GLES20.glLinkProgram(mProgramId);//链接program成功之后，可以在program对象中创建一个或多个可执行文件，当调用glUseProgram时，这些文件成为当前状态的一部分

        int status[] = new int[1];
        GLES20.glGetProgramiv(mProgramId, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "createProgam: link error");
            Log.e(TAG, "createProgam: " + GLES20.glGetProgramInfoLog(mProgramId));
            GLES20.glDeleteProgram(mProgramId);
        }
    }

    /**
     * @description 加载对应着色器
     * @date: 2020/8/23 10:33
     * @author: qzhuorui
     */
    private int loadShader(int shaderType, String shaderSource) {
        int shaderId = GLES20.glCreateShader(shaderType);//创建一个着色器对象，并返回一个可以引用的非零值
        GLES20.glShaderSource(shaderId, shaderSource);//替换着色器对象中的源代码
        GLES20.glCompileShader(shaderId);//编译一个着色器对象

        int status[] = new int[1];
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, status, 0);//查询状态值，判断着色器编译是否成功
        if (status[0] == GLES20.GL_FALSE) {
            Log.e(TAG, "loadShader: compiler error");
            Log.e(TAG, "loadShader: " + GLES20.glGetShaderInfoLog(shaderId));
            GLES20.glDeleteShader(shaderId);
            return 0;
        }
        return shaderId;
    }

    /**
     * @description 创建外部纹理，后续可以通过ID创建SurfaceTexture传给camera，提供传递预览数据进来
     * @date: 2020/8/23 12:13
     * @author: qzhuorui
     */
    protected int loadExternelTexture() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);//生成纹理ID，且这个ID不会被后续调用返回，除非先deleteTextures。此时纹理ID还是没有维度的，当bind时才会指定
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);//将一个纹理ID，绑定到一个纹理目标(target)上，这个target之前的绑定自动解除。可以重复绑定。
        //为纹理对象设置参数（过滤方式）
        //处理相机数据使用OES，因为camera输出数据类型是YUV420P，使用OES可以自动将YUV转成RGB，我们就不需要在存储成MP4时再转化了。
        //处理贴纸图片使用2D
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);//设置缩小过滤方式为GL_LINEAR(双线性，目前最主要过滤方式)，GL_NEAREST(容易出现锯齿效果)和MIP贴图(占用更多内存)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);//设置放大过滤
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);//设置纹理S方向范围，控制纹理贴纸的范围在(0,1)之内
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);//设置纹理T方向范围
        return texture[0];
    }

    protected abstract String getVertexSource();

    protected abstract String getFragmentSource();

    /**
     * 生命周期对应的方法
     */

    protected abstract void onCreate();

    protected abstract void onChanged();

    protected abstract void onDraw();

}
