package com.example.larginine.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PillDao {
    @Query("SELECT * FROM pills ORDER BY reminderHour, reminderMinute")
    fun getAllPills(): Flow<List<Pill>>

    @Query("SELECT * FROM pills ORDER BY reminderHour, reminderMinute")
    fun getAllPillsLiveData(): LiveData<List<Pill>>

    @Query("SELECT * FROM pills WHERE id = :id")
    suspend fun getPillById(id: Long): Pill?

    @Query("SELECT * FROM pills WHERE isEnabled = 1")
    suspend fun getEnabledPills(): List<Pill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPill(pill: Pill): Long

    @Update
    suspend fun updatePill(pill: Pill)

    @Delete
    suspend fun deletePill(pill: Pill)

    @Query("DELETE FROM pills WHERE id = :id")
    suspend fun deletePillById(id: Long)

    @Query("UPDATE pills SET lastNotificationTime = :time WHERE id = :pillId")
    suspend fun updateLastNotificationTime(pillId: Long, time: Long)

    @Query("UPDATE pills SET isEnabled = :enabled WHERE id = :pillId")
    suspend fun setPillEnabled(pillId: Long, enabled: Boolean)
}
