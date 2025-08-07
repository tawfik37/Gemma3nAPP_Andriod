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
import com.yourname.androidllmapp.data.WhisperBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUserMessage: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val image: Bitmap? = null,
    val original: String? = null,
    val isThinking: Boolean = false,
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
    val followUpMessages = mutableStateListOf<Message>()


    //For Text to speech
    private var tts: TextToSpeech? = null


    // Inference settings (default values similar to iOS)
    var topK = mutableStateOf(40)
    var topP = mutableStateOf(0.9f)
    var temperature = mutableStateOf(0.9f)
    var enableVisionModality = mutableStateOf(true)

    private var generationJob: Job? = null

    //Initialize the model
    fun initializeModel(context: Context, modelName: String = "gemma-3n-E2B-it-int4.task") {
        isModelLoading.value = true
        messages.clear()

        viewModelScope.launch {
            try {
                LLMManager.initialize(context, modelName)
                isModelLoading.value = false
                messages.clear()
            } catch (e: Exception) {
                isModelLoading.value = false
                messages.clear()
                messages.add(Message(content = "Error loading model: ${e.message}", isUserMessage = false))
            }
        }
    }

    // Send message to Gemma 3n
    fun sendMessage(context: Context, sourceLang: String, targetLang: String) {
        val text = inputText.value.trim()
        val imageToSend = selectedImage.value

        if (text.isEmpty() && imageToSend == null) return

        inputText.value = ""
        selectedImage.value = null
        isThinking.value = true

        generationJob?.cancel()

        // Add user's message
        messages.add(Message(content = text, isUserMessage = true))

        // Add placeholder bot message with "Thinking..."
        val placeholder = Message(
            content = "",
            original = text,
            isUserMessage = false,
            isThinking = true
        )
        messages.add(placeholder)

        generationJob = viewModelScope.launch {
            try {
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

                // Replace the placeholder with the real response
                val index = messages.indexOfFirst { it.id == placeholder.id }
                if (index != -1) {
                    messages[index] = placeholder.copy(
                        content = response,
                        isThinking = false
                    )
                }

            } catch (e: Exception) {
                messages.add(
                    Message(
                        content = "Error generating response: ${e.message}",
                        isUserMessage = false,
                        original = text
                    )
                )
            } finally {
                isThinking.value = false
            }
        }
    }


    //Clear chat history
    fun clearChat() {
        messages.clear()
        messages.add(Message(content = "Chat cleared. Ready for a new conversation.", isUserMessage = false))
    }

    //still not used
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


    fun transcribeAndSend(file: File, context: Context, sourceLang: String, targetLang: String) {
        val modelFile = WhisperBridge.copyWhisperModelFromAssets(context)
        viewModelScope.launch {
            isTranscribing.value = true
            try {
                val transcript = withContext(Dispatchers.IO) {
                    WhisperBridge.transcribe(file.absolutePath, modelFile.absolutePath)
                }
                inputText.value = transcript
                sendMessage(context, sourceLang, targetLang)
                file.delete()
            } catch (e: Exception) {
                Log.e("WhisperError", "Transcription failed", e)
                Toast.makeText(context, "❌ Transcription failed", Toast.LENGTH_SHORT).show()
            } finally {
                isTranscribing.value = false
            }
        }
    }

    fun sendFollowUpMessage(
        query: String,
        originalMessage: String,
        context: Context,
        sourceLang: String,
        targetLang: String
    ) {
        followUpMessages.add(Message(content = query, isUserMessage = true))
        viewModelScope.launch {
            try {
                val prompt = """
                    The following is a translation: "$originalMessage".
                
                    Answer the user's question based on it:
                    "$query"
                
                    Do not translate anything.
                    Respond in English only. No other languages.
                """.trimIndent()

                val response = LLMManager.generateTextResponse(prompt, sourceLang, targetLang)
                followUpMessages.add(Message(content = response, isUserMessage = false))
            } catch (e: Exception) {
                followUpMessages.add(Message(content = "Error: ${e.message}", isUserMessage = false))
            }
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}
