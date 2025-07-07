package com.uptsu.fhr

// NotificationUtils.kt
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.media.RingtoneManager
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat

object NotificationUtils {
    private const val CHANNEL_ID = "my_channel_id"
    const val NOTIFICATION_ID = 1


    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_HIGH // Ensures the notification shows prominently
            ).apply {
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000, 500)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    fun getNotification(context: Context): Notification {
        createNotificationChannel(context)

        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("NotificationUtils", "Notification permission not granted")
                return Notification() // Return an empty notification or handle appropriately
            }
        }

        val contentView = RemoteViews(context.packageName, R.layout.custom_notification_big)
        val contentViews = RemoteViews(context.packageName, R.layout.custom_notification)

        // Create an Intent to open your MainActivity
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        // Create a PendingIntent to be triggered when the notification is clicked
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d("NotificationUtils", "Showing notification with ID $NOTIFICATION_ID")
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(contentViews)
            .setCustomBigContentView(contentView)
            .setSmallIcon(R.drawable.baseline_mic_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for banner display
            .setCategory(NotificationCompat.CATEGORY_CALL) // Category call for importance
            .setVibrate(longArrayOf(0, 500, 1000, 500)) // Vibration pattern
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Notification sound
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Display in the notification shade
            .setAutoCancel(true) // Dismiss notification on click
            .setContentIntent(pendingIntent) // Set the PendingIntent here
            .build()
    }


    fun showNotification(context: Context) {
        val notification = getNotification(context)
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManagerCompat.notify(NOTIFICATION_ID, notification)
        }

    }

    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Log.d("NotificationUtils", "Attempting to cancel notification with ID $NOTIFICATION_ID")
        notificationManager.cancel(NOTIFICATION_ID)

        // Confirm if notification was canceled
        val activeNotifications = notificationManager.activeNotifications
        val isCanceled = activeNotifications.none { it.id == NOTIFICATION_ID }

        if (isCanceled) {
            Log.d("NotificationUtils", "Notification successfully canceled with ID $NOTIFICATION_ID")
        } else {
            Log.d("NotificationUtils", "Notification with ID $NOTIFICATION_ID still exists")
        }
    }



}