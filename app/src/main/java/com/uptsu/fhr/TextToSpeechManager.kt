package com.uptsu.fhr

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech

class TextToSpeechManager(
    context: Context,
    private val listener: TextToSpeech.OnInitListener
) {

    val textToSpeech: TextToSpeech = TextToSpeech(context, listener)
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        setVolumeToMax()
    }

    fun speak(message: String, utteranceId: String) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun setVolumeToMax() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }
}
