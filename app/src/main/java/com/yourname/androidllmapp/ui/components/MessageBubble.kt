package com.yourname.androidllmapp.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.yourname.androidllmapp.ui.ChatViewModel
import com.yourname.androidllmapp.ui.Message

@Composable
fun MessageBubble(
    message: Message,
    targetLang: String,
    sourceLang: String,
    context: Context,
    viewModel: ChatViewModel,
    onFollowUpClick: (Message) -> Unit
) {
    val isUser = message.isUserMessage
    val textColor = MaterialTheme.colorScheme.onSurface
    var isPressed by remember { mutableStateOf(false) }


    if (!isUser) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Column {
                    // Image (if exists)
                    message.image?.let {
                        Text("🖼 Image", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Attached Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Original Input
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", modifier = Modifier.padding(end = 4.dp))
                        Text(
                            text = message.original ?: "User input",
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        isPressed = !isPressed
                                    }
                                )
                            }
                    ) {
                        // Translated or Thinking...
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.speakText(message.content, targetLang, context)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Play",
                                    tint = Color(0xFF007AFF)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = if (message.isThinking && message.content.isEmpty()) "Thinking..." else message.content,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Chat About This Button
                if (isPressed) {
                    Button(
                        onClick = { onFollowUpClick(message) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat About This")
                    }

                }
            }
        }
    }
}
