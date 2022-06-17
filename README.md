# ZLameMp3
PCM（WAV）- Lame-MP3转换器。

使用Lame将PCM/WAV文件转换成MP3文件。

## 引入ZLameMp3插件

1. 在根gradle文件中引入jitpack仓库：
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
2. 在工程gradle文件中引入ZLameMp3插件
```
dependencies {
    implementation 'com.github.zrunker:ZLameMp3:v1.0.4'
}
```

## 使用
ZLameMp3提供使用两种方式进行文件格式转换：

### 一、Mp3Converter
Mp3Converter通过直接操作so库方法，来对PCM/WAV文件进行操作处理，该类适合开发者自定义实现，常用API如下：
```
public class Mp3Converter {

    /**
     * init lame
     *
     * @param inSampleRate  input sample rate in Hz
     * @param channel       number of channels
     * @param mode          0 = CBR, 1 = VBR, 2 = ABR.  default = 0
     * @param outSampleRate output sample rate in Hz
     * @param outBitRate    rate compression ratio in KHz
     * @param quality       quality=0..9. 0=best (very slow). 9=worst.<br />
     *                      recommended:<br />
     *                      2 near-best quality, not too slow<br />
     *                      5 good quality, fast<br />
     *                      7 ok quality, really fast
     */
    public native static void init(int inSampleRate, int channel, int mode,
                                   int outSampleRate, int outBitRate, int quality);

    /**
     * file convert to mp3
     * it may cost a lot of time and better put it in a thread
     *
     * @param inputPath    file path to be converted
     * @param mp3Path      mp3 output file path
     * @param inSampleRate input sample rate in Hz
     */
    public native static void convertMp3(String inputPath, String mp3Path, int inSampleRate, int channels);

    /**
     * file convert to mp3
     * it may cost a lot of time and better put it in a thread
     *
     * @param inputPath file path to be converted
     * @param mp3Path   mp3 output file path
     */
    public native static void wavConvertMp3(String inputPath, String mp3Path);

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
    public native static int encode(short[] bufferLeft, short[] bufferRight,
                                    int samples, byte[] mp3buf);

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
    public native static int flush(byte[] mp3buf);

    /**
     * Close LAME.
     */
    public native static void close();

    /**
     * get converted bytes in inputBuffer
     *
     * @return converted bytes in inputBuffer
     * to ignore the deviation of the file size,when return to -1 represents convert complete
     */
    public native static long getConvertBytes();

    /**
     * get library lame version
     *
     * @return lame version
     */
    public native static String getLameVersion();

}
```
Mp3Converter调用方式也很简单，直接调用静态API即可，例如：
```
// 获取当前lame版本
Mp3Converter.getLameVersion()
```

### 二、AudioToMp3Util
AudioToMp3Util是Mp3Converter的包装类，提供转码进度、错误和完成监听，用法如下：
```
AudioToMp3Util.getInstance().startConvert(
                "xxx.pcm/xxx.mav", "xx.mp3", new AudioToMp3Util.ConverterListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onProgress(float progress) {
                        // 转码进度："转码中" + progress + "%"
                    }

                    @Override
                    public void onComplete(String mp3Path) {
                        // 转码完成
                    }

                    @Override
                    public void onError(String msg) {
                        // 转码出错
                    }
                });
```
为防止内存泄露，建议在Activity/Fragment的销毁方法中执行AudioToMp3Util的销毁事件：
```
@Override
protected void onDestroy() {
    super.onDestroy();
    AudioToMp3Util.getInstance().destroy();
}
```
