package com.example.larginine.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationPhraseDao {
    @Query("SELECT * FROM notification_phrases WHERE isEnabled = 1")
    fun getEnabledPhrases(): Flow<List<NotificationPhrase>>

    @Query("SELECT * FROM notification_phrases")
    fun getAllPhrases(): Flow<List<NotificationPhrase>>

    @Query("SELECT * FROM notification_phrases")
    fun getAllPhrasesLiveData(): LiveData<List<NotificationPhrase>>

    @Query("SELECT * FROM notification_phrases WHERE isCustom = 1")
    fun getCustomPhrases(): Flow<List<NotificationPhrase>>

    @Insert
    suspend fun insertPhrase(phrase: NotificationPhrase): Long

    @Update
    suspend fun updatePhrase(phrase: NotificationPhrase)

    @Delete
    suspend fun deletePhrase(phrase: NotificationPhrase)

    @Query("DELETE FROM notification_phrases WHERE id = :id")
    suspend fun deletePhraseById(id: Long)

    @Query("UPDATE notification_phrases SET isEnabled = :enabled WHERE id = :id")
    suspend fun setPhraseEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM notification_phrases")
    suspend fun getPhraseCount(): Int
}
