package com.example.cooldowns

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.util.concurrent.TimeUnit

/**
 * Represents a single timer entry with functionality to manage countdown, notifications, and updates.
 */
class TimerEntry(
    val name: String,  // Name of the timer.
    val hours: Int,    // Hours component of the timer's duration.
    val minutes: Int,  // Minutes component of the timer's duration.
    val seconds: Int,  // Seconds component of the timer's duration.
    private var countdownUpdateCallback: (String) -> Unit // Callback for updating the UI.
) {
    private var endTimeInMillis: Long = 0  // Time when the timer will end in milliseconds.
    private var isCountdownFinished: Boolean = false // Flag to indicate if the countdown has finished.
    private var durationInMillis: Long = 0 // Total duration of the timer in milliseconds.

    init {
        // Calculate the total duration in milliseconds during initialization.
        durationInMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())
    }

    // Set or update the callback function for countdown updates.
    fun setCountdownCallback(callback: (String) -> Unit) {
        countdownUpdateCallback = callback
    }

    // Get the formatted time of the timer
    fun getFormattedTime(): String {
        val millisUntilFinished = if (isCountdownFinished) 0 else endTimeInMillis - System.currentTimeMillis()
        return formatTime(millisUntilFinished)
    }

    // Start the countdown for this timer entry.
    fun startCountdown(context: Context) {
        val currentTime = System.currentTimeMillis()
        endTimeInMillis = currentTime + durationInMillis
        scheduleAlarm(context)
        updateCountdownTime(endTimeInMillis - currentTime)
    }

    // Get the progress percentage of the timer.
    fun getProgressPercentage(): Int {
        val totalDuration = durationInMillis
        val remaining = endTimeInMillis - System.currentTimeMillis()
        if (remaining <= 0) return 0
        return (remaining.toDouble() / totalDuration * 100).toInt()
    }

    // Schedule an alarm for the timer using AlarmManager.
    private fun scheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check for exact alarm scheduling permissions on Android S and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Notification permissions not fully enabled.", Toast.LENGTH_LONG).show()
            return
        }
        setAlarm(context, alarmManager)
    }

    // Set the actual alarm for when the timer ends.
    private fun setAlarm(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, TimerReceiver::class.java).apply {
            putExtra("TIMER_NAME", name)
        }
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, name.hashCode(), intent, pendingIntentFlag)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTimeInMillis, pendingIntent)
    }

    // Stop the countdown and cancel any set alarms.
    fun stopCountdown(context: Context) {
        cancelAlarm(context)
        isCountdownFinished = false
    }

    // Cancel the alarm associated with this timer.
    private fun cancelAlarm(context: Context) {
        val intent = Intent(context, TimerReceiver::class.java)
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, name.hashCode(), intent, pendingIntentFlag)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    // Check if the countdown is finished.
    fun isCountdownFinished(): Boolean = isCountdownFinished

    // Update the timer based on the current system time.
    fun updateBasedOnSystemTime() {
        if (!isCountdownFinished) {
            val currentTime = System.currentTimeMillis()
            if (currentTime >= endTimeInMillis) {
                isCountdownFinished = true
                countdownUpdateCallback.invoke("00:00:00")
            } else {
                updateCountdownTime(endTimeInMillis - currentTime)
            }
        }
    }

    // Update the countdown time and invoke the callback for UI update.
    private fun updateCountdownTime(remainingTimeMillis: Long) {
        if (remainingTimeMillis <= 0) {
            isCountdownFinished = true
            countdownUpdateCallback.invoke("00:00:00")
        } else {
            val formattedTime = formatTime(remainingTimeMillis)
            countdownUpdateCallback.invoke(formattedTime)
        }
    }

    // Format the time in hours:minutes:seconds format.
    private fun formatTime(millis: Long): String {
        if (millis <= 0) {
            return "00:00:00"
        }
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Serialization method for saving timer state.
    fun serialize(): String {
        return "$name,$hours,$minutes,$seconds,$endTimeInMillis,$isCountdownFinished"
    }

    // Companion object for deserialization of timer data.
    companion object {
        fun deserialize(serializedData: String, callback: (String) -> Unit): TimerEntry? {
            val parts = serializedData.split(',')
            if (parts.size < 6) {
                Log.e("TimerEntry", "Incomplete serialized data")
                return null
            }

            // Parsing the serialized data.
            val name = parts[0]
            val hours = parts[1].toIntOrNull()
            val minutes = parts[2].toIntOrNull()
            val seconds = parts[3].toIntOrNull()
            val endTimeInMillis = parts[4].toLongOrNull()
            val isCountdownFinished = parts[5].toBoolean()

            if (hours == null || minutes == null || seconds == null || endTimeInMillis == null) {
                Log.e("TimerEntry", "Parsing failed")
                return null
            }

            return TimerEntry(name, hours, minutes, seconds, callback).apply {
                this.endTimeInMillis = endTimeInMillis
                this.isCountdownFinished = isCountdownFinished
                if (!isCountdownFinished) {
                    updateBasedOnSystemTime()
                }
            }
        }
    }
}
