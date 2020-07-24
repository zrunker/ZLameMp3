#include <string>
#include <cstdio>
#include "jni.h"
#include "libmp3lame/lame.h"
#include "android/log.h"

// 7200 + 1024 * 1.25
#define BUFFER_SIZE 8480

static lame_global_flags *lame = nullptr;
long nowConvertBytes = 0;

void resetLame() {
    if (lame != nullptr) {
        lame_close(lame);
        lame = nullptr;
    }
}

unsigned char *convertJByteArrayToChars(JNIEnv *env, jbyteArray bytearray) {
    unsigned char *chars = nullptr;
    jbyte *bytes;
    bytes = env->GetByteArrayElements(bytearray, nullptr);
    int chars_len = env->GetArrayLength(bytearray);
    chars = new unsigned char[chars_len + 1];
    memset(chars, 0, chars_len + 1);
    memcpy(chars, bytes, chars_len);
    chars[chars_len] = 0;
    env->ReleaseByteArrayElements(bytearray, bytes, 0);
    return chars;
}

void lameInit(jint inSampleRate,
              jint channel, jint mode, jint outSampleRate,
              jint outBitRate, jint quality) {
    resetLame();
    lame = lame_init();
    lame_set_in_samplerate(lame, inSampleRate);
    lame_set_num_channels(lame, channel);
    lame_set_out_samplerate(lame, outSampleRate);
    lame_set_brate(lame, outBitRate);
    lame_set_quality(lame, quality);
    if (mode == 0) { // use CBR
        lame_set_VBR(lame, vbr_default);
    } else if (mode == 1) { //use VBR
        lame_set_VBR(lame, vbr_abr);
    } else { // use ABR
        lame_set_VBR(lame, vbr_mtrh);
    }
    lame_init_params(lame);
}

void convert(JNIEnv *env,
             jstring input_path, jstring mp3_path, jint inSampleRate) {
    const char *cInput = env->GetStringUTFChars(input_path, nullptr);
    const char *cMp3 = env->GetStringUTFChars(mp3_path, nullptr);
    //open input file and output file
    FILE *fInput = fopen(cInput, "rb");
    // 去掉文件头信息
    fseek(fInput, 4 * 1024, SEEK_CUR);
    FILE *fMp3 = fopen(cMp3, "wb");
    short int inputBuffer[BUFFER_SIZE * 2];
    unsigned char mp3Buffer[BUFFER_SIZE];//You must specified at least 7200
    int read = 0; // number of bytes in inputBuffer, if in the end return 0
    int write = 0;// number of bytes output in mp3buffer.  can be 0
    long total = 0; // the bytes of reading input file
    nowConvertBytes = 0;
    //if you don't init lame, it will init lame use the default value
    if (lame == nullptr) {
        lameInit(inSampleRate, 2, 0, 44100, 96, 7);
    }

    //convert to mp3
    do {
        read = static_cast<int>(fread(inputBuffer, sizeof(short int) * 2, BUFFER_SIZE, fInput));
        total += read * sizeof(short int) * 2;
        nowConvertBytes = total;
        if (read != 0) {
            write = lame_encode_buffer_interleaved(lame, inputBuffer, read, mp3Buffer, BUFFER_SIZE);
            //write the converted buffer to the file
            fwrite(mp3Buffer, sizeof(unsigned char), static_cast<size_t>(write), fMp3);
        }
        //if in the end flush
        if (read == 0) {
            lame_encode_flush(lame, mp3Buffer, BUFFER_SIZE);
        }
    } while (read != 0);

    //release resources
    resetLame();
    fclose(fInput);
    fclose(fMp3);
    env->ReleaseStringUTFChars(input_path, cInput);
    env->ReleaseStringUTFChars(mp3_path, cMp3);
    nowConvertBytes = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_init(JNIEnv *env, jclass clazz,
                                                      jint in_sample_rate, jint channel, jint mode,
                                                      jint out_sample_rate, jint out_bit_rate,
                                                      jint quality) {
    lameInit(in_sample_rate, channel, mode, out_sample_rate, out_bit_rate, quality);
}

extern "C"
JNIEXPORT void JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_convertMp3(JNIEnv *env, jclass clazz,
                                                            jstring input_path, jstring mp3_path,
                                                            jint inSampleRate) {
    convert(env, input_path, mp3_path, inSampleRate);
}

extern "C"
JNIEXPORT jint JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_encode(JNIEnv *env, jclass clazz,
                                                        jshortArray buffer_left,
                                                        jshortArray buffer_right, jint samples,
                                                        jbyteArray mp3buf) {
    jshort *j_buffer_l = env->GetShortArrayElements(buffer_left, nullptr);
    jshort *j_buffer_r = env->GetShortArrayElements(buffer_right, nullptr);

    const jsize mp3buf_size = env->GetArrayLength(mp3buf);
    unsigned char *c_mp3buf = convertJByteArrayToChars(env, mp3buf);
    int result = lame_encode_buffer(lame, j_buffer_l, j_buffer_r,
                                    samples, c_mp3buf, mp3buf_size);

    env->ReleaseShortArrayElements(buffer_left, j_buffer_l, 0);
    env->ReleaseShortArrayElements(buffer_right, j_buffer_r, 0);
    *c_mp3buf = '\0';
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_flush(JNIEnv *env, jclass clazz,
                                                       jbyteArray mp3buf) {
    const jsize mp3buf_size = env->GetArrayLength(mp3buf);
    unsigned char *c_mp3buf = convertJByteArrayToChars(env, mp3buf);

    int result = lame_encode_flush(lame, c_mp3buf, mp3buf_size);

    *c_mp3buf = '\0';
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_close(JNIEnv *env, jclass clazz) {
    lame_close(lame);
    lame = nullptr;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_getConvertBytes(JNIEnv *env, jclass clazz) {
    return nowConvertBytes;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_getLameVersion(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(get_lame_version());
}

extern "C"
JNIEXPORT void JNICALL
Java_cc_ibooker_android_zlamemp3lib_Mp3Converter_wavConvertMp3(JNIEnv *env, jclass clazz,
                                                               jstring input_path,
                                                               jstring mp3_path) {
    convert(env, input_path, mp3_path, 44100);
}