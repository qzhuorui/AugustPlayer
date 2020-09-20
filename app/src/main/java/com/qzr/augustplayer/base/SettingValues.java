package com.qzr.augustplayer.base;

/**
 * @ProjectName: AugustPlayer
 * @Package: com.qzr.augustplayer.base
 * @ClassName: SettingValues
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/9/20 14:24
 */
public class SettingValues {

    public SettingValues() {
    }

    private int videoWidth = 1080;
    private int videoHeight = 1920;

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }
}
