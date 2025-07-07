package com.uptsu.fhr

// ClockManager.kt

import android.os.Handler
import android.os.Looper
import android.widget.TextView


class ClockManager(private val clockTextView: TextView) {

    private var secondsCount: Long = 0
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable: Runnable
    private var isRunning = false // Track if the clock is running

    init {
        clockRunnable = object : Runnable {
            override fun run() {
                // Increment the seconds count
                secondsCount++

                // Update the clock UI
                updateClock()

                // Schedule the next update after 1000 milliseconds (1 second)
                clockHandler.postDelayed(this, 1000)
            }
        }
    }

    fun startClock() {
        if (!isRunning) { // Only start if not already running
            isRunning = true
            clockHandler.post(clockRunnable) // Start the runnable
        }
    }

    fun stopClock() {
        if (isRunning) { // Only stop if currently running
            isRunning = false
            clockHandler.removeCallbacks(clockRunnable) // Stop the runnable
        }
    }

    private fun updateClock() {
        // Calculate hours, minutes, and seconds
        val hours = (secondsCount / 3600) % 24 // Wrap around after 24 hours
        val minutes = (secondsCount % 3600) / 60
        val seconds = secondsCount % 60

        // Format the time
        val formattedTime = String.format("%02d : %02d : %02d", hours, minutes, seconds)

        // Update the TextView with the formatted time
        clockTextView.text = formattedTime
    }
}


