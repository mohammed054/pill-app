package com.example.larginine.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.larginine.R
import com.example.larginine.data.AppDatabase
import com.example.larginine.data.NotificationPhrase
import com.example.larginine.data.PillHistory
import com.example.larginine.ui.MainActivity
import com.example.larginine.worker.PillReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "pill_reminder_channel"
        const val ACTION_TAKEN = "com.example.larginine.ACTION_TAKEN"
        const val ACTION_SKIP = "com.example.larginine.ACTION_SKIP"
        const val EXTRA_PILL_ID = "pill_id"
        const val EXTRA_PILL_NAME = "pill_name"
        const val NOTIFICATION_ID_BASE = 1000

        private val takenResponses = listOf(
            "Awesome, you took it!",
            "Great job! Keep it up!",
            "You did it! Proud of you!",
            "Health points +10!",
            "Way to go! Stay consistent!",
            "You're amazing!",
            "That's the spirit!",
            "Well done! Your body thanks you!"
        )

        private val skippedResponses = listOf(
            "No worries, you can still take it later!",
            "It's okay, tomorrow is another day!",
            "Don't worry, we won't judge!",
            "You can always catch up later!",
            "No pressure! Your health matters!",
            "We'll remind you again soon!"
        )
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * Creates a notification channel for Android 8.0+.
     * This channel allows users to customize notification settings.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pill Reminders"
            val descriptionText = "Notifications for your pill reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
                enableLights(true)
                lightColor = Color.GREEN
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Gets a random notification phrase from enabled phrases.
     * Falls back to default phrases if no custom phrases are enabled.
     */
    suspend fun getRandomPhrase(): String {
        return try {
            val database = AppDatabase.getDatabase(context)
            val phrases = database.notificationPhraseDao().getEnabledPhrases().first()
            if (phrases.isNotEmpty()) {
                phrases[Random.nextInt(phrases.size)].phrase
            } else {
                getDefaultPhrase()
            }
        } catch (e: Exception) {
            getDefaultPhrase()
        }
    }

    private fun getDefaultPhrase(): String {
        val defaultPhrases = listOf(
            "Did you remember your commitment?",
            "Time to be awesome and take your pill!",
            "Stay consistent! Your future self will thank you.",
            "Quick check — did you take it?",
            "A moment of discipline. A lifetime of health."
        )
        return defaultPhrases[Random.nextInt(defaultPhrases.size)]
    }

    /**
     * Shows a reminder notification with Take and Skip action buttons.
     * The notification does NOT contain the pill name for privacy.
     *
     * @param pillId The ID of the pill to remind about
     * @param pillName The name of the pill (not shown in notification)
     * @param soundEnabled Whether sound should play
     * @param vibrationEnabled Whether vibration should occur
     * @param ledEnabled Whether LED should flash
     */
    fun showReminderNotification(
        pillId: Long,
        pillName: String,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        ledEnabled: Boolean = false
    ) {
        val notificationId = NOTIFICATION_ID_BASE + pillId.toInt()

        // Intent for when user taps the notification (opens app)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Mark as Taken
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_PILL_ID, pillId)
            putExtra(EXTRA_PILL_NAME, pillName)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            takenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Skip
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra(EXTRA_PILL_ID, pillId)
            putExtra(EXTRA_PILL_NAME, pillName)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            skipIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reminder")
            .setContentText("Tap to open app")
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_send,
                "I Took It",
                takenPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Skip",
                skipPendingIntent
            )

        // Apply sound setting
        if (!soundEnabled) {
            builder.setSound(null)
        }

        // Apply vibration setting
        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
        } else {
            builder.setVibrate(null)
        }

        // Apply LED setting (only works on supported devices)
        if (ledEnabled) {
            builder.setLights(Color.GREEN, 500, 500)
        }

        try {
            // Launch coroutine to get random phrase asynchronously
            CoroutineScope(Dispatchers.Main).launch {
                val phrase = NotificationHelper(context).getRandomPhrase()
                val updatedBuilder = builder
                    .setContentText(phrase)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(phrase))

                try {
                    notificationManager.notify(notificationId, updatedBuilder.build())
                } catch (e: SecurityException) {
                    // Handle case where notification permission is not granted
                }
            }
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
        }
    }

    /**
     * Shows a small popup notification after user takes or skips a pill.
     * This gives immediate feedback without interrupting the user.
     *
     * @param isTaken Whether the pill was taken (true) or skipped (false)
     */
    fun showResponseNotification(isTaken: Boolean) {
        val notificationId = System.currentTimeMillis().toInt()
        
        val message = if (isTaken) {
            takenResponses[Random.nextInt(takenResponses.size)]
        } else {
            skippedResponses[Random.nextInt(skippedResponses.size)]
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isTaken) "Great job!" else "Noted")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
        }
    }

    /**
     * Cancels a specific notification.
     */
    fun cancelNotification(pillId: Long) {
        val notificationId = NOTIFICATION_ID_BASE + pillId.toInt()
        notificationManager.cancel(notificationId)
    }
}

/**
 * BroadcastReceiver that handles notification button clicks.
 * This runs when user clicks "I Took It" or "Skip" in the notification.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pillId = intent.getLongExtra(NotificationHelper.EXTRA_PILL_ID, -1)
        val pillName = intent.getStringExtra(NotificationHelper.EXTRA_PILL_NAME) ?: "Pill"

        if (pillId == -1L) return

        when (intent.action) {
            NotificationHelper.ACTION_TAKEN -> {
                handlePillTaken(context, pillId, pillName)
            }
            NotificationHelper.ACTION_SKIP -> {
                handlePillSkipped(context, pillId, pillName)
            }
        }
    }

    private fun handlePillTaken(context: Context, pillId: Long, pillName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                
                // Record the history
                database.pillHistoryDao().insertHistory(
                    PillHistory(
                        pillId = pillId,
                        pillName = pillName,
                        status = PillHistory.STATUS_TAKEN
                    )
                )

                // Cancel the reminder notification
                NotificationHelper(context).cancelNotification(pillId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Show fun response notification
        NotificationHelper(context).showResponseNotification(isTaken = true)
    }

    private fun handlePillSkipped(context: Context, pillId: Long, pillName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                
                // Record the history
                database.pillHistoryDao().insertHistory(
                    PillHistory(
                        pillId = pillId,
                        pillName = pillName,
                        status = PillHistory.STATUS_SKIPPED
                    )
                )

                // Cancel the reminder notification
                NotificationHelper(context).cancelNotification(pillId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Show encouraging response notification
        NotificationHelper(context).showResponseNotification(isTaken = false)
    }
}
