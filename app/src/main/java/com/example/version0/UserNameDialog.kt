package com.example.version0

import android.graphics.drawable.Drawable
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class UserNameDialog(
    private val context: Context,
    private val onNameEntered: (String, Int, String) -> Unit,
    private val saveCallback: () -> Unit
) {

    fun show(triggeringButton: String) {
        // Inflate the custom layout
        val inflater = LayoutInflater.from(context)
        val dialogView: View = inflater.inflate(R.layout.dialog_user_details, null)

        // Find views in the custom layout
        val input: EditText = dialogView.findViewById(R.id.editTextUserName)
        val minusButton: Button = dialogView.findViewById(R.id.buttonMinus)
        val plusButton: Button = dialogView.findViewById(R.id.buttonPlus)
        val valueTextView: TextView = dialogView.findViewById(R.id.textViewDilationValue)

        // Create the AlertDialog builder
        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)

        // Handle - Button click to decrement the value
        minusButton.setOnClickListener {
            val currentValue = valueTextView.text.toString().toInt()
            if (currentValue > 1) {
                valueTextView.text = (currentValue - 1).toString()
            }
        }

        // Handle + Button click to increment the value
        plusButton.setOnClickListener {
            val currentValue = valueTextView.text.toString().toInt()
            if (currentValue < 10) {
                valueTextView.text = (currentValue + 1).toString()
            }
        }

        // Handle the positive button click
        builder.setPositiveButton("OK") { dialog, _ ->
            val userName = input.text.toString()
            if (userName.isBlank()) {
                Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
            } else {
                val numberValue = valueTextView.text.toString().toInt()
                onNameEntered(userName, numberValue, triggeringButton)
                saveCallback()
                dialog.dismiss()
            }
        }

        builder.setCancelable(false)
        builder.show()
    }
}
