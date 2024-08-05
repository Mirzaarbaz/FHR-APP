package com.example.version0

import android.app.AlertDialog
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class UserNameDialog(
    private val context: Context,
    private val onNameEntered: (String, Int, String) -> Unit // Added String parameter for button identifier
) {

    fun show(triggeringButton: String) { // Added parameter to identify which button clicked
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Enter Details")

        // Create a LinearLayout to hold the EditText and other components
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val padding = 16 // Padding for better spacing
        layout.setPadding(padding, padding, padding, padding)

        // Create and configure EditText
        val input = EditText(context)
        input.hint = "Enter Patient's Name"
        input.setPadding(36, 48, 36, 38)
        layout.addView(input)

        // Create a horizontal LinearLayout to hold the buttons and TextView
        val horizontalLayout = LinearLayout(context)
        horizontalLayout.orientation = LinearLayout.HORIZONTAL
        horizontalLayout.setPadding(16, 36, 16, 16) // Padding for better spacing

        // Create and configure - Button
        val di = TextView(context)
        di.text = "Dilation:  "
        di.textSize = 16f
        horizontalLayout.addView(di)

        val minusButton = Button(context)
        minusButton.text = "-"
        horizontalLayout.addView(minusButton)

        // Create and configure TextView to display the value
        val valueTextView = TextView(context)
        valueTextView.text = "4"
        valueTextView.textSize = 18f
        valueTextView.setPadding(16, 0, 16, 0) // Padding around the text
        valueTextView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        horizontalLayout.addView(valueTextView)

        val cm = TextView(context)
        cm.text = "cm  "
        horizontalLayout.addView(cm)

        // Create and configure + Button
        val plusButton = Button(context)
        plusButton.text = "+"
        horizontalLayout.addView(plusButton)

        // Add horizontal layout to the main layout
        layout.addView(horizontalLayout)

        // Handle - Button click to decrement the value
        minusButton.setOnClickListener {
            val currentValue = valueTextView.text.toString().toInt()
            if (currentValue > 1) { // Ensures the value does not go below 1
                valueTextView.text = (currentValue - 1).toString()
            }
        }

        // Handle + Button click to increment the value
        plusButton.setOnClickListener {
            val currentValue = valueTextView.text.toString().toInt()
            if (currentValue < 10) { // Ensures the value does not exceed 10
                valueTextView.text = (currentValue + 1).toString()
            }
        }

        // Set the custom view for the AlertDialog
        builder.setView(layout)

        // Handle the positive button click
        builder.setPositiveButton("OK") { dialog, _ ->
            val userName = input.text.toString()
            if (userName.isBlank()) {
                Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
            } else {
                val numberValue = valueTextView.text.toString().toInt()
                onNameEntered(userName, numberValue, triggeringButton) // Pass the button identifier
                dialog.dismiss()
            }
        }

        builder.setCancelable(false)
        builder.show()
    }
}
