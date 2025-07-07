package com.uptsu.fhr

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

object TableUtils {

    private fun createTextView(context: Context, text: String, gravity: Int): TextView {
        return TextView(context).apply {
            this.text = text
            this.gravity = gravity
            this.setTextColor(Color.BLACK)
            this.setPadding(16, 16, 16, 16)
        }
    }

    fun getTableDataAsJson(tableLayout: TableLayout): String {
        val jsonArray = JSONArray()

        for (i in 0 until tableLayout.childCount) {
            val tableRow = tableLayout.getChildAt(i) as? TableRow ?: continue
            val jsonObject = JSONObject()
            for (j in 0 until tableRow.childCount) {
                val textView = tableRow.getChildAt(j) as? TextView ?: continue
                jsonObject.put("column_$j", textView.text.toString())
            }
            jsonArray.put(jsonObject)
        }

        return jsonArray.toString()
    }

    fun addDataToTable(
        context: Context,
        tableLayout: TableLayout,
        counter: Int,
        numbers: List<Int>,
        formattedTime: String
    ) {
        val newRow = TableRow(context)
        val numberValue = numbers[0]

        val counterTextView = createTextView(
            context,
            counter.toString(),
            Gravity.CENTER
        )
        val numbersTextView = createTextView(
            context,
            numbers.joinToString(", "),
            Gravity.CENTER
        )
        val timeTextView = createTextView(
            context,
            formattedTime,
            Gravity.CENTER
        )

        newRow.addView(counterTextView)
        newRow.addView(numbersTextView)
        newRow.addView(timeTextView)

        // Set background color based on the number value
        if (numberValue in 120..160) {
            newRow.setBackgroundColor(ContextCompat.getColor(context, R.color.light_blue))
        } else {
            newRow.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
            counterTextView.setTextColor(Color.WHITE)
            numbersTextView.setTextColor(Color.WHITE)
            timeTextView.setTextColor(Color.WHITE)
        }

        tableLayout.addView(newRow)
    }
}
