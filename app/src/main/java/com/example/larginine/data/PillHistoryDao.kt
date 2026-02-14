package com.example.larginine.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PillHistoryDao {
    @Query("SELECT * FROM pill_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PillHistory>>

    @Query("SELECT * FROM pill_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<PillHistory>>

    @Query("SELECT * FROM pill_history WHERE pillId = :pillId ORDER BY timestamp DESC")
    fun getHistoryForPill(pillId: Long): Flow<List<PillHistory>>

    @Query("SELECT * FROM pill_history WHERE pillId = :pillId AND timestamp >= :startOfDay ORDER BY timestamp DESC LIMIT 1")
    suspend fun getTodayStatusForPill(pillId: Long, startOfDay: Long): PillHistory?

    @Insert
    suspend fun insertHistory(history: PillHistory): Long

    @Query("DELETE FROM pill_history WHERE pillId = :pillId")
    suspend fun deleteHistoryForPill(pillId: Long)

    @Query("DELETE FROM pill_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM pill_history WHERE status = :status AND timestamp >= :startOfDay")
    suspend fun getCountByStatusToday(status: String, startOfDay: Long): Int
}
