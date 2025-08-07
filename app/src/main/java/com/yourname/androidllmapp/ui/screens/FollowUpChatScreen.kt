package com.yourname.androidllmapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourname.androidllmapp.ui.ChatViewModel

@Composable
fun FollowUpChatScreen(
    initialMessage: String,
    sourceLang: String,
    targetLang: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Discussion about:", style = MaterialTheme.typography.titleMedium)
        Text(text = initialMessage, modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
        ) {
            items(viewModel.followUpMessages) { msg ->
                Text(
                    text = if (msg.isUserMessage) "👤 ${msg.content}" else "🤖 ${msg.content}",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text("Ask about this...") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { onBack() }) {
                Text("Back")
            }

            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.sendFollowUpMessage(input, initialMessage, context, sourceLang, targetLang)
                        input = ""
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}
