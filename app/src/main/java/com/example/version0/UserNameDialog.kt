package com.example.version0

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.Toast

class UserNameDialog(private val context: Context, private val onNameEntered: (String) -> Unit) {

    fun show() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Enter your name")

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val userName = input.text.toString()
            if (userName.isBlank()) {
                Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
            } else {
                onNameEntered(userName)
                dialog.dismiss()
            }
        }
        builder.setCancelable(false)
        builder.show()
    }
}

