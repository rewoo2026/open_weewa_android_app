package com.xh.hotme.widget;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.macoli.wav_player.Wav;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WAV播放器
 * 解析文件头，播放pcm
 * 每次创建wavplayer都会创建独立的audiotrack
 * 每次只能独立播放一个wav
 *
 * @see Downloader 音频文件下载线程
 * @see RealPlayer 音频文件播放线程
 */
public class WavPlayer {
    public volatile boolean isPlaying = false; //标识播放状态
    private final LinkedBlockingQueue<byte[]> mSoundData = new LinkedBlockingQueue<>();    //播放缓冲
    private volatile Wav mWav;     //当前正在播放的wav文件
    private volatile int mDownloadComplete = -1;   //-1 未下载，-2 下载错误 ， 1 下载成功
    private final byte[] mWavReady = new byte[1];  //下载线程和播放线程同步锁

    boolean _interrupt = false;

    public WavPlayer() {

    }

    public void play(String urlStr, boolean local) {
        isPlaying = true;
        _interrupt = false;
        //清理资源
        mSoundData.clear();
        mDownloadComplete = -1;
        mWav = null;
        new Thread(new Downloader(urlStr, local), "WavPlayer-Downloader").start();
        new Thread(new RealPlayer(), "WavPlayer-RealPlayer").start();
    }

    public void stop() {
        _interrupt = true;
    }

    /**
     * 获取WAV声道数
     */
    private int getChannel() {
        return mWav.getWavHeader().getMNumChannel() == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
    }

    /**
     * 获取WAV采样精度
     */
    private int getEncoding() {
        int ENCODING = AudioFormat.ENCODING_DEFAULT;
        if (mWav.getWavHeader().getMBitsPerSample() == 8) {
            ENCODING = AudioFormat.ENCODING_PCM_8BIT;
        } else if (mWav.getWavHeader().getMBitsPerSample() == 16) {
            ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        } else if (mWav.getWavHeader().getMBitsPerSample() == 32) {
            ENCODING = AudioFormat.ENCODING_PCM_FLOAT;
        }
        return ENCODING;
    }

    private int getMiniBufferSize() {
        return AudioTrack.getMinBufferSize(
                mWav.getWavHeader().getMSampleRate(), getChannel(), getEncoding());
    }

    private WavOnCompletionListener onCompletionListener;

    public void setOnCompletionListener(WavOnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    public interface WavOnCompletionListener {
        /**
         * 播放完成回调
         *
         * @param success true 播放成功，false 播放失败（下载失败或者播放失败）
         */
        void onCompletion(boolean success);
    }

    private class Downloader implements Runnable {

        private final String mUrlStr;
        private final boolean isLocal;

        private Downloader(String urlStr, boolean local) {
            mUrlStr = urlStr;
            isLocal = local;
        }

        @Override
        public void run() {
            mDownloadComplete = -1;
            InputStream in = null;
            try {
                if (!isLocal) {
                    URL url = new URL(mUrlStr);
                    URLConnection urlConnection = url.openConnection();
                    in = new BufferedInputStream(urlConnection.getInputStream());
                } else {
                    in = new BufferedInputStream(new FileInputStream(mUrlStr));
                }

                if (in == null) {
                    mDownloadComplete = -2;
                    isPlaying = false;
                    if (onCompletionListener != null) {
                        onCompletionListener.onCompletion(false);
                    }
                    synchronized (mWavReady) {
                        mWavReady.notifyAll();
                    }

                    return;
                }
                synchronized (mWavReady) {
                    mWav = new Wav(in);
                    mWavReady.notifyAll();//wav初始化完毕，通知播放线程开始播放
                }
            } catch (Exception e) {

                mDownloadComplete = -2;
                isPlaying = false;
                if (onCompletionListener != null) {
                    onCompletionListener.onCompletion(false);
                }
                synchronized (mWavReady) {
                    mWavReady.notifyAll();
                }
                return;

            }
            int iniBufferSize = getMiniBufferSize();
            byte[] buffer = new byte[iniBufferSize];
            int read = 0;
            long startTime = System.currentTimeMillis();

            try {
                int bufferFilledCount = 0;//buffer已填充大小
                //避免网络下载出现buffer填充不满导致的播放有杂音问题，强制buffer填充满再加入播放缓冲队列
                while ((read = in.read(buffer, bufferFilledCount, iniBufferSize - bufferFilledCount)) != -1) {
                    bufferFilledCount += read;
                    if (bufferFilledCount >= iniBufferSize) {
                        byte[] newBuffer = Arrays.copyOf(buffer, iniBufferSize);
                        mSoundData.put(newBuffer);
                        read = 0;
                        bufferFilledCount = 0;
                    }
                }

                mDownloadComplete = 1;
            } catch (IOException | InterruptedException e) {
                mDownloadComplete = -2;
                isPlaying = false;
                if (onCompletionListener != null) {
                    onCompletionListener.onCompletion(false);
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class RealPlayer implements Runnable {
        private AudioTrack mAudioTrack;



        /**
         * 初始化audiotrack
         */
        private void initAudioTracker() {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setEncoding(getEncoding())
                    .setSampleRate(mWav.getWavHeader().getMSampleRate())
                    .build();
            mAudioTrack = new AudioTrack(audioAttributes, audioFormat, getMiniBufferSize()
                    , AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

        }

        /**
         * 开始播放wav
         */
        public void play() {
            mAudioTrack.play();
            byte[] buffer;
            try {
                while (true) {
                    if(_interrupt){
                        break;
                    }
                    buffer = mSoundData.take();
                    if (mWav.getWavHeader().getMBitsPerSample() == 8) {
                        try {
                            mAudioTrack.write(buffer, 0, buffer.length, AudioTrack.WRITE_BLOCKING);
                        } catch (Exception e) {
                        }
                    } else if (mWav.getWavHeader().getMBitsPerSample() == 16) {
                        try {
                            ShortBuffer sb = ByteBuffer.wrap(buffer, 0, buffer.length).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                            short[] out = new short[sb.capacity()];
                            sb.get(out);
                            mAudioTrack.write(out, 0, out.length, AudioTrack.WRITE_BLOCKING);
                        } catch (Exception e) {

                        }
                    } else if (mWav.getWavHeader().getMBitsPerSample() == 32) {
                        try {
                            FloatBuffer fb = ByteBuffer.wrap(buffer, 0, buffer.length).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
                            float[] out = new float[fb.capacity()];
                            fb.get(out);
                            mAudioTrack.write(out, 0, out.length, AudioTrack.WRITE_BLOCKING);
//                        mAudioTrack.write(mBuffer, 0, read ,  AudioTrack.WRITE_BLOCKING);
                        } catch (Exception e) {

                        }
                    }
                    //播放完成
                    // case1：下载完成，并且播放缓冲区的数据已经全部被提取
                    // case2：下载失败
                    if ((1 == mDownloadComplete && mSoundData.isEmpty()) || -2 == mDownloadComplete) {
                        break;
                    }
                }
            } catch (Exception e) {
                isPlaying = false;
                if (onCompletionListener != null) {
                    onCompletionListener.onCompletion(false);
                }
                return;
            } finally {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
                isPlaying = false;
            }
            if (onCompletionListener != null) {
                onCompletionListener.onCompletion(true);
            }
        }

        @Override
        public void run() {
            //等待下载线程初始化wav
            synchronized (mWavReady) {
                if (mWav == null) {
                    try {
                        mWavReady.wait();
                        if (mWav == null) {
                            Log.d("gggl", "realplayer wait finish wav is null!");
                            return;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            initAudioTracker();
            play();
        }
    }
}