package com.yourname.androidllmapp.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.androidllmapp.data.LLMManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Locale

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUserMessage: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val image: Bitmap? = null
)

class ChatViewModel : ViewModel() {

    // Chat UI state
    val messages = mutableStateListOf<Message>()
    var inputText = mutableStateOf("")
    var selectedImage = mutableStateOf<Bitmap?>(null)

    // Loading states
    var isModelLoading = mutableStateOf(true)
    var isThinking = mutableStateOf(false)
    var isTranscribing = mutableStateOf(false)

    //For Text to speech
    private var tts: TextToSpeech? = null
    var isTtsReady = mutableStateOf(false)


    // Inference settings (default values similar to iOS)
    var topK = mutableStateOf(40)
    var topP = mutableStateOf(0.9f)
    var temperature = mutableStateOf(0.9f)
    var enableVisionModality = mutableStateOf(true)

    private var generationJob: Job? = null

    /** Initialize the model */
    fun initializeModel(context: Context, modelName: String = "gemma-3n-E2B-it-int4.task") {
        isModelLoading.value = true
        messages.clear()
        messages.add(Message(content = "Initializing model... Please wait.", isUserMessage = false))

        viewModelScope.launch {
            try {
                LLMManager.initialize(context, modelName)
                isModelLoading.value = false
                messages.clear()
                messages.add(
                    Message(content = "Model loaded successfully. Hello! How can I help?",
                        isUserMessage = false)
                )
            } catch (e: Exception) {
                isModelLoading.value = false
                messages.clear()
                messages.add(Message(content = "Error loading model: ${e.message}", isUserMessage = false))
            }
        }
    }

    /** Send message to LLM */
    fun sendMessage(context: Context, sourceLang: String, targetLang: String)
    {
        val text = inputText.value.trim()
        val imageToSend = selectedImage.value

        if (text.isEmpty() && imageToSend == null) return

        // Add user's message to the list
        messages.add(Message(content = text, isUserMessage = true, image = imageToSend))

        inputText.value = ""
        selectedImage.value = null
        isThinking.value = true

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                // If image is selected and vision is enabled
                if (enableVisionModality.value && imageToSend != null) {
                    LLMManager.addImageToContext(imageToSend)
                }

                val prompt = when {
                    imageToSend != null && text.isBlank() -> {
                        "Look at the image. Reply with the object name in $sourceLang, then in $targetLang, separated by a newline. Nothing else."
                    }
                    else -> {
                        "Translate this to $targetLang. Only give the translated sentence: ${text.trim()}"
                    }
                }

                val response = LLMManager.generateTextResponse(prompt, sourceLang, targetLang)
                messages.add(Message(content = response, isUserMessage = false))
            } catch (e: Exception) {
                messages.add(
                    Message(content = "Error generating response: ${e.message}", isUserMessage = false)
                )
            } finally {
                isThinking.value = false
            }
        }
    }

    /** Stop ongoing generation */
    fun stopGeneration() {
        generationJob?.cancel()
        isThinking.value = false
    }

    /** Clear chat history */
    fun clearChat() {
        messages.clear()
        messages.add(Message(content = "Chat cleared. Ready for a new conversation.", isUserMessage = false))
    }
    fun deleteOldRecordings(context: Context) {
        val dir = File(context.filesDir, "whisper_recordings")
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".wav")) {
                    file.delete()
                }
            }
        }
    }
    fun initializeTTS(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(Locale.ENGLISH) // default

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Default language not supported or missing data")
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }
    }

    fun speakText(text: String, lang: String, context: Context) {
        val locale = when (lang.lowercase()) {
            "arabic" -> Locale("ar")
            "french" -> Locale.FRENCH
            "german" -> Locale.GERMAN
            "spanish" -> Locale("es")
            else -> Locale.ENGLISH
        }

        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "TTS data for $lang is missing. Opening TTS settings...", Toast.LENGTH_LONG).show()
            val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            context.startActivity(installIntent)
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ttsMessage")
    }


    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
    fun askForContext(originalTranslation: String, sourceLang: String, targetLang: String, context: Context) {
        isThinking.value = true
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                val prompt = """
                You gave a translation: "$originalTranslation".
                Now explain so brielfy and short this translation in its $targetLang context: usage, nuance, cultural significance, etc.
                Please answer in $targetLang only.
            """.trimIndent()


                val response = LLMManager.generateTextResponse(prompt, sourceLang, targetLang)
                messages.add(Message(content = response, isUserMessage = false))
            } catch (e: Exception) {
                messages.add(Message(content = "Error: ${e.message}", isUserMessage = false))
            } finally {
                isThinking.value = false
            }
        }
    }
}
