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

    fun saveData(textViewData: Map<String, Any>, tableData: String, callback: (Boolean) -> Unit) {
        val jsonBody = JSONObject().apply {
            put("textViews", JSONObject(textViewData))
            put("tableData", tableData)
        }

        val requestBody = jsonBody.toString().toRequestBody(mediaTypeJson)

        val request = Request.Builder()
            .url("http://3.7.253.3:3000/save-data")
            .post(requestBody)
            .build()

        Log.d("ApiHelper", "Request URL: ${request.url}")
        Log.d("ApiHelper", "Request Body: ${jsonBody.toString()}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("ApiHelper", "Request failed", e)
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("ApiHelper", "Response Code: ${response.code}")
                Log.d("ApiHelper", "Response Body: ${response.body?.string()}")

                if (response.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        })
    }
}
