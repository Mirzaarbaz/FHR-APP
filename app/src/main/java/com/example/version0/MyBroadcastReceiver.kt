package com.example.version0

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle the received broadcast
        val message = intent.getStringExtra("message")
        Log.d("MyBroadcastReceiver", "Received broadcast with message: $message")
    }
}
