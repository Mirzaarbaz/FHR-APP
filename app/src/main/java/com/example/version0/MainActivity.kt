package com.example.version0

import SpeechRecognitionManager
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, ResultListener {

    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var textToSpeechManager: TextToSpeechManager
    private lateinit var userNameDialog: UserNameDialog
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var finishButton: Button
    lateinit var yourButton: Button
    private lateinit var shareButton: ImageButton
    lateinit var card1: LinearLayout

    private lateinit var resultTextView: TextView
    private lateinit var clockTextView: TextView
    private lateinit var graph: GraphView
    private lateinit var speechLogTable: TableLayout
    private lateinit var clockManager: ClockManager
    private lateinit var graphInitializer: GraphInitializer

    private var speechCounter: Int = 1
    private var userName: String? = null
    private var userNumber: Int = 0
    var isFirstClick = true

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        clockTextView = findViewById(R.id.clockTextView)
        graph = findViewById(R.id.graph)
        speechLogTable = findViewById(R.id.speechLogTable)
        yourButton = findViewById(R.id.yourButton)
        shareButton = findViewById(R.id.shareButton)
        card1 = findViewById(R.id.card1)
        resultTextView = findViewById(R.id.resultTextView)
        finishButton = findViewById(R.id.finish)

        // Initialize components
        graphInitializer = GraphInitializer(this, graph)
        clockManager = ClockManager(clockTextView)
        textToSpeechManager = TextToSpeechManager(this, this)

        // Initialize MediaPlayer with a prepared resource
        initializeMediaPlayer()

        // Initialize SpeechRecognitionManager
        speechRecognitionManager = SpeechRecognitionManager(this)
        speechRecognitionManager.setResultListener(this)

        // Initialize UserNameDialog
        userNameDialog = UserNameDialog(this) { name, number ->
            userName = name
            userNumber = number
            clockManager.startClock()
            checkAndRequestPermissions()
        }

        // Setup PDF sharing
        val pdfUtils = PDFUtils(this)
        shareButton.setOnClickListener {
            pdfUtils.sharePDF(clockTextView, graph, speechLogTable, userName, userNumber.toString())
        }

        yourButton.setOnClickListener {
            speechRecognitionManager.retryCount = 0
            if (isFirstClick) {
                userNameDialog.show()
                isFirstClick = false
            } else {
                clockManager.startClock()
                checkAndRequestPermissions()
            }
        }

        finishButton.setOnClickListener {
            AppUtils.showConfirmationDialog(
                this,
                pdfUtils,
                clockTextView,
                graph,
                speechLogTable,
                userName,
                userNumber,
                ::resetApp
            )
        }
    }

    private fun initializeMediaPlayer() {
        // Release any existing MediaPlayer instance
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.fhr)
        mediaPlayer.setOnPreparedListener {
            // MediaPlayer is prepared and ready to start
        }
        mediaPlayer.setOnCompletionListener {
            runOnUiThread {
                ButtonStyleUtils.setListeningStyle(this@MainActivity, yourButton, card1)
                speechRecognitionManager.startSpeechRecognition()
            }
        }
    }

    private fun startListening() {
        // Check if MediaPlayer is initialized and ready
        if (!::mediaPlayer.isInitialized) {
            initializeMediaPlayer()
        }

        // Get the AudioManager service
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Set the volume to maximum
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume - 1, 0)

        // Start MediaPlayer playback
        try {
            mediaPlayer.start()
        } catch (e: IllegalStateException) {
            // Handle the exception or reinitialize MediaPlayer if needed
            initializeMediaPlayer()
            mediaPlayer.start()
        }
    }

    private fun resetApp() {
        isFirstClick= true
        AppUtils.resetApp(
            this,
            clockTextView,
            graph,
            speechLogTable,
            resultTextView,
            mediaPlayer,
            textToSpeechManager,
            clockManager,
        ) { newGraphInitializer, newClockManager ->
            graphInitializer = newGraphInitializer
            clockManager = newClockManager
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeechManager.stop()
        clockManager.stopClock()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release() // Release MediaPlayer resources
        }
    }

    private fun checkAndRequestPermissions() {
        if (PermissionUtils.hasNotificationPermission(this)) {
            // NotificationUtils.showNotification(this)
        } else {
            PermissionUtils.requestNotificationPermission(this)
        }

        if (PermissionUtils.hasMicrophonePermission(this)) {
            startListening()
        } else {
            PermissionUtils.requestMicrophonePermission(this)
        }
    }

    private fun showNotification() {
        NotificationUtils.showNotification(this)
    }

    private fun listeningDone() {
        NotificationUtils.cancelNotification(this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("hin", "IND")
            val result = textToSpeechManager.textToSpeech.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(
                    this,
                    "Text-to-Speech not supported in this language",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                textToSpeechManager.textToSpeech.setSpeechRate(0.8f)
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResult(numbers: List<Int>) {
        if (numbers.isNotEmpty()) {
            val currentValue = numbers[0]
            graphInitializer.addDataPoint(currentValue.toDouble())
            resultTextView.text = currentValue.toString()

            val formattedTime = "${clockTextView.text} (${convertTime(getCurrentTime())})"
            TableUtils.addDataToTable(
                this,
                speechLogTable,
                speechCounter,
                numbers,
                formattedTime
            )
            speechCounter++

            ButtonStyleUtils.setStartStyle(this, yourButton, card1)
            listeningDone()
        } else {
            onError("No number recognized")
        }
    }

    override fun onError(errorMessage: String) {
        resultTextView.text = errorMessage
        ButtonStyleUtils.setStartStyle(this, yourButton, card1)
        listeningDone()
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }
}
