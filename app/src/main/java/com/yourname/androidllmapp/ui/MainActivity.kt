package com.yourname.androidllmapp.ui   // ✅ Must match your folder structure

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.yourname.androidllmapp.ui.screens.ChatScreen
import java.io.File


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Request audio permission at runtime
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1001
        )
        setContent {
            ChatScreen()
        }
    }
}
fun deleteOldRecordings(context: android.content.Context) {
    val dir = File(context.filesDir, "whisper_recordings")
    if (dir.exists()) {
        dir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".m4a") || file.name.endsWith(".wav"))) {
                file.delete()
            }
        }
    }
}