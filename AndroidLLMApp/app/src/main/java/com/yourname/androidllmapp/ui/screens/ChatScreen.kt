package com.yourname.androidllmapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.androidllmapp.ui.ChatViewModel
import com.yourname.androidllmapp.ui.Message
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch


@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.initializeModel(context)
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            SettingsScreen(viewModel) {
                coroutineScope.launch { sheetState.hide() }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            TopAppBar(
                title = { Text("Offline LLM Chat") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch { sheetState.show() }
                    }) {
                        Text("⚙")
                    }
                }
            )

            // Chat list + Input remains same as before
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(viewModel.messages) { message ->
                    MessageBubble(message = message, isThinking = viewModel.isThinking.value)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Divider()
            ChatInputArea(viewModel, context)
        }
    }
}

@Composable
fun MessageBubble(message: Message, isThinking: Boolean) {
    val isUser = message.isUserMessage
    val bubbleColor =
        if (isUser) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val textColor = if (isUser) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
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
            Text(text = if (isThinking && !isUser) "Thinking..." else message.content,
                color = textColor)
        }
    }
}

@Composable
fun ChatInputArea(viewModel: ChatViewModel, context: android.content.Context) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(stream)
            viewModel.selectedImage.value = bitmap
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .padding(8.dp)
    ) {
        // Show selected image preview if exists
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
            // Gallery Button
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                shape = CircleShape,
                modifier = Modifier.size(50.dp)
            ) {
                Text("📷")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Input
            OutlinedTextField(
                value = viewModel.inputText.value,
                onValueChange = { viewModel.inputText.value = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Type a message...") },
                maxLines = 4,
                singleLine = false,
                enabled = !viewModel.isModelLoading.value
            )

            // Send or Stop button
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
                    onClick = { viewModel.sendMessage(context) },
                    enabled = viewModel.inputText.value.isNotBlank() || viewModel.selectedImage.value != null,
                    shape = CircleShape,
                    modifier = Modifier.size(50.dp)
                ) {
                    Text("➤")
                }
            }
        }

        // Clear Chat Button
        TextButton(
            onClick = { viewModel.clearChat() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Clear Chat")
        }
    }
}
