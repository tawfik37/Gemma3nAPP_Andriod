package com.yourname.androidllmapp.data

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LLMManager {

    private var llm: LlmInference? = null
    private var cacheDir: File? = null
    private var modelIdentifier: String = "gemma-3n-E2B-it-int4.task"

    /**
     * Initialize the LLM model from assets and copy to cache if needed.
     * Must be called before any inference.
     */
    suspend fun initialize(context: Context, modelName: String = modelIdentifier) {
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

            // Initialize LLM Options
            val options = LlmInferenceOptions.builder()
                .setModelPath(cachedModel.absolutePath)
                .setMaxTokens(1000)
                // If your model supports vision:
                .setMaxImages(1)
                .build()

            llm?.close() // Close any existing instance
            llm = LlmInference.createFromOptions(context, options)
        }
    }

    /**
     * Send a single text message synchronously and get the response.
     */
    suspend fun generateTextResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            llm?.generateResponse(prompt) ?: "LLM not initialized!"
        }
    }

    /**
     * Add an image to the current context (if vision is supported).
     */
    suspend fun addImageToContext(image: Bitmap) {
        withContext(Dispatchers.IO) {
            llm?.addImage(image)
        }
    }

    /**
     * Close the model (call on app exit or when switching models).
     */
    fun close() {
        llm?.close()
        llm = null
    }
}
