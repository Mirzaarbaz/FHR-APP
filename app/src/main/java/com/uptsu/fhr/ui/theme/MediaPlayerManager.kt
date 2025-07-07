package com.uptsu.fhr

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log

class MediaPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun initializeMediaPlayer() {
        // Release any existing MediaPlayer instance
        mediaPlayer?.release()

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(context, R.raw.fhr)
        mediaPlayer?.setOnPreparedListener {
            // MediaPlayer is prepared and ready to start
            Log.d("MediaPlayerManager", "MediaPlayer prepared")
        }
        mediaPlayer?.setOnCompletionListener {
            Log.d("MediaPlayerManager", "MediaPlayer completed")
            // Handle what should happen when media playback completes
        }
    }

    fun startListening(onCompletion: () -> Unit) {
        // Initialize the MediaPlayer if it's null
        if (mediaPlayer == null) {
            initializeMediaPlayer()
        }

        // Get the AudioManager service
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Set the volume to maximum
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume -1, 0)

        // Start MediaPlayer playback
        try {
            mediaPlayer?.start()
        } catch (e: IllegalStateException) {
            Log.e("MediaPlayerManager", "MediaPlayer error: ${e.message}")
            // Handle the exception or reinitialize MediaPlayer if needed
            initializeMediaPlayer()
            mediaPlayer?.start()
        }

        mediaPlayer?.setOnCompletionListener {
            onCompletion()
            // Handle what should happen when media playback completes
        }
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
