package com.yourname.androidllmapp.ui

import android.content.Context
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
    fun sendMessage(context: Context) {
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

                val response = LLMManager.generateTextResponse(text.ifEmpty { " " })
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
                if (file.isFile && file.name.endsWith(".m4a")) {
                    file.delete()
                }
            }
        }
    }

}
