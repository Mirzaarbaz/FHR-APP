// OfflineDataUtils.kt
package com.uptsu.fhr

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OfflineDataUtils {

    fun saveDataOffline(context: Context, textViewData: Map<String, Any>, tableData: String, patientId: Int?) {
        val sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val offlineDataList = mutableListOf<Map<String, Any>>()
        val existingDataJson = sharedPreferences.getString("offline_data_list", null)
        if (existingDataJson != null) {
            val type = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
            val existingData = Gson().fromJson<MutableList<Map<String, Any>>>(existingDataJson, type)
            offlineDataList.addAll(existingData)
        }

        val dataToSave = mutableMapOf<String, Any>().apply {
            put("textViewData", textViewData)
            put("tableData", tableData)
            put("patientId", patientId?.toString() ?: "-1")
            put("status", "pending")
        }
        offlineDataList.add(dataToSave)

        val updatedDataJson = Gson().toJson(offlineDataList)
        editor.putString("offline_data_list", updatedDataJson)
        editor.apply()

        PendingPopupUtils.updateOfflineCount(context)
    }

    fun getLatestSavedData(context: Context): Map<String, Any> {
        val sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE)
        val offlineDataJson = sharedPreferences.getString("offline_data_list", null) ?: return emptyMap()

        val type = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
        val offlineDataList: MutableList<Map<String, Any>> = Gson().fromJson(offlineDataJson, type)

        val latestEntry = offlineDataList.lastOrNull() ?: return emptyMap()

        // ✅ Ensure we retrieve the **correct patient ID**
        val patientId = latestEntry["patientId"] as? Int ?: -1
        return latestEntry.toMutableMap().apply { put("patientId", patientId) }
    }


    fun uploadOfflineData(context: Context, apiHelper: ApiHelper) {
        val sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE)
        val offlineDataJson = sharedPreferences.getString("offline_data_list", null)

        if (offlineDataJson != null) {
            val type = object : TypeToken<MutableList<MutableMap<String, Any>>>() {}.type
            val offlineDataList = Gson().fromJson<MutableList<MutableMap<String, Any>>>(offlineDataJson, type)
            val editor = sharedPreferences.edit()

            for (data in offlineDataList) {
                val status = data["status"] as? String
                if (status == "pending") {
                    val textViewData = data["textViewData"] as Map<String, Any>
                    val tableData = data["tableData"] as String
                    val patientIdString = data["patientId"] as? String

                    val patientId: Int? = patientIdString?.toIntOrNull()

                    if (patientId != null && patientId != -1) {
                        apiHelper.saveOrUpdateData(patientId, textViewData, tableData) { success, id, errorMessage  ->
                            (context as AppCompatActivity).runOnUiThread {
                                if (success) {
                                    offlineDataList.remove(data)
                                    val updatedDataJson = Gson().toJson(offlineDataList)
                                    editor.putString("offline_data_list", updatedDataJson)
                                    editor.apply()
                                }
                            }
                        }
                    } else {
                        apiHelper.saveOrUpdateData(null, textViewData, tableData) { success, newId, errorMessage  ->
                            (context as AppCompatActivity).runOnUiThread {
                                if (success) {
                                    if (newId != null) {
                                        data["patientId"] = newId.toString()
                                        val updatedDataJson = Gson().toJson(offlineDataList)
                                        editor.putString("offline_data_list", updatedDataJson)
                                        editor.apply()
                                    }
                                }
                            }
                        }
                    }
                }
                PendingPopupUtils.updateOfflineCount(context)
            }
        }
    }
}
