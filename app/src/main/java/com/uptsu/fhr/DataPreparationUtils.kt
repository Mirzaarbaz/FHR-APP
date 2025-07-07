// File: DataPreparationUtils.kt
package com.uptsu.fhr

import android.util.Log
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

import java.util.Date
import java.util.Locale

object DataPreparationUtils {

    fun prepareDataForUpload(
        id: String,  // ✅ Corrected to ID
        idType: String,  // ✅ Corrected to ID Type
        pname: String,
        mobile: String,
        dilation: String,
        clockText: String,
        speechLogTable: TableLayout,
        existingTextViewData: Map<String, Any>? // Preserve existing data
    ): Pair<Map<String, Any>, String> {

        val textViewData = existingTextViewData?.toMutableMap() ?: mutableMapOf()

        // Add current date and time
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDateTime = sdf.format(Date())
        textViewData["date_time"] = currentDateTime

        // ✅ Corrected field names to match required JSON output
        textViewData["id type"] = idType
        textViewData["id"] = id
        textViewData["pname"] = pname
        textViewData["mobile"] = mobile
        textViewData["dilation"] = extractDilationNumber(dilation)
        textViewData["total_time"] = clockText

        // ✅ Remove `age`, `patientName`, and `mobileNo`
        textViewData.remove("age")
        textViewData.remove("patientName")
        textViewData.remove("mobileNo")

        // ✅ Extract table data
        val tableData = extractTableData(speechLogTable, textViewData)

        return Pair(textViewData, tableData)
    }

    fun extractDilationNumber(dilationText: String): Int {
        val regex = Regex("\\d+")
        val matchResult = regex.find(dilationText)
        return matchResult?.value?.toInt() ?: 0
    }

    private fun extractTableData(speechLogTable: TableLayout, textViewData: Map<String, Any>): String {
        val tableData = mutableListOf<MutableMap<String, String>>()

        val rowCount = speechLogTable.childCount
        if (rowCount < 2) return Gson().toJson(tableData) // Ensure at least one data row

        // Extract column headers
        val headerRow = speechLogTable.getChildAt(0) as TableRow
        val headers = mutableListOf<String>()
        for (i in 0 until headerRow.childCount) {
            val headerTextView = headerRow.getChildAt(i) as? TextView
            headers.add(headerTextView?.text?.toString() ?: "Column$i")
        }

        // Iterate over rows (excluding the header)
        for (i in 1 until rowCount) {
            val row = speechLogTable.getChildAt(i) as? TableRow ?: continue
            val rowData = mutableMapOf<String, String>()

            for (j in 0 until row.childCount) {
                val cellTextView = row.getChildAt(j) as? TextView ?: continue
                rowData[headers[j]] = cellTextView.text.toString()
            }

            tableData.add(rowData)
        }

        // ✅ Ensure the last row contains actual values from `textViewData`
        if (tableData.isNotEmpty()) {
            val lastRow = tableData.last()
            lastRow["DeliveryType"] = textViewData["DeliveryType"]?.toString() ?: "Not Provided"
            lastRow["MotherCondition"] = textViewData["MotherCondition"]?.toString() ?: "Not Provided"
            lastRow["ChildCondition"] = textViewData["ChildCondition"]?.toString() ?: "Not Provided"
        }

        val finalData = Gson().toJson(tableData)
        Log.d("DataPreparation", "✅ Final Table Data with Dropdown Values: $finalData")
        return finalData
    }

    private fun getCurrentTimeInIST(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.timeZone = calendar.timeZone
        return format.format(calendar.time)
    }
}
