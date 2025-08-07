package com.yourname.androidllmapp.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.core.app.ActivityCompat
import com.yourname.androidllmapp.ui.screens.ChatScreen
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request microphone permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1001
        )

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                ChatScreen()
            }
        }
    }
}

fun deleteOldRecordings(context: android.content.Context) {
    val dir = File(context.filesDir, "whisper_recordings")
    if (dir.exists()) {
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".wav")) {
                file.delete()
            }
        }
    }
}
