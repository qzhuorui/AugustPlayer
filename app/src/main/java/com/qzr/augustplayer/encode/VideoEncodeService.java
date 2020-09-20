package com.qzr.augustplayer.encode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.qzr.augustplayer.base.Base;
import com.qzr.augustplayer.utils.CameraUtil;
import com.qzr.augustplayer.utils.ThreadPoolProxyFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @ProjectName: SevenPlayer
 * @Package: com.qzr.sevenplayer.encode
 * @ClassName: VideoEncodeService
 * @Description:
 * @Author: qzhuorui
 * @CreateDate: 2020/7/11 12:34
 */
public class VideoEncodeService {

    private static final String TAG = "VideoEncodeService";

    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    public final static byte ENCODE_STATUS_NEED_NONE = 0;
    public final static byte ENCODE_STATUS_NEED_RECORD = 1;
    private static final int TIMEOUT_S = 10000;

    private static VideoEncodeService instance;

    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private MediaCodec.BufferInfo encodeBufferInfo;

    private Surface mInputSurface;

    private CopyOnWriteArraySet<OnEncodeDataAvailable> bufferAvailableCallback;

    private byte mVideoEncodeUseState = 0;

    private boolean mEncoding = false;
    private boolean mTransmit = false;

    private byte[] sps = null;
    private byte[] pps = null;

    private Queue<MuxerBean> encodeDataQueue;
    byte[] YUVDate;


    public synchronized static VideoEncodeService getInstance() {
        if (instance == null || instance.mMediaCodec == null) {
            instance = new VideoEncodeService();
        }
        return instance;
    }

