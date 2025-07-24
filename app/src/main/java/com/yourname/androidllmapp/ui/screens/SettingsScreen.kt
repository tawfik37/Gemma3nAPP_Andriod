package com.yourname.androidllmapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourname.androidllmapp.ui.ChatViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onClose: () -> Unit
) {
    var topK by remember { mutableStateOf(viewModel.topK.value) }
    var topP by remember { mutableStateOf(viewModel.topP.value) }
    var temperature by remember { mutableStateOf(viewModel.temperature.value) }
    var visionEnabled by remember { mutableStateOf(viewModel.enableVisionModality.value) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inference Settings") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Top-K: $topK")
            Slider(
                value = topK.toFloat(),
                onValueChange = { topK = it.toInt() },
                valueRange = 1f..100f
            )

            Text("Top-P: ${String.format("%.2f", topP)}")
            Slider(
                value = topP,
                onValueChange = { topP = it },
                valueRange = 0f..1f
            )

            Text("Temperature: ${String.format("%.2f", temperature)}")
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..2f
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = visionEnabled,
                    onCheckedChange = { visionEnabled = it }
                )
                Text("Enable Vision Modality")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.topK.value = topK
                    viewModel.topP.value = topP
                    viewModel.temperature.value = temperature
                    viewModel.enableVisionModality.value = visionEnabled
                    viewModel.clearChat()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply & Reset Chat")
            }

            OutlinedButton(
                onClick = {
                    viewModel.topK.value = 40
                    viewModel.topP.value = 0.9f
                    viewModel.temperature.value = 0.9f
                    viewModel.enableVisionModality.value = true
                    viewModel.clearChat()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}
