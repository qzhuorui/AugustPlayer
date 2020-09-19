package com.qzr.augustplayer.utils;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.utils
 * @ClassName: EGLHelper
 * @Description: EGL方法相关，工具类
 * @Author: qzhuorui
 * @CreateDate: 2020/9/6 11:08
 */
public class EGLHelper {

    /**
     * EGLDisplay：是对 实际显示设备 的抽象
     * EGLSurface：是对 用来存储图像的内存区域 的抽象
     * EGLContext：存储OpenGL.ES绘图的一些状态信息。管理上述两者关联的状态。
     */

    /**
     * 创建EGL过程，开始正常绘图的流程步骤：
     * 1. 获取EGLDisplay对象
     * 2. 初始化与EGLDisplay之间的关联
     * 3. 获取EGLConfig对象
     * 4. 获取EGLContext实例
     * 5. 创建EGLSurface实例
     * 6. 连接EGLContext和EGLSurface
     * ------以上 封装在GLSurfaceView，对使用者透明------
     * 7. 使用GL指令绘制图形———>render三大回调，渲染死循环
     * ------以下 封装在GLSurfaceView，对使用者透明------
     * 8. 断开并释放与EGLSurface关联的EGLContext对象
     * 9. 删除EGLSurface对象
     * 10.删除EGLContext对象
     * 11.终止与EGLDisplay之间的连接
     */

    private static final String TAG = "EGLHelper";

    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLConfig mEglConfig;

    /**
     * @description 根据指定线程的EGLContext，来创建openglContext
     * @date: 2020/9/6 12:01
     * @author: qzhuorui
     */
    public void createGL(EGLContext mEglContext) {
        //设置显示设备
        setDisplay();
        //设置属性
        setConfig();
        //创建上下文
        createContext(mEglContext);
    }

    /**
     * @description 获取显示Display的句柄
     * @date: 2020/9/12 9:46
     * @author: qzhuorui
     */
    private void setDisplay() {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);//从系统中得到用于显示图片绘制的Display的handle，作为OpenGL.ES的渲染目标
        //检测是否创建成功
        int[] version = new int[2];
        //初始化与EGLDisplay之间的关联
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("EGL error" + EGL14.eglGetError());
        }
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_VENDOR));
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_VERSION));
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_EXTENSIONS));
    }

    /**
     * @description 指定一组配置，使用eglChooseConfig得到Config
     * @date: 2020/9/12 9:50
     * @author: qzhuorui
     */
    private void setConfig() {
        //EGL配置，键值对
        int[] configAttribs = {
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,      //渲染类型
                EGL10.EGL_RED_SIZE, 8,  //指定 RGB 中的 R 大小（bits）
                EGL10.EGL_GREEN_SIZE, 8, //指定 G 大小
                EGL10.EGL_BLUE_SIZE, 8,  //指定 B 大小
                EGL10.EGL_ALPHA_SIZE, 8, //指定 Alpha 大小
                EGL10.EGL_DEPTH_SIZE, 8, //指定深度(Z Buffer) 大小
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, //指定渲染 api 类别,
                EGL10.EGL_NONE
        };
        setConfig(configAttribs);
    }

    /**
     * @description 得到EglConfig对象
     * @date: 2020/9/12 9:50
     * @author: qzhuorui
     */
    private void setConfig(int[] configAttribs) {
        int[] numConfigs = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, configAttribs, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }
        mEglConfig = configs[0];
    }

    /**
     * @description 根据EGL Context,display,config创建openGL Context
     * @date: 2020/9/6 11:16
     * @author: qzhuorui
     * @param: eglGetCurrentContext生成的EGL context
     * @return:
     */
    private void createContext(EGLContext context) {
        int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
        };
        //指定显示的连接；前面选好的Config；允许其他EGLContext共享数据，使用EGL_NO_CONTEXT表示不共享；与传入的context共享OpenGL资源
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, context, contextAttribs, 0);
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }
    }

    /**
     * @description EGLContext和Codec.Surface关联，创建我们需要的WindowSurface
     * @date: 2020/9/6 11:20
     * @author: qzhuorui
     */
    public EGLSurface createWindowSurface(Object surface) {
        return createWindowSurface(mEglConfig, surface);
    }

    /**
     * @description 创建要使用的渲染表面，绑定传入的surface(native的surface)
     * @date: 2020/9/12 9:54
     * @author: qzhuorui
     * @param: 符合条件的EglConfig；指定的原生窗口
     * @return:
     */
    private EGLSurface createWindowSurface(EGLConfig config, Object surface) {
        //将EGl和设备屏幕连接起来，这样OpenGL处理的图像就能显示在屏幕上了
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, config, surface, new int[]{EGL14.EGL_NONE}, 0);
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.d(TAG, "createWindowSurface" + EGL14.eglGetError());
            return null;
        }
        return eglSurface;
    }

    /**
     * @description 切换上下文，这样opengl方法才能起作用
     * @date: 2020/9/6 11:23
     * @author: qzhuorui
     */
    public boolean makeCurrent(EGLSurface surface) {
        return makeCurrent(surface, mEglContext);
    }

    private boolean makeCurrent(EGLSurface surface, EGLContext context) {
        return makeCurrent(surface, surface, context);
    }

    /**
     * @description 关联上下文，EGLSurface和EGLContext
     * @date: 2020/9/12 10:01
     * @author: qzhuorui
     * @param: EGL绘图表面；EGL读取表面(通常是一个）；指定连接到该表面的上下文
     * @return:
     */
    private boolean makeCurrent(EGLSurface draw, EGLSurface read, EGLContext context) {
        //指定某个EGLContext为当前上下文，关联特定的EGLContext和EGLSurface
        if (!EGL14.eglMakeCurrent(mEglDisplay, draw, read, context)) {
            Log.d(TAG, "makeCurrent" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    /**
     * @description 提交绘制结果；OpenGL双缓冲机制来渲染表面，所以需要每帧的交替读写的surface
     * @date: 2020/9/12 9:35
     * @author: qzhuorui
     */
    public boolean swapBuffers(EGLSurface surface) {
        //交换读写的渲染介质，把画面渲染出来
        if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
            Log.d(TAG, "swapBuffers" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    public boolean setPresentationTime(EGLSurface surface, long timeStamp) {
        if (!EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, timeStamp)) {
            Log.d(TAG, "setPresentationTime" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    public void destroySurface(EGLSurface surface) {
        EGL14.eglDestroySurface(mEglDisplay, surface);
    }

    public void destroyGL() {
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
        mEglContext = EGL14.EGL_NO_CONTEXT;
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
    }


}
