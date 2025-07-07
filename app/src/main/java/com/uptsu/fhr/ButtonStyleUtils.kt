package com.uptsu.fhr

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

object ButtonStyleUtils {

    fun setListeningStyle(context: Context, button: Button, card: LinearLayout) {
        button.text = "Listening..."
        button.isClickable = false
        val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.baseline_graphic_eq_black_36)

        button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
        button.setBackgroundResource(R.drawable.button_shape_maroon)
        button.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.maroon))
        button.setTextColor(ContextCompat.getColor(context, R.color.maroon))
        card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.maroon))
    }

    fun setStartStyle(context: Context, button: Button, card: LinearLayout) {
        button.text = "Start"
        button.isClickable = true
        val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.baseline_mic_black_36)

        button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
        button.setBackgroundResource(R.drawable.button_shape_blue)
        button.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue))
        button.setTextColor(ContextCompat.getColor(context, R.color.blue))
        card.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue))
    }
}
