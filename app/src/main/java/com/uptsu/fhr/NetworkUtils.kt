package com.uptsu.fhr

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

    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    fun showInternetRequiredDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Internet Required for Hindi Voice")
        builder.setMessage("Hindi voice recognition requires internet connection. Please enable it in your settings.")
        builder.setPositiveButton("Open Settings") { _, _ ->
            // Open network settings
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            context.startActivity(intent)
        }
        builder.setNegativeButton("Continue with English") { dialog, _ ->
            networkActionListener?.onNetworkActionCancelled()
            dialog.dismiss()
        }
        builder.show()
    }
}


