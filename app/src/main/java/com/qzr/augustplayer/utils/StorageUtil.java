package com.qzr.augustplayer.utils;

import android.os.Environment;
import android.widget.Toast;

import com.qzr.augustplayer.base.Base;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.utils
 * @ClassName: StorageUtil
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 10:29
 */
public class StorageUtil {

    private static final String TAG = "StorageUtil";

    private static String getDirName() {
        return "AugustPlayer";
    }

    public static final ThreadLocal<SimpleDateFormat> spFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault());
        }
    };

    private static String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    public static boolean checkDirExist(String path) {
        File mDir = new File(path);
        if (!mDir.exists()) {
            return mDir.mkdirs();
        }
        return true;
    }

    public static File getOutPutImageFile() {
        String timestamp = spFormat.get().format(new Date());
        File picFile = new File(getSDPath() + "/" + getDirName() + "/image/");
        if (!picFile.exists()) {
            if (!picFile.mkdirs()) {
                Toast.makeText(Base.CURRENT_APP, "pic mkdirs failure", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return  new File(picFile + File.separator + timestamp + ".jpg");
    }

    public static File getOutPutVideoFile() {
        String timestamp = spFormat.get().format(new Date());
        File videoFile = new File(getSDPath() + "/" + getDirName() + "/video/");
        if (!videoFile.exists()) {
            if (!videoFile.mkdirs()) {
                Toast.makeText(Base.CURRENT_APP, "video mkdirs failure", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return new File(videoFile + File.separator + timestamp + ".mp4");
    }

    public static File getOutPutLogFile() {
        return new File(getSDPath() + "/" + getDirName() + "/log/");
    }

}
