package com.example.larginine.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.larginine.data.AppDatabase
import com.example.larginine.notifications.NotificationHelper

/**
 * Worker that triggers pill reminder notifications.
 * Runs periodically based on user-defined intervals.
 * 
 * Key features:
 * - Respects custom reminder intervals
 * - Does NOT show pill name in notification (privacy)
 * - Shows different motivational phrases each time
 * - Supports notification customization (sound, vibration, LED)
 */
class PillReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pillId = inputData.getLong(KEY_PILL_ID, -1)
        val intervalMinutes = inputData.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES)

        if (pillId == -1L) {
            return Result.failure()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val pill = database.pillDao().getPillById(pillId)

        if (pill == null || !pill.isEnabled) {
            return Result.success()
        }

        // Check if we should skip this notification based on interval
        val currentTime = System.currentTimeMillis()
        val timeSinceLastNotification = currentTime - pill.lastNotificationTime
        val intervalMillis = intervalMinutes * 60 * 1000L

        // Only show notification if enough time has passed since last one
        if (pill.lastNotificationTime > 0 && timeSinceLastNotification < intervalMillis) {
            return Result.success()
        }

        // Update last notification time
        database.pillDao().updateLastNotificationTime(pillId, currentTime)

        // Show the notification with customizable settings
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showReminderNotification(
            pillId = pillId,
            pillName = pill.name,
            soundEnabled = pill.soundEnabled,
            vibrationEnabled = pill.vibrationEnabled,
            ledEnabled = pill.ledEnabled
        )

        return Result.success()
    }

    companion object {
        const val KEY_PILL_ID = "pill_id"
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        const val DEFAULT_INTERVAL_MINUTES = 480 // Default: 8 hours
    }
}
