package com.yourname.androidllmapp.data

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LLMManager {

    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var cacheDir: File? = null
    private var modelIdentifier: String = "gemma-3n-E2B-it-int4.task"

    /**
     * Initialize the LLM model and session.
     */
    suspend fun initialize(
        context: Context,
        modelName: String = modelIdentifier,
        topK: Int = 40,
        temperature: Float = 0.8f,
        enableVision: Boolean = true
    ) {
        withContext(Dispatchers.IO) {
            if (llm != null && modelIdentifier == modelName) {
                return@withContext // Already initialized with same model
            }

            modelIdentifier = modelName
            cacheDir = File(context.filesDir, "llm_cache")
            if (!cacheDir!!.exists()) {
                cacheDir!!.mkdirs()
            }

            // Copy model from assets to cache if not already copied
            val cachedModel = File(cacheDir, modelName)
            if (!cachedModel.exists()) {
                context.assets.open(modelName).use { input ->
                    FileOutputStream(cachedModel).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Base inference options
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(cachedModel.absolutePath)
                .setMaxTokens(1000)
                .setMaxNumImages(if (enableVision) 1 else 0)
                .build()

            llm?.close() // Close old instance if exists
            llm = LlmInference.createFromOptions(context, options)

            // Session options (for each conversation)
            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTopK(topK)
                .setTemperature(temperature)
                .setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(enableVision)
                        .build()
                )
                .build()

            session?.close()
            session = LlmInferenceSession.createFromOptions(llm!!, sessionOptions)
        }
    }

    /**
     * Send a text message and get the generated response.
     */
    suspend fun generateTextResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            session?.addQueryChunk(prompt)
            session?.generateResponse() ?: "LLM not initialized!"
        }
    }

    /**
     * Add an image to the current session (must be called before sending text).
     */
    suspend fun addImageToContext(image: Bitmap) {
        withContext(Dispatchers.IO) {
            val mpImage: MPImage = BitmapImageBuilder(image).build()
            session?.addImage(mpImage)
        }
    }

    /**
     * Close the session and model (optional on app exit).
     */
    fun close() {
        session?.close()
        llm?.close()
        session = null
        llm = null
    }
    fun copyModelFromAssets(context: Context): File {
        val file = File(context.filesDir, "ggml-base.en.bin")
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
