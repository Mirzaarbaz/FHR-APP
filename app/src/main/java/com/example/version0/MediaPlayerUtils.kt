package com.example.version0

import SpeechRecognitionManager
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.version0.R

object MediaPlayerUtils {

    private var mediaPlayer: MediaPlayer? = null

    fun initializeMediaPlayer(context: Context) {
        // Release any existing MediaPlayer instance
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, R.raw.fhr)
        mediaPlayer?.apply {
            setOnPreparedListener {
                // MediaPlayer is prepared and ready to start
            }
            setOnCompletionListener {
                val activity = context as? AppCompatActivity
                activity?.let {
                    ButtonStyleUtils.setListeningStyle(it, it.findViewById(R.id.yourButton), it.findViewById(R.id.card1))
                    SpeechRecognitionManager(it).startSpeechRecognition()
                }
            }
        }
    }

    fun startListening(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            it.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume - 1, 0)
        }

        try {
            mediaPlayer?.start()
        } catch (e: IllegalStateException) {
            Log.e("MediaPlayerUtils", "IllegalStateException: ${e.message}")
            initializeMediaPlayer(context)
            mediaPlayer?.start()
        }
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
