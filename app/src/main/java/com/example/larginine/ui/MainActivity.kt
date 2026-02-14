package com.example.larginine.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.larginine.R
import com.example.larginine.data.Pill
import com.example.larginine.data.PillHistory
import com.example.larginine.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PillViewModel
    private lateinit var adapter: PillAdapter

    private var selectedHour: Int = 8
    private var selectedMinute: Int = 0
    private var selectedInterval: Int = 480
    private var soundEnabled: Boolean = true
    private var vibrationEnabled: Boolean = true
    private var ledEnabled: Boolean = false

    private val intervalOptions = listOf(
        30 to "Every 30 minutes",
        60 to "Every 1 hour",
        120 to "Every 2 hours",
        240 to "Every 4 hours",
        360 to "Every 6 hours",
        480 to "Every 8 hours",
        720 to "Every 12 hours",
        1440 to "Every 24 hours"
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                getString(R.string.permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[PillViewModel::class.java]

        setupRecyclerView()
        setupButtons()
        observePills()
        observeStats()
        requestNotificationPermission()

        viewModel.rescheduleAllPills()
    }

    private fun setupRecyclerView() {
        adapter = PillAdapter(
            onDeleteClick = { pill -> showDeleteConfirmation(pill) },
            onEditClick = { pill -> showEditPillDialog(pill) },
            onStatusClick = { pill, status -> handleStatusClick(pill, status) },
            getTodayStatus = { pillId -> viewModel.getTodayStatusForPill(pillId) }
        )
        binding.pillsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.pillsRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.addPillFab.setOnClickListener {
            showAddPillDialog()
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observePills() {
        viewModel.allPills.observe(this) { pills ->
            adapter.submitList(pills)
            binding.emptyText.visibility = if (pills.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun observeStats() {
        lifecycleScope.launch {
            val stats = viewModel.getTodayStats()
            binding.takenCountText.text = getString(R.string.taken_count, stats.first)
            binding.skippedCountText.text = getString(R.string.skipped_count, stats.second)
        }
    }

    private fun showAddPillDialog() {
        resetDialogValues()

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_pill, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.pillNameEditText)
        val timeTextView = dialogView.findViewById<android.widget.TextView>(R.id.selectedTimeText)
        val intervalSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.intervalSpinner)
        val soundCheckBox = dialogView.findViewById<android.widget.CheckBox>(R.id.soundCheckBox)
        val vibrationCheckBox = dialogView.findViewById<android.widget.CheckBox>(R.id.vibrationCheckBox)
        val ledCheckBox = dialogView.findViewById<android.widget.CheckBox>(R.id.ledCheckBox)

        // Setup interval spinner
        val intervalAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            intervalOptions.map { it.second }
        )
        intervalSpinner.adapter = intervalAdapter

        updateTimeDisplay(timeTextView)

        timeTextView.setOnClickListener {
            showTimePicker { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                updateTimeDisplay(timeTextView)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_pill)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    selectedInterval = intervalOptions[intervalSpinner.selectedItemPosition].first
                    soundEnabled = soundCheckBox.isChecked
                    vibrationEnabled = vibrationCheckBox.isChecked
                    ledEnabled = ledCheckBox.isChecked

                    viewModel.addPill(
                        name = name,
                        hour = selectedHour,
                        minute = selectedMinute,
                        intervalMinutes = selectedInterval,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        ledEnabled = ledEnabled
                    )
                    Toast.makeText(
                        this,
                        getString(R.string.reminder_set_for, name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditPillDialog(pill: Pill) {
        selectedHour = pill.reminderHour
        selectedMinute = pill.reminderMinute
        selectedInterval = pill.intervalMinutes
        soundEnabled = pill.soundEnabled
        vibrationEnabled = pill.vibrationEnabled
        ledEnabled = pill.ledEnabled

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_pill, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.pillNameEditText)
        val timeTextView = dialogView.findViewById<android.widget.TextView>(R.id.selectedTimeText)
        val intervalSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.intervalSpinner)
        val soundCheckBox = dialogView.findViewById<android.widget.CheckBox>(R.id.soundCheckBox)
        val vibrationCheckBox = dialogView.findViewById<android.widget.CheckBox>(R.id.vibrationCheckBox)
        val ledCheckBox = dialogView.findViewById<android.widget.CheckBox>(R.id.ledCheckBox)

        nameEditText.setText(pill.name)
        updateTimeDisplay(timeTextView)

        // Setup interval spinner
        val intervalAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            intervalOptions.map { it.second }
        )
        intervalSpinner.adapter = intervalAdapter

        // Set current interval
        val intervalIndex = intervalOptions.indexOfFirst { it.first == pill.intervalMinutes }
        if (intervalIndex >= 0) {
            intervalSpinner.setSelection(intervalIndex)
        }

        soundCheckBox.isChecked = pill.soundEnabled
        vibrationCheckBox.isChecked = pill.vibrationEnabled
        ledCheckBox.isChecked = pill.ledEnabled

        timeTextView.setOnClickListener {
            showTimePicker { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                updateTimeDisplay(timeTextView)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_pill)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    selectedInterval = intervalOptions[intervalSpinner.selectedItemPosition].first
                    soundEnabled = soundCheckBox.isChecked
                    vibrationEnabled = vibrationCheckBox.isChecked
                    ledEnabled = ledCheckBox.isChecked

                    viewModel.updatePill(
                        pill = pill,
                        name = name,
                        hour = selectedHour,
                        minute = selectedMinute,
                        intervalMinutes = selectedInterval,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        ledEnabled = ledEnabled
                    )
                    Toast.makeText(this, R.string.pill_updated, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleStatusClick(pill: Pill, status: String) {
        when (status) {
            PillHistory.STATUS_TAKEN -> viewModel.markAsTaken(pill)
            PillHistory.STATUS_SKIPPED -> viewModel.markAsSkipped(pill)
        }
        observeStats()
    }

    private fun showTimePicker(onTimeSelected: (Int, Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                onTimeSelected(hour, minute)
            },
            selectedHour,
            selectedMinute,
            true
        ).show()
    }

    private fun updateTimeDisplay(textView: android.widget.TextView) {
        textView.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            selectedHour,
            selectedMinute
        )
    }

    private fun resetDialogValues() {
        selectedHour = 8
        selectedMinute = 0
        selectedInterval = 480
        soundEnabled = true
        vibrationEnabled = true
        ledEnabled = false
    }

    private fun showDeleteConfirmation(pill: Pill) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deletePill(pill)
                Toast.makeText(this, R.string.reminder_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
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
}
