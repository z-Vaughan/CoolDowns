package com.example.cooldowns

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * BroadcastReceiver that handles the alarm events for timers.
 * When a timer expires, this receiver is triggered to show a notification.
 */
class TimerReceiver : BroadcastReceiver() {

    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast for an expired timer.
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received, containing the timer's data.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Extract the name of the timer from the intent.
        val timerName = intent.getStringExtra("TIMER_NAME") ?: "Timer"

        // Check if the POST_NOTIFICATIONS permission is granted (required for Android 13 and above).
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Create and configure the notification for the expired timer.
            val notification = NotificationCompat.Builder(context, "YOUR_NOTIFICATION_CHANNEL_ID")
                .setSmallIcon(R.mipmap.ic_launcher) // Use an appropriate icon for your app.
                .setContentTitle("Timer: $timerName")
                .setContentText("has expired.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            // Post the notification using NotificationManagerCompat.
            with(NotificationManagerCompat.from(context)) {
                notify(timerName.hashCode(), notification)
            }
        } else {
            // Handle the case where the POST_NOTIFICATIONS permission is not granted.
            // Log this situation or handle it according to your app's requirement.
        }
    }
}
