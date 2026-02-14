package com.example.larginine.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.larginine.data.AppDatabase
import com.example.larginine.data.Pill
import com.example.larginine.data.PillRepository
import com.example.larginine.worker.PillReminderWorker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PillRepository
    val allPills: LiveData<List<Pill>>

    init {
        val pillDao = AppDatabase.getDatabase(application).pillDao()
        repository = PillRepository(pillDao)
        allPills = repository.allPillsLiveData
    }

    fun addPill(name: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val pill = Pill(
                name = name,
                reminderHour = hour,
                reminderMinute = minute
            )
            val pillId = repository.insertPill(pill)
            scheduleReminder(pillId, hour, minute)
        }
    }

    fun deletePill(pill: Pill) {
        viewModelScope.launch {
            cancelReminder(pill.id)
            repository.deletePill(pill)
        }
    }

    fun scheduleReminder(pillId: Long, hour: Int, minute: Int) {
        val workName = "pill_reminder_$pillId"

        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<PillReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(PillReminderWorker.KEY_PILL_ID to pillId))
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(pillId: Long) {
        val workName = "pill_reminder_$pillId"
        WorkManager.getInstance(getApplication()).cancelUniqueWork(workName)
    }

    fun rescheduleAllPills() {
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            val pills = database.pillDao().getAllPills()
            pills.collect { pillList ->
                pillList.forEach { pill ->
                    scheduleReminder(pill.id, pill.reminderHour, pill.reminderMinute)
                }
            }
        }
    }
}
