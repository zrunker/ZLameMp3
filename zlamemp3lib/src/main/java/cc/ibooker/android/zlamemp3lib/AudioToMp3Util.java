package cc.ibooker.android.zlamemp3lib;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 音频转MP3管理类
 *
 * @author 邹峰立
 */
public class AudioToMp3Util {
    private ExecutorService executors;
    private Handler handler;

    private static AudioToMp3Util audioToMp3Util;

    public static AudioToMp3Util getInstance() {
        if (audioToMp3Util == null) {
            synchronized (AudioToMp3Util.class) {
                audioToMp3Util = new AudioToMp3Util();
            }
        }
        return audioToMp3Util;
    }

    /**
     * Flush LAME buffer.
     * <p>
     * REQUIRED:
     * lame_encode_flush will flush the intenal PCM buffers, padding with
     * 0's to make sure the final frame is complete, and then flush
     * the internal MP3 buffers, and thus may return a
     * final few mp3 frames.  'mp3buf' should be at least 7200 bytes long
     * to hold all possible emitted data.
     * <p>
     * will also write id3v1 tags (if any) into the bitstream
     * <p>
     * return code = number of bytes output to mp3buf. Can be 0
     *
     * @param mp3buf result encoded MP3 stream. You must specified at least 7200
     *               bytes.
     * @return number of bytes output to mp3buf. Can be 0.
     */
    public int flush(byte[] mp3buf) {
        return Mp3Converter.flush(mp3buf);
    }

    /**
     * Encode buffer to mp3.
     *
     * @param bufferLeft  PCM data for left channel.
     * @param bufferRight PCM data for right channel.
     * @param samples     number of samples per channel.
     * @param mp3buf      result encoded MP3 stream. You must specified
     *                    "7200 + (1.25 * buffer_l.length)" length array.
     * @return <p>number of bytes output in mp3buf. Can be 0.</p>
     * <p>-1: mp3buf was too small</p>
     * <p>-2: malloc() problem</p>
     * <p>-3: lame_init_params() not called</p>
     * -4: psycho acoustic problems
     */
    public int encode(short[] bufferLeft, short[] bufferRight,
                      int samples, byte[] mp3buf) {
        return Mp3Converter.encode(bufferLeft, bufferRight, samples, mp3buf);
    }

    /**
     * 初始化Lame
     * <p>
     * inSampleRate 要转换的音频文件采样率
     * mode 音频编码模式，包括VBR、ABR、CBR
     * outSampleRate 转换后音频文件采样率
     * outBitRate 输出的码率
     * quality 压缩质量
     */
    public void initLame(int inSampleRate, int channel, int mode,
                         int outSampleRate, int outBitRate, int quality) {
        Mp3Converter.init(inSampleRate, channel, mode, outSampleRate, outBitRate, quality);
    }

    /**
     * Wav文件转MP3
     */
    public void initLameWav() {
        Mp3Converter.init(44100, 2, 0, 44100, 96, 7);
    }

    /**
     * 开始转码，默认Wav
     *
     * @param sourcePath        源文件地址
     * @param mp3Path           目标MP3文件地址
     * @param converterListener 监听
     */
    public void startConvert(@NonNull String sourcePath,
                             @NonNull final String mp3Path,
                             final ConverterListener converterListener) {
        try {
            if (!TextUtils.isEmpty(sourcePath)
                    && !sourcePath.endsWith(".mp3")
                    && !sourcePath.endsWith(".MP3")) {// 非mp3格式，转mp3
                final File sourceFile = new File(sourcePath);
                if (sourceFile.exists() && sourceFile.isFile()) {
                    final long fileSize = sourceFile.length();
                    if (fileSize > 0) {
                        MediaExtractor mediaExtractor = null;
                        int sampleRate = 44100;
                        try {
                            // 获取原文件码率，音轨数
                            mediaExtractor = new MediaExtractor();
                            mediaExtractor.setDataSource(sourcePath);
                            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(0);
//                            int trackCount = mediaExtractor.getTrackCount();
//                            int bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                            sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                            initLame(sampleRate, 2, 0, 44100, 96, 7);
                            Log.d("AudioToMp3Util-1", sampleRate + "");
                        } catch (Exception e) {
                            e.printStackTrace();
                            initLameWav();
                        } finally {
                            if (mediaExtractor != null)
                                mediaExtractor.release();
                        }
                        // 开启子线程转码
                        if (executors == null || executors.isShutdown()) {
                            executors = Executors.newCachedThreadPool();
                        }
                        final int finalSampleRate = sampleRate;
                        executors.execute(new Runnable() {
                            @Override
                            public void run() {
                                Mp3Converter.convertMp3(sourceFile.getAbsolutePath(), mp3Path, finalSampleRate, 2);
                            }
                        });
                        // 监听事件
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                long bytes = Mp3Converter.getConvertBytes();
                                float progress = (100f * bytes / fileSize);
                                if (bytes == -1) {
                                    progress = 100;
                                    destroy();
                                }
                                if (converterListener != null) {
                                    converterListener.onProgress(progress);
                                    if (progress >= 100) { // 转码完成
                                        converterListener.onComplete(mp3Path);
                                    } else {
                                        startTime(this);
                                    }
                                }
                            }
                        };
                        // 定时事件
                        startTime(runnable);
                    } else {
                        if (converterListener != null) {
                            converterListener.onError("当前文件大小为0！");
                        }
                        destroy();
                    }
                } else {
                    if (converterListener != null) {
                        converterListener.onError("文件不存在！");
                    }
                    destroy();
                }
            } else {
                if (converterListener != null) {
                    converterListener.onError("文件不存在，或者已是MP3文件！");
                }
                destroy();
            }
        } catch (Exception e) {
            if (converterListener != null) {
                converterListener.onError(e.getMessage());
            }
            destroy();
        }
    }

    // 开始定时
    private void startTime(Runnable runnable) {
        if (handler == null) {
            handler = new Handler();
        }
        handler.postDelayed(runnable, 500);
    }

    /**
     * 销毁
     */
    public void destroy() {
        if (executors != null) {
            executors.shutdownNow();
            executors = null;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    // 对外接口监听
    public interface ConverterListener {
        void onProgress(float progress);

        void onComplete(String mp3Path);

        void onError(String msg);
    }

}
