package com.example.larginine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pill_history")
data class PillHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pillId: Long,
    val pillName: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_TAKEN = "taken"
        const val STATUS_SKIPPED = "skipped"
        const val STATUS_PENDING = "pending"
    }
}
