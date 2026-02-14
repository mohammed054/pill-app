package com.example.larginine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_phrases")
data class NotificationPhrase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phrase: String,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true
)
