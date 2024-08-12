// File: DataPreparationUtils.kt
package com.example.version0

import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

object DataPreparationUtils {

    fun prepareDataForUpload(
        pname: String,
        dilation: String,
        clockText: String,
        speechLogTable: TableLayout
    ): Pair<Map<String, Any>, String> {

        val nameWithoutPrefix = pname.removePrefix("Name: ").trim()

        val textViewData: Map<String, Any> = mapOf(
            "name" to nameWithoutPrefix,
            "age" to 0,  // default age for now
            "date_time" to getCurrentTimeInIST(),
            "dilation" to extractDilationNumber(dilation),
            "total_time" to clockText
        )

        // Extract table data as JSON string
        val tableData: String = extractTableData(speechLogTable)

        return Pair(textViewData, tableData)
    }

    fun extractDilationNumber(dilationText: String): Int {
        val regex = Regex("\\d+")
        val matchResult = regex.find(dilationText)
        return matchResult?.value?.toInt() ?: 0
    }


    private fun extractTableData(speechLogTable: TableLayout): String {
        val tableData = mutableListOf<Map<String, String>>()

        // Get the number of rows in the table (excluding the header)
        val rowCount = speechLogTable.childCount

        // Assuming the first row is the header
        val headerRow = speechLogTable.getChildAt(0) as TableRow
        val headers = mutableListOf<String>()
        for (i in 0 until headerRow.childCount) {
            val headerTextView = headerRow.getChildAt(i) as TextView
            headers.add(headerTextView.text.toString())
        }

        // Iterate over the rows (excluding the header row)
        for (i in 1 until rowCount) {
            val row = speechLogTable.getChildAt(i) as TableRow
            val rowData = mutableMapOf<String, String>()
            for (j in 0 until row.childCount) {
                val cellTextView = row.getChildAt(j) as TextView
                rowData[headers[j]] = cellTextView.text.toString()
            }
            tableData.add(rowData)
        }

        // Convert the list of maps to a JSON string
        return Gson().toJson(tableData)
    }

    private fun getCurrentTimeInIST(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.timeZone = calendar.timeZone
        return format.format(calendar.time)
    }
}
