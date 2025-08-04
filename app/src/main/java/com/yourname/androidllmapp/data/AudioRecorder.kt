package com.yourname.androidllmapp.data

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.*

object AudioRecorder {
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var outputFile: File? = null

    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    fun startRecording(context: Context): File {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        val outputDir = File(context.filesDir, "whisper_recordings")
        if (!outputDir.exists()) outputDir.mkdirs()

        outputFile = File(outputDir, "recording_${System.currentTimeMillis()}.wav")
        val rawData = File(outputDir, "temp_${System.currentTimeMillis()}.pcm")

        isRecording = true
        recorder?.startRecording()

        recordingThread = Thread {
            val buffer = ByteArray(bufferSize)
            FileOutputStream(rawData).use { out ->
                while (isRecording) {
                    val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) out.write(buffer, 0, read)
                }
            }
            writeWavHeader(rawData, outputFile!!, SAMPLE_RATE, 1, 16)
            rawData.delete()
        }
        recordingThread?.start()

        return outputFile!!
    }

    fun stopRecording(): File? {
        isRecording = false
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        recordingThread?.join()
        return outputFile
    }

    // ✅ Helper: Convert raw PCM into WAV
    private fun writeWavHeader(pcmFile: File, wavFile: File, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val pcmData = pcmFile.readBytes()
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        DataOutputStream(FileOutputStream(wavFile)).use { out ->
            out.writeBytes("RIFF")
            out.writeIntLE(totalDataLen)
            out.writeBytes("WAVE")
            out.writeBytes("fmt ")
            out.writeIntLE(16)
            out.writeShortLE(1) // PCM
            out.writeShortLE(channels.toShort())
            out.writeIntLE(sampleRate)
            out.writeIntLE(byteRate)
            out.writeShortLE((channels * bitsPerSample / 8).toShort())
            out.writeShortLE(bitsPerSample.toShort())
            out.writeBytes("data")
            out.writeIntLE(pcmData.size)
            out.write(pcmData)
        }
    }

    private fun DataOutputStream.writeIntLE(value: Int) {
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        ))
    }

    private fun DataOutputStream.writeShortLE(value: Short) {
        write(byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        ))
    }
}
