package com.qzr.augustplayer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.utils
 * @ClassName: GlesUtil
 * @Description: OpenGL ES基础操作方法Utils
 * @Author: qzhuorui
 * @CreateDate: 2020/9/5 10:59
 */
public class GlesUtil {

    private static final String TAG = "GlesUtil";

    /**
     * @description create && link program
     * @date: 2020/9/5 10:45
     * @author: qzhuorui
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int mVertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int mFragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, mVertexShader);
        GLES30.glAttachShader(program, mFragmentShader);
        GLES30.glLinkProgram(program);

        int[] status = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "createProgam: link error");
            Log.e(TAG, "createProgam: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            return 0;
        }
        GLES30.glDeleteShader(mVertexShader);
        GLES30.glDeleteShader(mFragmentShader);
        return program;
    }

    /**
     * @description 加载着色器
     * @date: 2020/9/5 10:46
     * @author: qzhuorui
     */
    public static int loadShader(int shaderType, String shaderSource) {
        int shader = GLES30.glCreateShader(shaderType);
        GLES30.glShaderSource(shader, shaderSource);
        GLES30.glCompileShader(shader);
        int status[] = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "loadShader: compiler error");
            Log.e(TAG, "loadShader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * @description 打印错误信息
     * @date: 2020/9/5 10:47
     * @author: qzhuorui
     */
    public static void checkError() {
        if (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "createOutputTexture: " + GLES30.glGetError());
        }
    }

    /**
     * @description 生成FBO，帧缓冲区
     * @date: 2020/8/29 15:41
     * @author: qzhuorui
     */
    public static int createFrameBuffer() {
        int[] buffers = new int[1];
        GLES30.glGenFramebuffers(1, buffers, 0);//生成FBO，帧缓冲区对象
        checkError();
        return buffers[0];
    }

    /**
     * @description 创建OES纹理，接收camera原始数据
     * @date: 2020/8/29 15:12
     * @author: qzhuorui
     */
    public static int createCameraTexture() {
        int[] texture = new int[1];
        GLES30.glGenTextures(1, texture, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);//纹理ID绑定到OES target
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return texture[0];
    }

    /**
     * @description 创建2D的纹理
     * @date: 2020/8/29 15:14
     * @author: qzhuorui
     */
    public static int createFrameTexture(int width, int height) {
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "createOutputTexture: width or height is 0");
            return -1;
        }
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        if (textures[0] == 0) {
            Log.e(TAG, "createFrameTexture: glGenTextures is 0");
            return -1;
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);//纹理ID绑定到2D target
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);//指定一个二维的纹理图片
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GlesUtil.checkError();
        return textures[0];
    }

    /**
     * @description 创建图片纹理
     * @date: 2020/8/29 16:01
     * @author: qzhuorui
     */
    public static int loadBitmapTexture(Bitmap bitmap) {
        int[] textureIds = new int[1];
        GLES30.glGenTextures(1, textureIds, 0);
        if (textureIds[0] == 0) {
            Log.e(TAG, "loadBitmapTexture: glGenTextures is 0");
            return -1;
        }
        //绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0]);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //根据以上指定的参数，生成一个2D纹理
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        return textureIds[0];
    }

     /**
      * @description 创建图片纹理
      * @date: 2020/9/5 11:01
      * @author: qzhuorui
      */
    public static int loadBitmapTexture(Context context, int resourceId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        if (bitmap == null) {
            Log.e(TAG, "loadBitmapTexture:bitmap is null");
            return -1;
        }
        int textureId = loadBitmapTexture(bitmap);
        bitmap.recycle();
        return textureId;
    }

    /**
     * @description 纹理绑定到FBO
     * @date: 2020/9/5 10:54
     * @author: qzhuorui
     */
    public static void bindFrameTexture(int frameBufferId, int textureId) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId);//绑定一个命名的帧缓冲区对象
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureId, 0);//纹理图像添加到FBO
        //解绑
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GlesUtil.checkError();
    }

}
