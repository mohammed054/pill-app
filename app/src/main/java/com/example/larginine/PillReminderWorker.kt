package com.example.larginine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PillReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefsHelper = PreferencesHelper(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)

        if (!prefsHelper.isPillTakenToday()) {
            notificationHelper.showPillReminderNotification()
        }

        return Result.success()
    }
}
