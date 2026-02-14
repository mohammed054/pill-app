package com.example.larginine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Pill::class, PillHistory::class, NotificationPhrase::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pillDao(): PillDao
    abstract fun pillHistoryDao(): PillHistoryDao
    abstract fun notificationPhraseDao(): NotificationPhraseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pill_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaultPhrases(database.notificationPhraseDao())
                }
            }
        }

        suspend fun populateDefaultPhrases(phraseDao: NotificationPhraseDao) {
            val defaultPhrases = listOf(
                "Did you remember your commitment?",
                "Time to be awesome and take your pill!",
                "Your health is waiting — don't forget!",
                "A quick reminder for your daily dose",
                "Stay consistent! Your future self will thank you.",
                "Quick check — did you take it?",
                "What happened to your commitment?",
                "Small habits. Big results.",
                "Don't break the streak!",
                "Time for your daily wellness moment",
                "Your body will thank you!",
                "Be kind to yourself — take your pill!",
                "Almost forgot, didn't you?",
                "Health is wealth — take your pill!",
                "You're doing great! Don't forget this one."
            )
            defaultPhrases.forEach { phrase ->
                phraseDao.insertPhrase(NotificationPhrase(phrase = phrase, isCustom = false, isEnabled = true))
            }
        }
    }
}
