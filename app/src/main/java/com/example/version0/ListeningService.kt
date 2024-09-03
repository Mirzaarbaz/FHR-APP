package com.example.version0

import SpeechRecognitionManager
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class ListeningService : Service(), ResultListener {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var handler: Handler
    private val interval: Long = 30000 // 60 seconds

    override fun onCreate() {
        super.onCreate()

        // Initialize SpeechRecognitionManager
        speechRecognitionManager = SpeechRecognitionManager(this)
        speechRecognitionManager.setResultListener(this)

        // Initialize handler for periodic execution
        handler = Handler(Looper.getMainLooper())

        // Start periodic task
        handler.postDelayed(runnableTask, interval)

        // Start the service as a foreground service
//        startForegroundService()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start periodic task
        handler.postDelayed(runnableTask, interval)

        // Start the service as a foreground service
        startForegroundService()
        return START_STICKY
    }

    // This method will handle stopping the service
    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    private val runnableTask = object : Runnable {
        override fun run() {
            startListening()
            handler.postDelayed(this, interval) // Re-run the task after 60 seconds
        }
    }

    private fun startListening() {
        // Ensure MediaPlayer is initialized and start it every cycle
        if (!::mediaPlayer.isInitialized) {
            mediaPlayer = MediaPlayer.create(this, R.raw.fhr)
            mediaPlayer.setOnCompletionListener {
                startSpeechRecognition()
            }
        }

        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        } else {

            handler.postDelayed({
                startSpeechRecognition()
            }, 2500) // 2.5 seconds delay

        }
    }

    private fun startSpeechRecognition() {
        speechRecognitionManager.retryCount=0
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
        handler.removeCallbacks(runnableTask)
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }

    override fun onResult(numbers: List<Int>) {
        // Pass the recognized numbers back to the UI using a broadcast or any other method
        val intent = Intent("UPDATE_UI")
        intent.putExtra("result", numbers[0].toString())
        sendBroadcast(intent)
    }

    override fun onError(errorMessage: String) {
        // If there's an error, simply log or handle it, but continue the cycles
    }
}
