package com.uptsu.fhr

import SpeechRecognitionManager
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.TypedValue
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
import com.uptsu.fhr.OfflineDataUtils.saveDataOffline
import com.uptsu.fhr.OfflineDataUtils.uploadOfflineData
import com.uptsu.fhr.PendingPopupUtils.updateOfflineCount
import com.uptsu.fhr.PendingPopupUtils.updatePendingPopupVisibility
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jjoe64.graphview.GraphView
import okhttp3.OkHttpClient
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

    private var textViewData: MutableMap<String, Any> = mutableMapOf() // Declare at class level


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
                // ✅ Corrected Function Call (Passing id, idType, pname, mobile)
                val (updatedTextViewData, tableData) = DataPreparationUtils.prepareDataForUpload(
                    id = textViewData["id"]?.toString() ?: "", // ✅ Changed to use textViewData["id"]
                    idType = textViewData["id type"]?.toString() ?: "Unknown",  // Fetch ID type
                    pname = textViewData["pname"]?.toString() ?: "",  // Fetch pname
                    mobile = textViewData["mobile"]?.toString() ?: "", // Fetch mobile number
                    dilation = dilation.text.toString(),
                    clockText = clockTextView.text.toString(),
                    speechLogTable = speechLogTable,
                    existingTextViewData = textViewData // ✅ Pass existing textViewData
                )


                // Save data to the server
                apiHelper.saveOrUpdateData(patientId, textViewData, tableData) { success, id, errorMessage ->
                    runOnUiThread {
                        if (success) {
                            if (patientId == null) {
                                patientId = id
                            }
                            Toast.makeText(this@MainActivity, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            val errorMsg = errorMessage ?: "Unknown Error"
                            val toastMessage = "❌ Error: $errorMsg"
                            Log.e("FinishButton", toastMessage) // ✅ Log the error too
                            Toast.makeText(this@MainActivity, toastMessage, Toast.LENGTH_LONG).show() // ✅ Show error in Toast
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

        userNameDialog = UserNameDialog(this, { name, number, idType, patientName, mobileNo, buttonClicked, userData ->
            updateUI(userData, number)


            // Use userData directly
            val (updatedTextViewData, tableData) = DataPreparationUtils.prepareDataForUpload(
                id = userData["id"].toString(), // ✅ Changed to use userData["id"]
                idType = userData["id type"].toString(),
                pname = userData["pname"].toString(),
                mobile = userData["mobile"].toString(),
                dilation = userData["dilation"].toString(),
                clockText = clockTextView.text.toString(),
                speechLogTable = speechLogTable,
                existingTextViewData = userData
            )

            textViewData = updatedTextViewData.toMutableMap()

            apiHelper.saveOrUpdateData(patientId, updatedTextViewData, tableData) { success, id, errorMessage ->
                runOnUiThread {
                    if (success) {
                        patientId = id
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "❌ Error: ${errorMessage ?: "Unknown"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }, {})


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
            if (isFirstClick) {
                showUserNameDialog("yourButton")
                isFirstClick = false
            } else {
                clockManager.startClock()
                checkAndRequestPermissions()

                // Enable background listening
                getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("shouldStartListening", true)
                    .apply()

                // Start the listening service
                val intent = Intent(this, ListeningService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }


        editButton.setOnClickListener {
            showUserNameDialog("editButton")
            isFirstClick = false
        }

        finishButton.setOnClickListener {
            Log.d("FinishButton", "✅ Finish button clicked.")

            AppUtils.showConfirmationDialog(
                this, pdfUtils, clockTextView, graph, speechLogTable, userName, userNumber, apiHelper, textViewData
            ) { deliveryType, motherCondition, childCondition ->

                Log.d("FinishButton", "✅ Confirmation dialog confirmed.")

                val (updatedTextViewData, tableDataJson) = DataPreparationUtils.prepareDataForUpload(
                    id = textViewData["id"]?.toString() ?: "", // ✅ Changed to use textViewData["id"]
                    idType = textViewData["id type"]?.toString() ?: "Unknown",  // Fetch ID type
                    pname = textViewData["pname"]?.toString() ?: "",  // Fetch pname
                    mobile = textViewData["mobile"]?.toString() ?: "", // Fetch mobile number
                    dilation = dilation.text.toString(),
                    clockText = clockTextView.text.toString(),
                    speechLogTable = speechLogTable,
                    existingTextViewData = textViewData // ✅ Pass existing textViewData
                )

                val tableDataList: MutableList<MutableMap<String, String>> = Gson().fromJson(
                    tableDataJson, object : TypeToken<MutableList<MutableMap<String, String>>>() {}.type
                ) ?: mutableListOf()

                if (tableDataList.isNotEmpty()) {
                    val lastRow = tableDataList.last()
                    lastRow["DeliveryType"] = deliveryType
                    lastRow["MotherCondition"] = motherCondition
                    lastRow["ChildCondition"] = childCondition
                }

                val updatedTableData = Gson().toJson(tableDataList)

                Log.d("FinishButton", "✅ Final Table Data with Dropdown Values: $updatedTableData")

                apiHelper.saveOrUpdateData(patientId, updatedTextViewData, updatedTableData) { success, id, errorMessage ->
                    runOnUiThread {
                        Log.d("FinishButton", "✅ API Call Response: Success = $success, ID = $id")

                        if (success) {
                            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                            if (patientId == null) {
                                patientId = id
                            }
                            resetApp()
                        } else {
                            val errorMsg = errorMessage ?: "Unknown Error" // ✅ Use actual error message
                            Toast.makeText(this, "Failed to save data. Error: $errorMsg", Toast.LENGTH_LONG).show()
                            saveDataOffline(this, updatedTextViewData, updatedTableData, patientId)
                            resetApp()
                        }
                    }
                }
            }
        }





    }


    private fun playAlertSound(fileName: String) {
        // Remove the file extension if present (e.g., ".mp3")
        val resourceName = fileName.substringBeforeLast(".")
        // Retrieve the resource ID dynamically from the raw folder
        val resourceId = resources.getIdentifier(resourceName, "raw", packageName)

        if (resourceId != 0) {
            // Create and start the MediaPlayer
            val alertPlayer = MediaPlayer.create(this, resourceId)
            alertPlayer.start()

            // Release the MediaPlayer once the audio has finished playing
            alertPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
        } else {
            Log.e("MainActivity", "Sound file $fileName not found in raw resources.")
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
    private fun updateUI(userData: Map<String, Any>, number: Int) {
        pname.text = "Name: ${userData["pname"].toString()}"
        dilation.text = "|    Dilatation: $number cm"
        userName = userData["pname"].toString()
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
        mediaPlayerManager.releaseMediaPlayer()

        AppUtils.resetApp(
            this,
            clockTextView,
            graph,
            pname,
            dilation,
            speechLogTable,
            resultTextView,
            mediaPlayerManager,
            clockManager,
        ) { newGraphInitializer, newClockManager ->
            graphInitializer = newGraphInitializer
            clockManager = newClockManager
        }

        // Stop background listening when Finish is clicked
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("shouldStartListening", false)
            .apply()

        // Stop the service explicitly
        val intent = Intent(this, ListeningService::class.java)
        stopService(intent)
    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkChangeReceiver)
        unregisterReceiver(uiUpdateReceiver)
        unregisterReceiver(listeningReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiUpdateReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listeningReceiver)
        mediaPlayerManager.releaseMediaPlayer()
        clockManager.stopClock()
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

            // Check if the number is out of range.
            if (currentValue > 160 ) {
                playAlertSound("fhr_up.mp3")
            }
            if (currentValue < 120) {
                playAlertSound("fhr_down.mp3")
            }



            // Prepare data for upload
            // Prepare data for upload, keeping existing user details
            val (updatedTextViewData, tableData) = DataPreparationUtils.prepareDataForUpload(
                id = textViewData["id"]?.toString() ?: "", // ✅ Changed to use textViewData["id"]
                idType = textViewData["id type"]?.toString() ?: "Unknown",  // Fetch ID type
                pname = textViewData["pname"]?.toString() ?: "",  // Fetch pname
                mobile = textViewData["mobile"]?.toString() ?: "", // Fetch mobile number
                dilation = dilation.text.toString(),
                clockText = clockTextView.text.toString(),
                speechLogTable = speechLogTable,
                existingTextViewData = textViewData // ✅ Pass existing textViewData
            )

// Update `textViewData` to persist details
            textViewData = updatedTextViewData.toMutableMap()

// Save data to the server
            apiHelper.saveOrUpdateData(patientId, textViewData, tableData) { success, id, errorMessage ->
                runOnUiThread {
                    if (success) {
                        if (patientId == null) {
                            patientId = id
                        }
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = errorMessage ?: "Unknown Error"
                        val toastMessage = "❌ Error: $errorMsg"
                        Log.e("FinishButton", toastMessage) // ✅ Log the error too
                        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show() // ✅ Show error in Toast

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
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT")) // Get current time
        return calendar.timeInMillis + (5 * 60 * 60 * 1000L) + (30 * 60 * 1000L)
    }

    private fun convertTime(milliseconds: Long): String {
        val sdf = SimpleDateFormat("HH:mm a", Locale.getDefault()) // Include AM/PM
        sdf.timeZone = TimeZone.getTimeZone("IST") // Set timezone to IST
        return sdf.format(milliseconds) // Format the milliseconds
    }


}
