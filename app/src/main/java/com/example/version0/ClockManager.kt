package com.example.version0

// ClockManager.kt

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockManager(private val clockTextView: TextView) {

    private var secondsCount: Long = 0
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable: Runnable

    init {
        clockRunnable = object : Runnable {
            override fun run() {
                // Update the clock UI
                updateClock()

                // Schedule the next update after 1000 milliseconds (1 second)
                clockHandler.postDelayed(this, 1000)
            }
        }
    }

    fun startClock() {
        clockHandler.post(clockRunnable)
    }

    fun stopClock() {
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun updateClock() {
        // Calculate hours, minutes, and seconds
        val hours = secondsCount / 3600
        val minutes = (secondsCount % 3600) / 60
        val seconds = secondsCount % 60

        // Format the time
        val formattedTime = String.format("%02d : %02d : %02d", hours, minutes, seconds)

        // Update the TextView with the formatted time
        clockTextView.text = formattedTime

        // Increment the seconds count
        secondsCount++
    }
}
