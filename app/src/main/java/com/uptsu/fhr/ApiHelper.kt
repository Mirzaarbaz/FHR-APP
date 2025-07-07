package com.uptsu.fhr

import android.util.Log


// File: ApiHelper.kt


import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ApiHelper(private val client: OkHttpClient) {

    companion object {
        private const val BASE_URL = "http://35.154.164.72:3000"
        private const val ENDPOINT = "/save-or-update-data"
        private const val TAG = "ApiHelper"
    }

    private val mediaTypeJson = "application/json".toMediaType()

    fun saveOrUpdateData(
        patientId: Int?,
        textViewData: Map<String, Any>,
        tableData: String,
        callback: (Boolean, Int?, String?) -> Unit // ✅ Now includes errorMessage
    ) {
        val jsonBody = JSONObject().apply {
            put("textViewData", JSONObject(textViewData))
            put("tableData", tableData)
            patientId?.let { put("patientId", it) }
        }

        Log.d("ApiHelper", "✅ API Request Prepared: $jsonBody")

        val requestBody = jsonBody.toString().toRequestBody(mediaTypeJson)

        val request = Request.Builder()
            .url("http://35.154.164.72:3000/save-or-update-data")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ApiHelper", "❌ API Call Failed: ${e.message}")
                callback(false, null, e.message) // ✅ Pass error message
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()
                    Log.d("ApiHelper", "✅ API Response: $responseBody")

                    if (response.isSuccessful) {
                        val responseJson = JSONObject(responseBody ?: "{}")
                        val id = responseJson.optInt("insertId", -1)
                        callback(true, if (patientId != null) patientId else id.takeIf { it != -1 }, null)
                    } else {
                        Log.e("ApiHelper", "❌ API Error: $responseBody")
                        callback(false, null, responseBody) // ✅ Pass API error response
                    }
                }
            }
        })
    }




}
