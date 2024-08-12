package com.example.version0


import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

object NetworkUtils {

    interface NetworkActionListener {
        fun onNetworkActionCancelled()
    }

    private var networkActionListener: NetworkActionListener? = null

    fun setNetworkActionListener(listener: NetworkActionListener) {
        networkActionListener = listener
    }

    // Network Utility
    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            networkInfo.isConnected
        }
    }

    // Dialog Utility
    fun showInternetRequiredDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Internet Required")
        builder.setMessage("Hindi voice recognition requires internet connection. Please enable it in your settings.")
        builder.setPositiveButton("Open Settings") { _, _ ->
            // Open network settings
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            context.startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            networkActionListener?.onNetworkActionCancelled()
            dialog.dismiss()
        }
        builder.show()
    }
}
