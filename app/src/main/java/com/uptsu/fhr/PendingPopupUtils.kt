// PendingPopupUtils.kt
package com.uptsu.fhr

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PendingPopupUtils {

    fun updatePendingPopupVisibility(context: Context, pendingPopup: View) {
        updateOfflineCount(context)
        pendingPopup.visibility = if (NetworkUtils.isInternetConnected(context)) {
            View.GONE  // Hide the pending popup
        } else {
            View.VISIBLE  // Show the pending popup
        }
    }

    fun updateOfflineCount(context: Context) {
        val sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE)
        val offlineDataJson = sharedPreferences.getString("offline_data_list", null)
        val pendingCountTextView: TextView = (context as AppCompatActivity).findViewById(R.id.offline_count)

        val offlineDataList = if (offlineDataJson != null) {
            val type = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
            Gson().fromJson<MutableList<Map<String, Any>>>(offlineDataJson, type)
        } else {
            mutableListOf() // Return an empty list if no data is found
        }

        val pendingCount = offlineDataList.count { (it["status"] as? String) == "pending" }
        pendingCountTextView.text = "Pending Records: $pendingCount"
    }
}
