package com.example.larginine.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.larginine.data.AppDatabase
import com.example.larginine.worker.PillReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver that reschedules all pill reminders after device boot.
 * This ensures reminders persist across device restarts.
 */
class BootReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllReminders(context)
        }
    }

    private fun rescheduleAllReminders(context: Context) {
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val pills = database.pillDao().getEnabledPills()

                pills.forEach { pill ->
                    scheduleReminder(
                        context = context,
                        pillId = pill.id,
                        hour = pill.reminderHour,
                        minute = pill.reminderMinute,
                        intervalMinutes = pill.intervalMinutes
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleReminder(
        context: Context,
        pillId: Long,
        hour: Int,
        minute: Int,
        intervalMinutes: Int
    ) {
        val workName = "pill_reminder_$pillId"

        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // If the target time has passed today, schedule for next occurrence
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        // Use the user-defined interval (converted to hours for WorkManager)
        val intervalHours = (intervalMinutes / 60).toLong().coerceAtLeast(1)

        val workRequest = PeriodicWorkRequestBuilder<PillReminderWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    PillReminderWorker.KEY_PILL_ID to pillId,
                    PillReminderWorker.KEY_INTERVAL_MINUTES to intervalMinutes
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
