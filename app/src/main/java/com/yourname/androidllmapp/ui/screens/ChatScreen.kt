package com.yourname.androidllmapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.androidllmapp.ui.ChatViewModel
import com.yourname.androidllmapp.ui.Message
import kotlinx.coroutines.launch
import com.yourname.androidllmapp.ui.components.DropdownMenuBox
import com.yourname.androidllmapp.ui.components.MessageBubble
import com.yourname.androidllmapp.ui.components.ChatInputArea
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val languages = listOf("English", "Arabic", "French", "Spanish", "German")
    var sourceLang by remember { mutableStateOf("English") }
    var targetLang by remember { mutableStateOf("Arabic") }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showFollowUp by remember { mutableStateOf(false) }
    var followUpMessage by remember { mutableStateOf<Message?>(null) }


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

    if (showFollowUp && followUpMessage != null) {
        FollowUpChatScreen(
            initialMessage = followUpMessage!!.content,
            sourceLang = sourceLang,
            targetLang = targetLang,
            viewModel = viewModel,
            onBack = {
                showFollowUp = false
                followUpMessage = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // From Language
                DropdownMenuBox(
                    options = languages,
                    selectedOption = sourceLang,
                    onSelected = { sourceLang = it }
                )

                // Swap Arrow (visual only)
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap",
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(28.dp),
                    tint = Color(0xFF007AFF) // Blue color
                )

                // To Language
                DropdownMenuBox(
                    options = languages,
                    selectedOption = targetLang,
                    onSelected = { targetLang = it }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(viewModel.messages) { message ->
                    MessageBubble(
                        message = message,
                        targetLang = targetLang,
                        sourceLang = sourceLang,
                        context = context,
                        viewModel = viewModel,
                        onFollowUpClick = {
                            showFollowUp = true
                            followUpMessage = it
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Divider()
            ChatInputArea(viewModel, context, sourceLang, targetLang)
        }
    }
}