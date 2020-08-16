package com.qzr.augustplayer.encode;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.encode
 * @ClassName: OnEncodeDataAvailable
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/12 10:52
 */
public interface OnEncodeDataAvailable {

    public void onCdsInfoUpdate(byte[] csd0, byte[] csd1, int source);

    public void onEncodeBufferAvailable(MuxerBean muxerBean, int source);
}
