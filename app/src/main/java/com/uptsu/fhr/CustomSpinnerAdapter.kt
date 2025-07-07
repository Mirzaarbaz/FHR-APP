package com.uptsu.fhr

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class CustomSpinnerAdapter(context: Context, items: Array<String>) : ArrayAdapter<String>(context, R.layout.spinner_item, items) {

    private val greenColor = ContextCompat.getColor(context, R.color.dark_green)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        // Set the text color for the currently selected item
        if (position > 0) {
            (view as TextView).setTextColor(greenColor)
        } else {
            (view as TextView).setTextColor(Color.BLACK) // Default color for the prompt
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        // Set the text color for all dropdown items
        (view as TextView).setTextColor(if (position == 0) Color.BLUE else Color.BLACK)
        return view
    }
}
