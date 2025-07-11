package com.uptsu.fhr

import SpeechRecognitionManager
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ListeningService : Service(), ResultListener {

    private lateinit var handler: Handler
    private val interval: Long = 60000*30 // 30 mins

    private lateinit var mediaPlayerManager: MediaPlayerManager
    private lateinit var speechRecognitionManager: SpeechRecognitionManager

    val UIintent = Intent("UPDATE_UI")

    override fun onCreate() {
        super.onCreate()

        // Initialize SpeechRecognitionManager
        speechRecognitionManager = SpeechRecognitionManager(this)
        speechRecognitionManager.setResultListener(this)

        // Initialize handler for periodic execution
        handler = Handler(Looper.getMainLooper())

        // Start periodic task
        handler.postDelayed(runnableTask, interval)

        // Initialize MediaPlayerManager
        mediaPlayerManager = MediaPlayerManager(this)
        mediaPlayerManager.initializeMediaPlayer()

        // Start the service as a foreground service
        startForegroundService()

        // Start periodic task
        startPeriodicTask()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val shouldStartListening = prefs.getBoolean("shouldStartListening", false)

        if (shouldStartListening) {
            startPeriodicTask() // Only start if listening was enabled
        } else {
            stopForegroundService() // Stop service if not allowed to run
        }

        return START_STICKY
    }


    private fun startPeriodicTask() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val shouldStartListening = prefs.getBoolean("shouldStartListening", false)

        if (shouldStartListening) {
            handler.removeCallbacks(runnableTask) // Remove any previously scheduled tasks
            handler.postDelayed(runnableTask, interval) // Schedule new task
        }
    }


    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }
    private fun cancelHandlerTasks() {
        handler.removeCallbacks(runnableTask)
    }


    private val runnableTask = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val shouldStartListening = prefs.getBoolean("shouldStartListening", false)

            if (shouldStartListening) {
                startListening() // Only start if explicitly allowed
            }

            // Always schedule the task again after interval, but only execute if allowed
            handler.postDelayed(this, interval)
        }
    }


    private fun startListening() {
        mediaPlayerManager.startListening {
            startSpeechRecognition()
        }
    }

    private fun startSpeechRecognition() {
        speechRecognitionManager.retryCount = 0
        speechRecognitionManager.startSpeechRecognition()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val channelId = "ListeningServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Listening Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Listening Service")
            .setContentText("Running background listening...")
            .setSmallIcon(R.drawable.baseline_mic_24)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerManager.releaseMediaPlayer()
        cancelHandlerTasks()
        stopForegroundService()

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val shouldStartListening = prefs.getBoolean("shouldStartListening", false)

        if (shouldStartListening) {
            val restartIntent = Intent(this, ListeningService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        }
    }



    override fun onResult(numbers: List<Int>) {
        if (numbers.isNotEmpty()) {
            // Convert List<Int> to ArrayList<Int>
            val arrayListNumbers = ArrayList(numbers)
            sendBroadcastWithData(arrayListNumbers)
        } else {
            onError("000")
        }
    }


    // Sending broadcast from another component (e.g., ListeningService)
    private fun sendBroadcastWithData(numbers: ArrayList<Int>) {
        val intent = Intent("UPDATE_UI").apply {
            putIntegerArrayListExtra("numbers", numbers)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onError(errorMessage: String) {
        UIintent.putExtra("result", errorMessage)
        sendBroadcast(UIintent)

    }
    override fun onUnbind(intent: Intent?): Boolean {
        stopSelf() // Stop the service if no longer bound
        return super.onUnbind(intent)
    }

}