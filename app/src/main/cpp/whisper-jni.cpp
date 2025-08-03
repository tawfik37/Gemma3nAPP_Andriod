#include <jni.h>
#include <vector>
#include <string>
#include <android/log.h>

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define DR_WAV_IMPLEMENTATION
#include "dr_wav.h" // bundled with whisper.cpp
#include "whisper.h"

// ✅ WAV loader helper (loads 16-bit PCM .wav as float32 samples)
std::vector<float> load_wav_file(const char *filename, int &n_samples_out) {
    drwav wav;
    if (!drwav_init_file(&wav, filename, nullptr)) {
        LOGE("Failed to open WAV file: %s", filename);
        return {};
    }

    std::vector<float> pcmf32(wav.totalPCMFrameCount * wav.channels);
    drwav_read_pcm_frames_f32(&wav, wav.totalPCMFrameCount, pcmf32.data());
    drwav_uninit(&wav);

    n_samples_out = pcmf32.size();
    LOGI("Loaded %d samples from WAV file.", n_samples_out);
    return pcmf32;
}

// ✅ JNI interface for Kotlin -> native transcription
extern "C"
JNIEXPORT jstring JNICALL
Java_com_yourname_androidllmapp_data_WhisperBridge_transcribe(
        JNIEnv *env,
        jobject /* this */,
        jstring audioPath,
        jstring modelPath
) {
    const char *audio = env->GetStringUTFChars(audioPath, nullptr);
    const char *model = env->GetStringUTFChars(modelPath, nullptr);

    // ✅ Load model
    whisper_context *ctx = whisper_init_from_file(model);
    if (!ctx) {
        LOGE("Failed to load Whisper model from: %s", model);
        env->ReleaseStringUTFChars(audioPath, audio);
        env->ReleaseStringUTFChars(modelPath, model);
        return env->NewStringUTF("ERROR: Model load failed");
    }

    // ✅ Load audio PCM
    int n_samples = 0;
    std::vector<float> pcmf32 = load_wav_file(audio, n_samples);
    if (pcmf32.empty()) {
        whisper_free(ctx);
        env->ReleaseStringUTFChars(audioPath, audio);
        env->ReleaseStringUTFChars(modelPath, model);
        return env->NewStringUTF("ERROR: Failed to load WAV audio");
    }

    // ✅ Transcribe
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    if (whisper_full(ctx, params, pcmf32.data(), n_samples) != 0) {
        LOGE("Whisper transcription failed");
        whisper_free(ctx);
        env->ReleaseStringUTFChars(audioPath, audio);
        env->ReleaseStringUTFChars(modelPath, model);
        return env->NewStringUTF("ERROR: Transcription failed");
    }

    // ✅ Get result (first segment only)
    const int n_segments = whisper_full_n_segments(ctx);
    std::string result;
    for (int i = 0; i < n_segments; ++i) {
        result += whisper_full_get_segment_text(ctx, i);
        result += " ";
    }

    // ✅ Cleanup
    whisper_free(ctx);
    env->ReleaseStringUTFChars(audioPath, audio);
    env->ReleaseStringUTFChars(modelPath, model);

    return env->NewStringUTF(result.c_str());
}
