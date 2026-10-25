package com.bookflow.reader.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    
    private val audioDir = File(context.filesDir, "audio_notes").also { it.mkdirs() }
    
    var isRecording = false
        private set
        
    var isPlaying = false
        private set

    fun startRecording(fileName: String): String? {
        val outputFile = File(audioDir, fileName)
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION.SDK_INT) {
            @Suppress("DEPRECATION")
            MediaRecorder()
        } else {
            MediaRecorder(context)
        }
        
        return try {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            outputFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    fun startPlaying(filePath: String, onCompletion: () -> Unit) {
        if (isPlaying) stopPlaying()
        
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    this@AudioRecorderHelper.isPlaying = false
                    onCompletion()
                }
                start()
                this@AudioRecorderHelper.isPlaying = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopPlaying() {
        if (!isPlaying) return
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }
    
    fun release() {
        stopRecording()
        stopPlaying()
    }
}
