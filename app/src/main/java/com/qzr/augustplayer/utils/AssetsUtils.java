package com.qzr.augustplayer.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.utils
 * @ClassName: AssetsUtils
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/9/19 13:32
 */
public class AssetsUtils {

    public static String getStringFromAssert(Context mContext, String assetsFilePath) {

        String content = ""; // 结果字符串
        try {
            InputStream is = mContext.getResources().getAssets().open(assetsFilePath);// 打开文件
            int ch = 0;
            ByteArrayOutputStream out = new ByteArrayOutputStream(); // 实现了一个输出流
            while ((ch = is.read()) != -1) {
                out.write(ch); // 将指定的字节写入此 byte 数组输出流
            }
            byte[] buff = out.toByteArray();// 以 byte 数组的形式返回此输出流的当前内容
            out.close(); // 关闭流
            is.close(); // 关闭流
            content = new String(buff, "UTF-8"); // 设置字符串编码
        } catch (Exception e) {
            Toast.makeText(mContext, "对不起，没有找到指定文件！", Toast.LENGTH_SHORT)
                    .show();
        }
        return content;
    }

}
