package com.example.version0

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*


class TextToSpeechManager(context: Context, private val listener: TextToSpeech.OnInitListener) {

    private val textToSpeech: TextToSpeech = TextToSpeech(context, listener)

    fun speak(message: String, utteranceId: String) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
