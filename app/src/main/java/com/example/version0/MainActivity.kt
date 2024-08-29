package com.example.version0

import SpeechRecognitionManager
import android.annotation.SuppressLint
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
    lateinit var yourButton: Button
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


    @SuppressLint("MissingInflatedId")
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
        yourButton = findViewById(R.id.yourButton)
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

        // Initialize MediaPlayer with a prepared resource
        initializeMediaPlayer()

        // Set the NetworkActionListener to this Activity
        NetworkUtils.setNetworkActionListener(this)

        // Initialize SpeechRecognitionManager
        speechRecognitionManager = SpeechRecognitionManager(this)
        speechRecognitionManager.setResultListener(this)



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
//            showNotification()
        }

        yourButton.setOnClickListener {
            if (isFirstClick) {
                showUserNameDialog("yourButton")
                isFirstClick = false
            } else {
                clockManager.startClock()
                checkAndRequestPermissions()
            }
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
//                            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                            // Append new data to the list in shared preferences
                            saveDataOffline(this, textViewData, tableData, patientId)
                            resetApp()
                        }
                    }
                }
            }
        }
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

    private fun updateUI(name: String, number: Int) {
        pname.text = "Name: $name"
        dilation.text = "|    Dilation: $number cm"
        userName = name
        userNumber = number
        patientId = null // Reset patientId for a new patient
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
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

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
        isFirstClick = true
        speechCounter = 1
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
        clockManager.stopClock()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release() // Release MediaPlayer resources
        }
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
            onError("No number recognized")
        }
    }

    override fun onError(errorMessage: String) {
        resultTextView.text = errorMessage
        ButtonStyleUtils.setStartStyle(this, yourButton, card1)
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
