package com.example.larginine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PreferencesHelper
    private lateinit var notificationHelper: NotificationHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled. You won't receive reminders.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsHelper = PreferencesHelper(this)
        notificationHelper = NotificationHelper(this)

        setupUI()
        requestNotificationPermission()
    }

    private fun setupUI() {
        val messageText = findViewById<TextView>(R.id.messageText)
        val tookItButton = findViewById<Button>(R.id.tookItButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        updateStatus(statusText)

        tookItButton.setOnClickListener {
            prefsHelper.markPillAsTaken()
            updateStatus(statusText)
            Toast.makeText(this, "Great! Pill marked as taken for today.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(statusText: TextView) {
        val taken = prefsHelper.isPillTakenToday()
        statusText.text = if (taken) "✓ Taken today" else "✗ Not taken yet"
        statusText.setTextColor(
            if (taken) ContextCompat.getColor(this, R.color.status_taken)
            else ContextCompat.getColor(this, R.color.status_not_taken)
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        updateStatus(statusText)
    }
}
