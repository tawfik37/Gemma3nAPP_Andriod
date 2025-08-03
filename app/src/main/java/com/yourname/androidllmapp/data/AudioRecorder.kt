package com.yourname.androidllmapp.data

import android.content.Context
import android.media.MediaRecorder
import java.io.File

object AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(context: Context): File {
        val outputDir = File(context.filesDir, "whisper_recordings")
        if (!outputDir.exists()) outputDir.mkdirs()

        outputFile = File(outputDir, "recording_${System.currentTimeMillis()}.m4a")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }

        return outputFile!!
    }

    fun stopRecording(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile
    }
}
