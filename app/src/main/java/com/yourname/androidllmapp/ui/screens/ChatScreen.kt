package com.yourname.androidllmapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.androidllmapp.ui.ChatViewModel
import com.yourname.androidllmapp.ui.Message
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.yourname.androidllmapp.data.AudioRecorder
import java.io.File
import com.yourname.androidllmapp.data.WhisperBridge
import loadBitmapWithCorrectRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val languages = listOf("English", "Arabic", "French", "Spanish", "German")
    var sourceLang by remember { mutableStateOf("English") }
    var targetLang by remember { mutableStateOf("Arabic") }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.initializeModel(context)
        viewModel.initializeTTS(context)
    }

    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { coroutineScope.launch { sheetState.hide() } },
            sheetState = sheetState
        ) {
            SettingsScreen(viewModel) {
                coroutineScope.launch { sheetState.hide() }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Offline LLM Chat") },
            actions = {
                IconButton(onClick = {
                    coroutineScope.launch { sheetState.show() }
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        )
        Row(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("From:")
                DropdownMenuBox(languages, sourceLang) { selected ->
                    sourceLang = selected
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("To:")
                DropdownMenuBox(languages, targetLang) { selected ->
                    targetLang = selected
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            items(viewModel.messages) { message ->
                MessageBubble(
                    message = message,
                    isThinking = viewModel.isThinking.value,
                    targetLang = targetLang,
                    viewModel = viewModel
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Divider()
        ChatInputArea(viewModel, context, sourceLang, targetLang)
    }
}

@Composable
fun DropdownMenuBox(
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedOption)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(label)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isThinking: Boolean, targetLang: String, viewModel: ChatViewModel) {
    val isUser = message.isUserMessage
    val bubbleColor =
        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor =
        if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(10.dp)
                .widthIn(max = 300.dp)
        ) {
            message.image?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Attached Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = if (isThinking && !isUser) "Thinking..." else message.content,
                color = textColor
            )
            if (!message.isUserMessage) {
                val context = LocalContext.current
                TextButton(
                    onClick = { viewModel.speakText(message.content, targetLang, context) }
                ) {
                    Text("🔈 Listen")
                }
            }
        }
    }
}


@Composable
fun ChatInputArea(
    viewModel: ChatViewModel,
    context: android.content.Context,
    sourceLang: String,
    targetLang: String
)
 {
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile: File? by remember { mutableStateOf(null) }
     val scope = rememberCoroutineScope()
     val permissionLauncher = rememberLauncherForActivityResult(
         contract = ActivityResultContracts.RequestPermission()
     ) { isGranted ->
         if (isGranted) {
             recordedFile = AudioRecorder.startRecording(context)
             isRecording = true
         } else {
             Toast.makeText(context, "🎙️ Microphone permission denied", Toast.LENGTH_SHORT).show()
         }
     }

     Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = {
                if (!isRecording) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        recordedFile = AudioRecorder.startRecording(context)
                        isRecording = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                } else {
                    val file = AudioRecorder.stopRecording()
                    isRecording = false
                    recordedFile = file

                    if (file != null && file.exists()) {
                        try {
                            val modelFile = WhisperBridge.copyWhisperModelFromAssets(context)
                            Log.d("WhisperDebug", "Model path: ${modelFile.absolutePath}, size: ${modelFile.length()}")

                            val audioFile = file ?: throw Exception("Audio file is null")
                            Log.d("WhisperDebug", "Audio path: ${audioFile.absolutePath}, size: ${audioFile.length()}")

                            if (viewModel.isTranscribing.value) {
                                Toast.makeText(context, "⏳ Transcription in progress...", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.isTranscribing.value = true

                            scope.launch {
                                try {
                                    val transcript = withContext(Dispatchers.IO) {
                                        WhisperBridge.transcribe(file.absolutePath, modelFile.absolutePath)
                                    }
                                    viewModel.inputText.value = transcript
                                    viewModel.sendMessage(context, sourceLang, targetLang)
                                    file.delete()
                                } catch (e: Exception) {
                                    Log.e("WhisperError", "Transcription failed", e)
                                    Toast.makeText(context, "❌ Transcription failed", Toast.LENGTH_SHORT).show()
                                } finally {
                                    viewModel.isTranscribing.value = false
                                }
                            }



                        } catch (e: Exception) {
                            Log.e("WhisperError", "Transcription failed", e)
                            Toast.makeText(context, "❌ Transcription failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            enabled = !viewModel.isTranscribing.value,
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isRecording) "⏹" else "🎤")
        }

        Spacer(modifier = Modifier.width(6.dp))
    }



    var showCamera by remember { mutableStateOf(false) }

        if (showCamera) {
            // ✅ Show the in-app camera preview instead of the chat input
            CameraPreview(
                onCapture = { bitmap ->
                    viewModel.selectedImage.value = bitmap
                    showCamera = false
                },
                onClose = { showCamera = false }
            )
        } else {
            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val bitmap = loadBitmapWithCorrectRotation(context, uri)
                    viewModel.selectedImage.value = bitmap
                }
            }

            val keyboardController = LocalSoftwareKeyboardController.current

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            ) {
                // ✅ Show selected image preview if exists
                viewModel.selectedImage.value?.let { img ->
                    Image(
                        bitmap = img.asImageBitmap(),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.selectedImage.value = null }) {
                        Text("Remove Image")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ✅ Camera Button
                    Button(
                        onClick = { showCamera = true },
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Text("📸")
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // ✅ Gallery Button
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Text("🖼")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // ✅ Text Input
                    OutlinedTextField(
                        value = viewModel.inputText.value,
                        onValueChange = { viewModel.inputText.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        singleLine = false,
                        enabled = !viewModel.isModelLoading.value,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (viewModel.inputText.value.isNotBlank() || viewModel.selectedImage.value != null) {
                                    viewModel.sendMessage(context, sourceLang, targetLang)
                                    keyboardController?.hide()
                                }
                            }
                        )
                    )

                    if (viewModel.isThinking.value) {
                        Button(
                            onClick = { viewModel.stopGeneration() },
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Text("■")
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.sendMessage(context, sourceLang, targetLang)
                                keyboardController?.hide()
                            },
                            enabled = viewModel.inputText.value.isNotBlank() || viewModel.selectedImage.value != null,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Text("➤")
                        }
                    }
                }

                TextButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear Chat")
                }
            }
        }
    }