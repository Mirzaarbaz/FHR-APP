import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import com.example.version0.ButtonStyleUtils
import com.example.version0.MainActivity
import com.example.version0.ResultListener
import java.util.Locale

class SpeechRecognitionManager(private val context: Context) : RecognitionListener {

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var resultListener: ResultListener? = null
    private val handler = Handler(Looper.getMainLooper())
    var retryCount = 0
    private val maxRetries = 3

    init {
        speechRecognizer.setRecognitionListener(this)
    }

    fun startSpeechRecognition() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("hi", "IN")) // Hindi locale
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something")
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun restartSpeechRecognitionAfterDelay() {
        if (retryCount < maxRetries) {
            retryCount++
            handler.postDelayed({
                // Update UI to reflect the listening state
                if (context is MainActivity) {
                    ButtonStyleUtils.setListeningStyle(context, context.yourButton, context.card1)
                }
                startSpeechRecognition()
            }, 2000) // 2 seconds delay
        } else {
            // Notify the listener about maximum retries reached
            resultListener?.onError("Click Start")
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
                retryCount = 0 // Reset retry count on success
                resultListener?.onResult(numericMatches)
            } else {
                resultListener?.onError("Speak Again")
                restartSpeechRecognitionAfterDelay()
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onError(error: Int) {
        resultListener?.onError("Try Again")
        restartSpeechRecognitionAfterDelay()
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