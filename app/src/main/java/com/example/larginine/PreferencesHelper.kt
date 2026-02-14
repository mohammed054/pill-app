package com.example.larginine

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val PREFS_NAME = "larginine_prefs"
    }

    fun getTodayKey(): String = "pill_taken_${dateFormat.format(Date())}"

    fun isPillTakenToday(): Boolean {
        return prefs.getBoolean(getTodayKey(), false)
    }

    fun markPillAsTaken() {
        prefs.edit().putBoolean(getTodayKey(), true).apply()
    }

    fun getLastTakenDate(): String? {
        val today = dateFormat.format(Date())
        for (i in 0..30) {
            val date = Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000)
            val key = "pill_taken_${dateFormat.format(date)}"
            if (prefs.getBoolean(key, false)) {
                return dateFormat.format(date)
            }
        }
        return null
    }
}
