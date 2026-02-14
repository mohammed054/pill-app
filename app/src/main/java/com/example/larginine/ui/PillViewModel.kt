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
import com.example.larginine.data.NotificationPhrase
import com.example.larginine.data.Pill
import com.example.larginine.data.PillHistory
import com.example.larginine.data.PillRepository
import com.example.larginine.worker.PillReminderWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * ViewModel for managing pills, history, and notifications.
 * Handles all business logic for the pill reminder app.
 */
class PillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PillRepository
    val allPills: LiveData<List<Pill>>
    val allHistory: LiveData<List<PillHistory>>
    val allPhrases: LiveData<List<NotificationPhrase>>

    init {
        val database = AppDatabase.getDatabase(application)
        val pillDao = database.pillDao()
        val pillHistoryDao = database.pillHistoryDao()
        val phraseDao = database.notificationPhraseDao()

        repository = PillRepository(pillDao, pillHistoryDao, phraseDao)
        allPills = repository.allPillsLiveData
        allHistory = repository.allHistory as LiveData<List<PillHistory>>
        allPhrases = repository.allPhrases as LiveData<List<NotificationPhrase>>
    }

    /**
     * Adds a new pill with custom reminder settings.
     */
    fun addPill(
        name: String,
        hour: Int,
        minute: Int,
        intervalMinutes: Int = 480,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        ledEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val pill = Pill(
                name = name,
                reminderHour = hour,
                reminderMinute = minute,
                intervalMinutes = intervalMinutes,
                isEnabled = true,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                ledEnabled = ledEnabled
            )
            val pillId = repository.insertPill(pill)
            scheduleReminder(pillId, hour, minute, intervalMinutes)
        }
    }

    /**
     * Updates an existing pill with new settings.
     */
    fun updatePill(
        pill: Pill,
        name: String,
        hour: Int,
        minute: Int,
        intervalMinutes: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        ledEnabled: Boolean
    ) {
        viewModelScope.launch {
            val updatedPill = pill.copy(
                name = name,
                reminderHour = hour,
                reminderMinute = minute,
                intervalMinutes = intervalMinutes,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                ledEnabled = ledEnabled
            )
            repository.updatePill(updatedPill)

            // Reschedule the reminder with new settings
            cancelReminder(pill.id)
            if (updatedPill.isEnabled) {
                scheduleReminder(pill.id, hour, minute, intervalMinutes)
            }
        }
    }

    /**
     * Deletes a pill and its history, then cancels any scheduled reminders.
     */
    fun deletePill(pill: Pill) {
        viewModelScope.launch {
            cancelReminder(pill.id)
            repository.deleteHistoryForPill(pill.id)
            repository.deletePill(pill)
        }
    }

    /**
     * Toggles whether a pill's reminder is enabled.
     */
    fun togglePillEnabled(pill: Pill) {
        viewModelScope.launch {
            val newEnabled = !pill.isEnabled
            repository.setPillEnabled(pill.id, newEnabled)

            if (newEnabled) {
                scheduleReminder(
                    pill.id,
                    pill.reminderHour,
                    pill.reminderMinute,
                    pill.intervalMinutes
                )
            } else {
                cancelReminder(pill.id)
            }
        }
    }

    /**
     * Schedules a periodic reminder for a pill using WorkManager.
     * The reminder will trigger every X minutes (intervalMinutes).
     *
     * @param pillId The unique ID of the pill
     * @param hour The hour of the first reminder (0-23)
     * @param minute The minute of the first reminder (0-59)
     * @param intervalMinutes How often to repeat the reminder (in minutes)
     */
    fun scheduleReminder(pillId: Long, hour: Int, minute: Int, intervalMinutes: Int) {
        val workName = "pill_reminder_$pillId"

        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // If the target time has passed today, schedule for tomorrow
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        // Convert interval to hours (WorkManager requirement)
        // Minimum 1 hour, otherwise use the specified interval
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

        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels a scheduled reminder for a specific pill.
     */
    fun cancelReminder(pillId: Long) {
        val workName = "pill_reminder_$pillId"
        WorkManager.getInstance(getApplication()).cancelUniqueWork(workName)
    }

    /**
     * Reschedules all pill reminders. Called on app start and after boot.
     */
    fun rescheduleAllPills() {
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            val pills = database.pillDao().getAllPills().first()

            pills.forEach { pill ->
                if (pill.isEnabled) {
                    scheduleReminder(
                        pill.id,
                        pill.reminderHour,
                        pill.reminderMinute,
                        pill.intervalMinutes
                    )
                }
            }
        }
    }

    /**
     * Gets the today's status for a specific pill (taken, skipped, or pending).
     */
    suspend fun getTodayStatusForPill(pillId: Long): PillHistory? {
        val startOfDay = getStartOfDay()
        return repository.getTodayStatusForPill(pillId, startOfDay)
    }

    /**
     * Gets today's statistics.
     */
    suspend fun getTodayStats(): Pair<Int, Int> {
        val startOfDay = getStartOfDay()
        val taken = repository.getCountByStatusToday(PillHistory.STATUS_TAKEN, startOfDay)
        val skipped = repository.getCountByStatusToday(PillHistory.STATUS_SKIPPED, startOfDay)
        return Pair(taken, skipped)
    }

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Adds a custom notification phrase.
     */
    fun addCustomPhrase(phrase: String) {
        viewModelScope.launch {
            repository.insertPhrase(
                NotificationPhrase(
                    phrase = phrase,
                    isCustom = true,
                    isEnabled = true
                )
            )
        }
    }

    /**
     * Toggles a phrase's enabled state.
     */
    fun togglePhraseEnabled(phrase: NotificationPhrase) {
        viewModelScope.launch {
            repository.setPhraseEnabled(phrase.id, !phrase.isEnabled)
        }
    }

    /**
     * Deletes a custom phrase.
     */
    fun deletePhrase(phrase: NotificationPhrase) {
        viewModelScope.launch {
            repository.deletePhrase(phrase)
        }
    }

    /**
     * Clears all history.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    /**
     * Manually marks a pill as taken.
     */
    fun markAsTaken(pill: Pill) {
        viewModelScope.launch {
            repository.insertHistory(
                PillHistory(
                    pillId = pill.id,
                    pillName = pill.name,
                    status = PillHistory.STATUS_TAKEN
                )
            )
        }
    }

    /**
     * Manually marks a pill as skipped.
     */
    fun markAsSkipped(pill: Pill) {
        viewModelScope.launch {
            repository.insertHistory(
                PillHistory(
                    pillId = pill.id,
                    pillName = pill.name,
                    status = PillHistory.STATUS_SKIPPED
                )
            )
        }
    }
}
