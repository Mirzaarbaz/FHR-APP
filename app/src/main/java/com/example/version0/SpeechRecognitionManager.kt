package com.example.version0

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.Locale

class SpeechRecognitionManager(private val context: Context) : RecognitionListener {

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)
    private var resultListener: ResultListener? = null

    init {
        speechRecognizer.setRecognitionListener(this)
    }

    fun startSpeechRecognition() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true) // Mute other audio streams

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something")

        try {
            speechRecognizer.startListening(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Speech recognition not supported on this device",
                Toast.LENGTH_SHORT
            ).show()
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
                // Notify the result to the listener
                resultListener?.onResult(numericMatches)
            } else {
                resultListener?.onError("Speak Again")
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Nothing
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        // Nothing
    }

    override fun onError(error: Int) {
        // Notify the error to the listener
        resultListener?.onError("Error: $error")
    }

    override fun onReadyForSpeech(params: Bundle?) {
        // Implementation for onReadyForSpeech
    }

    override fun onBeginningOfSpeech() {
        // Nothing
    }

    override fun onRmsChanged(rmsdB: Float) {

    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Nothing
    }

    override fun onEndOfSpeech() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
    }

    private fun extractNumeric(text: String): List<Int> {
        // This is a basic example; you might need to handle Hindi numbers differently
        // based on the way they are spoken.
        val words = text.split("\\s+".toRegex())
        return words.filter { it.matches(Regex("-?\\d+(\\.\\d*)?")) || it.matches(Regex("-?\\d+")) }
            .map { it.toInt() }

    }

    // Implement other required methods from RecognitionListener interface
}
