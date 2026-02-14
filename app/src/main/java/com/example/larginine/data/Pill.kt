package com.example.larginine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pills")
data class Pill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val reminderHour: Int,
    val reminderMinute: Int,
    val intervalMinutes: Int = 480,
    val isEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val ledEnabled: Boolean = false,
    val lastNotificationTime: Long = 0
)
