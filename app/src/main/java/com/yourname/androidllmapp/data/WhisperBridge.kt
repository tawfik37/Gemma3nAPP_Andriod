package com.yourname.androidllmapp.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object WhisperBridge {
    init {
        try {
            System.loadLibrary("whisper-jni")
            Log.d("Whisper", "JNI loaded!")
        } catch (e: Exception) {
            Log.e("Whisper", "Failed to load native lib", e)
        }
    }

    external fun transcribe(audioPath: String, modelPath: String): String

    // ✅ COPY MODEL FUNCTION HERE
    fun copyWhisperModelFromAssets(context: Context): File {
        val modelDir = File(context.filesDir, "models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }

        val file = File(modelDir, "ggml-base.en.bin")
        if (!file.exists()) {
            context.assets.open("models/ggml-base.en.bin").use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }
}