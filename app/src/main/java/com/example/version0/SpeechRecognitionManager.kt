package com.example.version0

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.*

class SpeechRecognitionManager(private val context: Context) : RecognitionListener {

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var resultListener: ResultListener? = null

    init {
        speechRecognizer.setRecognitionListener(this)
    }

    fun startSpeechRecognition() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something")
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun setResultListener(listener: ResultListener) {
        this.resultListener = listener
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val numericMatches = extractNumeric(matches[0])
            if (numericMatches.isNotEmpty()) {
                resultListener?.onResult(numericMatches)
            } else {
                resultListener?.onError("Speak Again")
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onError(error: Int) {
        resultListener?.onError("Error: $error")
    }

    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
    }

    private fun extractNumeric(text: String): List<Int> {
        val words = text.split("\\s+".toRegex())
        return words.filter { it.matches(Regex("-?\\d+(\\.\\d*)?")) || it.matches(Regex("-?\\d+")) }
            .map { it.toInt() }
    }
}
