package com.example.larginine.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

class PillRepository(
    private val pillDao: PillDao,
    private val pillHistoryDao: PillHistoryDao,
    private val notificationPhraseDao: NotificationPhraseDao
) {
    val allPills: Flow<List<Pill>> = pillDao.getAllPills()
    val allPillsLiveData: LiveData<List<Pill>> = pillDao.getAllPillsLiveData()
    val allHistory: Flow<List<PillHistory>> = pillHistoryDao.getAllHistory()
    val enabledPhrases: Flow<List<NotificationPhrase>> = notificationPhraseDao.getEnabledPhrases()
    val allPhrases: Flow<List<NotificationPhrase>> = notificationPhraseDao.getAllPhrases()

    suspend fun getPillById(id: Long): Pill? {
        return pillDao.getPillById(id)
    }

    suspend fun getEnabledPills(): List<Pill> {
        return pillDao.getEnabledPills()
    }

    suspend fun insertPill(pill: Pill): Long {
        return pillDao.insertPill(pill)
    }

    suspend fun updatePill(pill: Pill) {
        pillDao.updatePill(pill)
    }

    suspend fun deletePill(pill: Pill) {
        pillDao.deletePill(pill)
    }

    suspend fun deletePillById(id: Long) {
        pillDao.deletePillById(id)
    }

    suspend fun updateLastNotificationTime(pillId: Long, time: Long) {
        pillDao.updateLastNotificationTime(pillId, time)
    }

    suspend fun setPillEnabled(pillId: Long, enabled: Boolean) {
        pillDao.setPillEnabled(pillId, enabled)
    }

    suspend fun insertHistory(history: PillHistory): Long {
        return pillHistoryDao.insertHistory(history)
    }

    suspend fun deleteHistoryForPill(pillId: Long) {
        pillHistoryDao.deleteHistoryForPill(pillId)
    }

    suspend fun clearAllHistory() {
        pillHistoryDao.clearAllHistory()
    }

    suspend fun getTodayStatusForPill(pillId: Long, startOfDay: Long): PillHistory? {
        return pillHistoryDao.getTodayStatusForPill(pillId, startOfDay)
    }

    suspend fun getCountByStatusToday(status: String, startOfDay: Long): Int {
        return pillHistoryDao.getCountByStatusToday(status, startOfDay)
    }

    suspend fun getPhraseCount(): Int {
        return notificationPhraseDao.getPhraseCount()
    }

    suspend fun insertPhrase(phrase: NotificationPhrase): Long {
        return notificationPhraseDao.insertPhrase(phrase)
    }

    suspend fun updatePhrase(phrase: NotificationPhrase) {
        notificationPhraseDao.updatePhrase(phrase)
    }

    suspend fun deletePhrase(phrase: NotificationPhrase) {
        notificationPhraseDao.deletePhrase(phrase)
    }

    suspend fun setPhraseEnabled(id: Long, enabled: Boolean) {
        notificationPhraseDao.setPhraseEnabled(id, enabled)
    }
}
