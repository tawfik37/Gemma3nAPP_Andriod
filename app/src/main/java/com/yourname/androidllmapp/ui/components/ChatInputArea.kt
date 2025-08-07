package com.yourname.androidllmapp.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yourname.androidllmapp.data.AudioRecorder
import com.yourname.androidllmapp.ui.ChatViewModel
import com.yourname.androidllmapp.ui.screens.CameraPreview
import loadBitmapWithCorrectRotation
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputArea(
    viewModel: ChatViewModel,
    context: Context,
    sourceLang: String,
    targetLang: String
) {
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile: File? by remember { mutableStateOf(null) }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            recordedFile = AudioRecorder.startRecording(context)
            isRecording = true
        } else {
            Toast.makeText(context, "🎙️ Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    var showCamera by remember { mutableStateOf(false) }

    if (showCamera) {
        CameraPreview(
            onCapture = { bitmap ->
                viewModel.selectedImage.value = bitmap
                showCamera = false
            },
            onClose = { showCamera = false }
        )
        return
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapWithCorrectRotation(context, uri)
            viewModel.selectedImage.value = bitmap
        }
    }

    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        viewModel.selectedImage.value?.let { img ->
            Image(
                bitmap = img.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
            TextButton(
                onClick = { viewModel.selectedImage.value = null },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Remove Image")
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {

            // Input field

            val keyboardController = LocalSoftwareKeyboardController.current

            TextField(
                value = viewModel.inputText.value,
                onValueChange = { viewModel.inputText.value = it },
                placeholder = {
                    Text(
                        "Type text to translate",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                singleLine = false,
                maxLines = 3,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.sendMessage(context, sourceLang, targetLang)
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )


            // Icons row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                IconButton(onClick = { showCamera = true }) {
                    Text("📷", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("🖼", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = {
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
                            viewModel.transcribeAndSend(file, context, sourceLang, targetLang)
                        }
                    }
                }) {
                    Text(if (isRecording) "⏹" else "🎤", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
