package com.example.larginine

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class LArginineApp : Application() {

    companion object {
        const val WORK_NAME = "pill_reminder_work"
    }

    override fun onCreate() {
        super.onCreate()
        schedulePillReminder()
    }

    private fun schedulePillReminder() {
        val workRequest = PeriodicWorkRequestBuilder<PillReminderWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
