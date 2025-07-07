// File: TimeUtils.kt
package com.uptsu.fhr

/**
 * Converts a time string from "hh:mm:ss" format to "hh:mm am/pm" format.
 *
 * @param timeString The time string in "hh:mm:ss" format.
 * @return The formatted time string in "hh:mm am/pm" format.
 */
fun convertTime(timeString: String?): String {
    // Check if the input string is null or empty
    if (timeString.isNullOrEmpty()) {
        return "Invalid time format"
    }

    // Split the time string by colons and parse hours, minutes, and seconds
    val timeParts = timeString.split(":")
    return if (timeParts.size == 3) {
        try {
            val hours24 = timeParts[0].trim().toInt()
            val minutes = timeParts[1].trim().toInt()
            // Seconds are not used in the final format
            // val seconds = timeParts[2].trim().toInt()

            // Convert 24-hour format to 12-hour format
            val hours12 = if (hours24 % 12 == 0) 12 else hours24 % 12
            val amPm = if (hours24 < 12) "AM" else "PM"

            // Format the time to "hh:mm am/pm"
            String.format("%02d:%02d %s", hours12, minutes, amPm)

        } catch (e: NumberFormatException) {
            // Handle potential number format errors
            "Invalid time format"
        }
    } else {
        // Handle the case where the time string format is not as expected
        "Invalid time format"
    }
}
