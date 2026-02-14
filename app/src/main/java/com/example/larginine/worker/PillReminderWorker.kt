package com.example.larginine.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.larginine.data.AppDatabase
import com.example.larginine.notifications.NotificationHelper

class PillReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pillId = inputData.getLong(KEY_PILL_ID, -1)

        if (pillId == -1L) {
            return Result.failure()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val pill = database.pillDao().getPillById(pillId)

        if (pill == null) {
            return Result.success()
        }

        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showReminderNotification(pillId)

        return Result.success()
    }

    companion object {
        const val KEY_PILL_ID = "pill_id"
    }
}
