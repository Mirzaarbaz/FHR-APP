package com.example.version0

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView


class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var textToSpeech: TextToSpeech

    private lateinit var yourButton: Button
    private lateinit var shareButton: ImageButton
    private lateinit var card1: LinearLayout

    private lateinit var resultTextView: TextView



    private lateinit var clockTextView: TextView
    private lateinit var graph: GraphView
    private lateinit var speechLogTable: TableLayout
    private lateinit var clockManager: ClockManager

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        clockTextView = findViewById(R.id.clockTextView)
        graph = findViewById(R.id.graph)
        speechLogTable = findViewById(R.id.speechLogTable)

        // Initialize GraphView
        GraphInitializer(graph)


        // Setup PDF sharing
        val pdfUtils = PDFUtils(this)
        shareButton = findViewById(R.id.shareButton)
        shareButton.setOnClickListener {
            pdfUtils.sharePDF(clockTextView, graph, speechLogTable)
        }
//
//        // Initialize ClockManager
//        clockManager = ClockManager(clockTextView)
//        clockManager.startClock()





//        textToSpeech = TextToSpeech(this, this) // Initialize textToSpeech here
//
//        // Initialize SpeechRecognitionManager
//        speechRecognitionManager = SpeechRecognitionManager(this)
//
//

        yourButton = findViewById(R.id.yourButton)
//        card1 = findViewById(R.id.card1)
//        resultTextView = findViewById(R.id.resultTextView)
//

        yourButton.setOnClickListener {

            // Initialize ClockManager
            clockManager = ClockManager(clockTextView)
            clockManager.startClock()

            // Check and request permissions
            checkAndRequestPermissions()
        }
//


    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the clock when the activity is destroyed
        clockManager.stopClock()
    }

    private fun checkAndRequestPermissions() {
        if (PermissionUtils.hasNotificationPermission(this)) {
            NotificationUtils.showNotification(this)
        } else {
            PermissionUtils.requestNotificationPermission(this)
        }

        if (PermissionUtils.hasMicrophonePermission(this)) {
            startListening()
        } else {
            PermissionUtils.requestMicrophonePermission(this)
        }
    }

    private fun startListening() {
        // Implement your start listening logic here
    }



    // Call this function when listening is done to cancel the notification
    private fun listeningDone() {
        NotificationUtils.cancelNotification(this)
    }

}

