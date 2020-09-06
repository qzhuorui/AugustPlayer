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

    private void setDisplay() {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);//先创建一个默认的Display
        //检测是否创建成功
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("EGL error" + EGL14.eglGetError());
        }
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_VENDOR));
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_VERSION));
        Log.d(TAG, EGL14.eglQueryString(mEglDisplay, EGL14.EGL_EXTENSIONS));
    }

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
      * @return
      */
    private void createContext(EGLContext context) {
        int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
        };
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

    private EGLSurface createWindowSurface(EGLConfig config, Object surface) {
        //创建我们想要的EGLSurface，之前的信息保存在config中
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

    private boolean makeCurrent(EGLSurface draw, EGLSurface read, EGLContext context) {
        if (!EGL14.eglMakeCurrent(mEglDisplay, draw, read, context)) {
            Log.d(TAG, "makeCurrent" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    public boolean swapBuffers(EGLSurface surface) {
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
