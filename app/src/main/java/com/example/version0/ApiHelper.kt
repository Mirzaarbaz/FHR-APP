package com.example.version0


// File: ApiHelper.kt

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ApiHelper(private val client: OkHttpClient) {

    private val mediaTypeJson = "application/json".toMediaType()

    fun saveOrUpdateData(patientId: Int?, textViewData: Map<String, Any>, tableData: String, callback: (Boolean, Int?) -> Unit) {
        val jsonBody = JSONObject().apply {
            put("textViews", JSONObject(textViewData))
            put("tableData", tableData)
            patientId?.let { put("patientId", it) }
        }

        val requestBody = jsonBody.toString().toRequestBody(mediaTypeJson)

        val request = Request.Builder()
            .url("http://3.7.253.3:3000/save-or-update-data")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false, null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val responseJson = JSONObject(responseBody ?: "")
                    val id = responseJson.optInt("insertId", -1)
                    callback(true, if (id != -1) id else null)
                } else {
                    callback(false, null)
                }
            }
        })
    }
}