    private VideoEncodeService() {
        try {
            int width = Base.SV.getVideoWidth();
            int height = Base.SV.getVideoHeight();

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3 * 8 * 30 / 256);
            mMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);//这个别忘记/写错了
            mMediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            mMediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);

            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            mInputSurface = mMediaCodec.createInputSurface();//codec创建surface，为输入数据创建一个目标Surface，根据这个生成eglSurface

            encodeBufferInfo = new MediaCodec.BufferInfo();

            YUVDate = new byte[width * height * 3 / 2];
            encodeDataQueue = new LinkedList<>();
            bufferAvailableCallback = new CopyOnWriteArraySet<>();

        } catch (Exception e) {
            Log.e(TAG, "VideoEncodeService: video encoder config error");
            e.printStackTrace();
        }
    }

    /**
     * @description 获取编码feed数据的输入源surface；在输入Surface渲染的画面就能直接当做输入源流进到编码器中
     * @date: 2020/9/12 12:14
     * @author: qzhuorui
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    public boolean startVideoEncode() {
        if (mEncoding) {
            Log.e(TAG, "startVideoEncode: is encoding!");
            return true;
        }
        try {
            mMediaCodec.start();
            mEncoding = true;
        } catch (Exception e) {
            Log.e(TAG, "startVideoEncode: mediaCodec.start() is error");
            e.printStackTrace();
            mMediaCodec.stop();
            releaseVideoEncode();
        }
        return mEncoding;
    }

    public void stopVideoEncoding() {
        mEncoding = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopVideoEncode() {
        mEncoding = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void releaseVideoEncode() {
        Log.i(TAG, "releaseVideoEncode: ");
        mMediaCodec.release();
        mMediaCodec = null;
        encodeDataQueue.clear();
        bufferAvailableCallback.clear();
        bufferAvailableCallback = null;
        encodeDataQueue = null;
        instance = null;
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(transmitVideoDataTask);
    }

    public void handleNV21data(byte[] nv21Data) {
        if (nv21Data != null) {
            CameraUtil.NV21toI420SemiPlanar(nv21Data, YUVDate, 1920, 1080);
            try {
                int inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_S);
                if (inputIndex >= 0) {
                    long pts = getPts();
                    ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                    inputBuffer.clear();
                    inputBuffer.put(YUVDate);
                    mMediaCodec.queueInputBuffer(inputIndex, 0, YUVDate.length, pts, 0);
                }

                int outputIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, TIMEOUT_S);

                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = mMediaCodec.getOutputFormat();
                    ByteBuffer csdTmp = format.getByteBuffer("csd-0");
                    sps = csdTmp.array();
                    csdTmp = format.getByteBuffer("csd-1");
                    pps = csdTmp.array();

                    if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0) {
                        for (OnEncodeDataAvailable onEncodeDataAvailable : bufferAvailableCallback) {
                            if (onEncodeDataAvailable != null) {
                                onEncodeDataAvailable.onCdsInfoUpdate(sps, pps, Mp4MuxerManager.SOURCE_VIDEO);
                            }
                        }
                    }
                }

                while (outputIndex >= 0) {
                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputIndex);

                    if (encodeBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        encodeBufferInfo.size = 0;
                    }

                    if (encodeBufferInfo.size > 0) {
                        byte[] outputData = new byte[encodeBufferInfo.size];
                        outputBuffer.get(outputData);
                        outputBuffer.position(encodeBufferInfo.offset);
                        outputBuffer.limit(encodeBufferInfo.offset + encodeBufferInfo.size);
                        encodeBufferInfo.presentationTimeUs = getPts();
                        enqueueFrame(new MuxerBean(outputData, encodeBufferInfo, true));
                    }

                    mMediaCodec.releaseOutputBuffer(outputIndex, false);
                    encodeBufferInfo = new MediaCodec.BufferInfo();
                    outputIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, TIMEOUT_S);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "handleYUVdata: YUVData is null");
        }
    }

    private synchronized void enqueueFrame(MuxerBean muxerBean) {
        encodeDataQueue.offer(muxerBean);
    }

    public void startTransmitVideoData() {
        Log.i(TAG, "startTransmitVideoData: ");
        if (!mTransmit) {
            mTransmit = true;
            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(transmitVideoDataTask);
        }
    }

    private Runnable transmitVideoDataTask = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (encodeDataQueue == null) {
                    mTransmit = false;
                } else {
                    Log.i(TAG, "run: transmitVideoDataTask dequeueFrame");
                    MuxerBean tmpEncodeData = dequeueFrame();
                    if (tmpEncodeData == null && !mEncoding) {
                        mTransmit = false;
                    }
                    if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0 && tmpEncodeData != null) {
                        Iterator<OnEncodeDataAvailable> iterator = bufferAvailableCallback.iterator();
                        while (iterator.hasNext()) {
                            OnEncodeDataAvailable onEncodeDataAvailable = iterator.next();
                            onEncodeDataAvailable.onEncodeBufferAvailable(tmpEncodeData, Mp4MuxerManager.SOURCE_VIDEO);
                        }
                    } else if (tmpEncodeData == null) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (!mTransmit) {
                    if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0) {
                        for (OnEncodeDataAvailable onEncodeDataAvailable : bufferAvailableCallback) {
                            if (onEncodeDataAvailable != null) {
                                onEncodeDataAvailable.onEncodeBufferAvailable(null, Mp4MuxerManager.SOURCE_VIDEO);
                            }
                        }
                    }
                    break;
                }
            }
        }
    };

    private synchronized MuxerBean dequeueFrame() {
        return encodeDataQueue.poll();
    }

    private long getPts() {
        return System.nanoTime() / 1000L;
    }

    public synchronized byte getmVideoEncodeUseState() {
        return mVideoEncodeUseState;
    }

    public synchronized void addmVideoEncodeUseState(byte value) {
        mVideoEncodeUseState = (byte) (mVideoEncodeUseState | value);
    }

    public synchronized void removeEncoderUseStatus(byte value) {
        mVideoEncodeUseState = (byte) (mVideoEncodeUseState & (~value));
    }

    public synchronized void addCallBack(OnEncodeDataAvailable onEncodeDataAvailable) {
        bufferAvailableCallback.add(onEncodeDataAvailable);
        if (sps != null) {
            //后进来的回调保证能得到 sps,pps
            onEncodeDataAvailable.onCdsInfoUpdate(sps, pps, Mp4MuxerManager.SOURCE_VIDEO);
        }
    }

    public synchronized void removeCallBack(OnEncodeDataAvailable onEncodeDataAvailable) {
        if (bufferAvailableCallback != null) {
            bufferAvailableCallback.remove(onEncodeDataAvailable);
        }
    }

    /**
     * @description 从编码器中提取所有未处理的数据
     * @date: 2020/9/12 12:21
     * @author: qzhuorui
     * @param: 编码是否结束
     * @return:
     */
    public void drainEncoderData(boolean endOfStream) {
        if (endOfStream) {
            //发送一个EOS结束标志位到输入源
            //然后等到我们在编码输出的数据发现EOS的时候，证明最后的一批编码数据已经编码成功了
            mMediaCodec.signalEndOfInputStream();
        }
        while (true) {
            int outputIndex = mMediaCodec.dequeueOutputBuffer(encodeBufferInfo, TIMEOUT_S);

            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat format = mMediaCodec.getOutputFormat();
                ByteBuffer csdTmp = format.getByteBuffer("csd-0");
                sps = csdTmp.array();
                csdTmp = format.getByteBuffer("csd-1");
                pps = csdTmp.array();

                if (bufferAvailableCallback != null && bufferAvailableCallback.size() > 0) {
                    for (OnEncodeDataAvailable onEncodeDataAvailable : bufferAvailableCallback) {
                        if (onEncodeDataAvailable != null) {
                            onEncodeDataAvailable.onCdsInfoUpdate(sps, pps, Mp4MuxerManager.SOURCE_VIDEO);
                        }
                    }
                }
            } else if (outputIndex < 0) {
                Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + outputIndex);
            } else {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputIndex);

                if (encodeBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                    //标记为这样的缓冲器包含编解码器初始化/编解码器特定数据而不是媒体数据
                    encodeBufferInfo.size = 0;
                }

                if (encodeBufferInfo.size > 0) {
                    byte[] outputData = new byte[encodeBufferInfo.size];
                    outputBuffer.get(outputData);
                    outputBuffer.position(encodeBufferInfo.offset);
                    outputBuffer.limit(encodeBufferInfo.offset + encodeBufferInfo.size);
                    encodeBufferInfo.presentationTimeUs = getPts();
                    enqueueFrame(new MuxerBean(outputData, encodeBufferInfo, true));
                }

                mMediaCodec.releaseOutputBuffer(outputIndex, false);

                if ((encodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }
}
