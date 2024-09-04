package com.example.version0

import SpeechRecognitionManager
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.version0.OfflineDataUtils.saveDataOffline
import com.example.version0.OfflineDataUtils.uploadOfflineData
import com.example.version0.PendingPopupUtils.updateOfflineCount
import com.example.version0.PendingPopupUtils.updatePendingPopupVisibility
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jjoe64.graphview.GraphView
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, ResultListener, NetworkUtils.NetworkActionListener {

    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var userNameDialog: UserNameDialog
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var apiHelper: ApiHelper

    private lateinit var progressBar: ProgressBar
    private lateinit var pdfUtils: PDFUtils
    private lateinit var finishButton: Button
    lateinit var startButton: Button
    private lateinit var shareButton: ImageButton
    private lateinit var editButton: ImageButton
    lateinit var card1: LinearLayout

    private lateinit var resultTextView: TextView
    private lateinit var clockTextView: TextView
    lateinit var pname: TextView
    lateinit var dilation: TextView
    private lateinit var graph: GraphView
    private lateinit var speechLogTable: TableLayout
    private lateinit var clockManager: ClockManager
    private lateinit var graphInitializer: GraphInitializer

    private var speechCounter: Int = 1
    private var userName: String? = null
    private var userNumber: Int = 0
    var isFirstClick = true

    private var patientId: Int? = null

    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var pendingPopup: LinearLayout
    private lateinit var handler: Handler


    private lateinit var mediaPlayerManager: MediaPlayerManager


    private val listeningReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "LISTENING_STARTED" -> {
                    ButtonStyleUtils.setListeningStyle(this@MainActivity, startButton, card1)
                    resultTextView.text="000"
                    adjustTextSizeToFit(resultTextView)

                }
                "LISTENING_STOPPED" -> {
                    ButtonStyleUtils.setStartStyle(this@MainActivity, startButton, card1)
                }
            }
        }
    }

    private val uiUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val numbers = intent?.getIntegerArrayListExtra("numbers")

            if (numbers != null && numbers.isNotEmpty()) {
                val currentValue = numbers[0]

                // Update graph and result text view
                graphInitializer.addDataPoint(currentValue.toDouble())
                resultTextView.text = currentValue.toString()
                adjustTextSizeToFit(resultTextView)

                // Prepare formatted time
                val formattedTime = "${clockTextView.text} (${convertTime(getCurrentTime())})"

                // Update table with new data
                TableUtils.addDataToTable(
                    this@MainActivity,
                    speechLogTable,
                    speechCounter,
                    numbers,
                    formattedTime
                )
                speechCounter++

                // Optionally update UI elements (Uncomment if needed)
                // ButtonStyleUtils.setStartStyle(this@MainActivity, startButton, card1)
                // listeningDone()

                // Prepare data for upload
                val (textViewData, tableData) = DataPreparationUtils.prepareDataForUpload(
                    pname.text.toString(),
                    dilation.text.toString(),
                    clockTextView.text.toString(),
                    speechLogTable
                )

                // Save data to the server
                apiHelper.saveOrUpdateData(patientId, textViewData, tableData) { success, id ->
                    runOnUiThread {
                        if (success) {
                            if (patientId == null) {
                                patientId = id
                            }
                            Toast.makeText(this@MainActivity, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to save data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                onError("No number recognized")
            }
        }
    }


    @SuppressLint("MissingInflatedId", "InlinedApi")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        clockTextView = findViewById(R.id.clockTextView)
        pname = findViewById(R.id.name)
        dilation = findViewById(R.id.dia)
        graph = findViewById(R.id.graph)
        speechLogTable = findViewById(R.id.speechLogTable)
        startButton = findViewById(R.id.yourButton)
        editButton = findViewById(R.id.edit)
        shareButton = findViewById(R.id.shareButton)
        card1 = findViewById(R.id.card1)
        resultTextView = findViewById(R.id.resultTextView)
        finishButton = findViewById(R.id.finish)
        progressBar = findViewById(R.id.progressBar)
        pendingPopup = findViewById(R.id.pending_popup)

        // Initialize ApiHelper
        val client = OkHttpClient()
        apiHelper = ApiHelper(client)

        // Initialize components
        graphInitializer = GraphInitializer(this, graph)
        clockManager = ClockManager(clockTextView)

        // Set the NetworkActionListener to this Activity
        NetworkUtils.setNetworkActionListener(this)

        // Initialize SpeechRecognitionManager
        speechRecognitionManager = SpeechRecognitionManager(this)
        speechRecognitionManager.setResultListener(this)


        // Register the receiver to listen for broadcasts
        val listeningFilter = IntentFilter().apply {
            addAction("LISTENING_STARTED")
            addAction("LISTENING_STOPPED")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(listeningReceiver, listeningFilter)

        val UIFilter = IntentFilter().apply {
            addAction("UPDATE_UI") // Use the appropriate action string
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(uiUpdateReceiver, UIFilter)

        // Initialize MediaPlayerManager
        mediaPlayerManager = MediaPlayerManager(this)
        mediaPlayerManager.initializeMediaPlayer()


        // Initialize the network change receiver
        networkChangeReceiver = NetworkChangeReceiver {
            uploadOfflineData(this,apiHelper)
            updateOfflineCount(this)
            updatePendingPopupVisibility(this,pendingPopup)  // Update visibility on network change
        }

        // Register the receiver for network changes
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, intentFilter)

        // Call the function to update visibility based on the current connection status
        updatePendingPopupVisibility(this,pendingPopup)

        // Initialize UserNameDialog with a callback to update UI and determine which button was clicked
        userNameDialog = UserNameDialog(this, { name, number, buttonClicked ->
            updateUI(name, number)
            if (buttonClicked == "yourButton") {
                clockManager.startClock()
                checkAndRequestPermissions()
            }
        }, {
            // Prepare data for upload
            val (textViewData, tableData) = DataPreparationUtils.prepareDataForUpload(
                pname.text.toString(),
                dilation.text.toString(),
                clockTextView.text.toString(),
                speechLogTable
            )

            // Save data to the server
            apiHelper.saveOrUpdateData(patientId, textViewData, tableData) { success, id ->
                runOnUiThread {
                    if (success) {
                        if (patientId == null) {
                            patientId = id
                        }
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        // Initialize PDFUtils
        pdfUtils = PDFUtils(this)

        // Setup PDF sharing with a 5-second loader
        shareButton.setOnClickListener {
            AppUtils.showLoaderAndSharePDF(
                this, pdfUtils, clockTextView,
                graph, speechLogTable, userName, userNumber
            )
        }

        startButton.setOnClickListener {
//            if (isFirstClick) {
//                showUserNameDialog("yourButton")
//                isFirstClick = false
//            } else {
                clockManager.startClock()
                checkAndRequestPermissions()
                // Start the listening service
                startListeningService()
//            }
        }

        editButton.setOnClickListener {
            showUserNameDialog("editButton")
            isFirstClick = false
        }

        finishButton.setOnClickListener {
            AppUtils.showConfirmationDialog(
                this, pdfUtils, clockTextView, graph, speechLogTable, userName, userNumber
            ) {
                val (textViewData, tableData) = DataPreparationUtils.prepareDataForUpload(
                    pname.text.toString(),
                    dilation.text.toString(),
                    clockTextView.text.toString(),
                    speechLogTable
                )

                apiHelper.saveOrUpdateData(patientId, textViewData, tableData) { success, id ->
                    runOnUiThread {
                        if (success) {
                            if (patientId == null) {
                                patientId = id
                            }
                            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                            resetApp()
                        } else {
                            // Append new data to the list in shared preferences
                            saveDataOffline(this, textViewData, tableData, patientId)
                            resetApp()
                        }
                    }
                }
            }
        }
    }

    private fun adjustTextSizeToFit(textView: TextView) {
        val paint = Paint().apply {
            textSize = textView.textSize
        }

        val text = textView.text.toString()
        val textWidth = paint.measureText(text)
        val viewWidth = textView.width - textView.paddingLeft - textView.paddingRight

        if (textWidth > viewWidth) {
            val newTextSize = textView.textSize * (viewWidth / textWidth)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize)
        }
    }


    private fun startListeningService() {
        val intent = Intent(this, ListeningService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopListeningService() {
        val intent = Intent(this, ListeningService::class.java)
        stopService(intent)
    }

    override fun onNetworkActionCancelled() {
        // Handle network action cancellation (e.g., restart listening)
        startListening()
    }

    private fun showUserNameDialog(triggeringButton: String) {
        val nameWithoutPrefix = pname.text.toString().removePrefix("Name: ").trim()
        val dilationValue = DataPreparationUtils.extractDilationNumber(dilation.text.toString())
        userNameDialog.show(
            triggeringButton,
            existingName = nameWithoutPrefix.takeIf { it.isNotBlank() },
            existingDilation = dilationValue.toString().toIntOrNull()
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(name: String, number: Int) {
        pname.text = "Name: $name"
        dilation.text = "|    Dilation: $number cm"
        userName = name
        userNumber = number
        patientId = null // Reset patientId for a new patient
    }

    private fun startListening() {
        mediaPlayerManager.startListening {
            ButtonStyleUtils.setListeningStyle(this@MainActivity, startButton, card1)
            speechRecognitionManager.startSpeechRecognition()
        }
    }


    private fun resetApp() {
        isFirstClick = true
        speechCounter = 1
        stopListeningService()
        AppUtils.resetApp(
            this,
            clockTextView,
            graph,
            pname,
            dilation,
            speechLogTable,
            resultTextView,
            mediaPlayer,
            clockManager,
        ) { newGraphInitializer, newClockManager ->
            graphInitializer = newGraphInitializer
            clockManager = newClockManager
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkChangeReceiver)
        unregisterReceiver(uiUpdateReceiver)
        unregisterReceiver(listeningReceiver)
        mediaPlayerManager.releaseMediaPlayer()
        clockManager.stopClock()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release() // Release MediaPlayer resources
        }
        stopListeningService()
    }

    private fun checkAndRequestPermissions() {
        // Check internet connection and show dialog if needed
        if (NetworkUtils.isInternetConnected(this)) {
            // Internet is connected, proceed with operations
            updatePendingPopupVisibility(this,pendingPopup)
            if (PermissionUtils.hasMicrophonePermission(this)) {
                startListening()
            } else {
                PermissionUtils.requestMicrophonePermission(this)
            }

        } else {
            // Internet is not connected, show dialog to user
            NetworkUtils.showInternetRequiredDialog(this)
        }
        if (PermissionUtils.hasNotificationPermission(this)) {
            // NotificationUtils.showNotification(this)
        } else {
            PermissionUtils.requestNotificationPermission(this)
        }
    }

    private fun showNotification() {
        NotificationUtils.showNotification(this)
    }

    private fun listeningDone() {
        NotificationUtils.cancelNotification(this)
    }

    override fun onInit(status: Int) {
    }

    override fun onResult(numbers: List<Int>) {
        if (numbers.isNotEmpty()) {
            val currentValue = numbers[0]
            graphInitializer.addDataPoint(currentValue.toDouble())
            resultTextView.text = currentValue.toString()
            adjustTextSizeToFit(resultTextView)

            val formattedTime = "${clockTextView.text} (${convertTime(getCurrentTime())})"
            TableUtils.addDataToTable(
                this,
                speechLogTable,
                speechCounter,
                numbers,
                formattedTime
            )
            speechCounter++

            ButtonStyleUtils.setStartStyle(this, startButton, card1)
            listeningDone()

            // Prepare data for upload
            val (textViewData, tableData) = DataPreparationUtils.prepareDataForUpload(
                pname.text.toString(),
                dilation.text.toString(),
                clockTextView.text.toString(),
                speechLogTable
            )

            // Save data to the server
            apiHelper.saveOrUpdateData(patientId, textViewData, tableData) { success, id ->
                runOnUiThread {
                    if (success) {
                        if (patientId == null) {
                            patientId = id
                        }
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            onError("000")
        }
    }

    override fun onError(errorMessage: String) {
        resultTextView.text = errorMessage
        ButtonStyleUtils.setStartStyle(this, startButton, card1)
        listeningDone()
    }

    private fun getCurrentTime(): Long {
        return Calendar.getInstance().timeInMillis
    }

    private fun convertTime(milliseconds: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(milliseconds)
    }


}
