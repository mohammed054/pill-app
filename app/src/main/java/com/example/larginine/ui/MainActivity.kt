package com.example.larginine.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.larginine.R
import com.example.larginine.data.Pill
import com.example.larginine.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PillViewModel
    private lateinit var adapter: PillAdapter

    private var selectedHour: Int = 8
    private var selectedMinute: Int = 0

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
        setupFab()
        observePills()
        requestNotificationPermission()
    }

    private fun setupRecyclerView() {
        adapter = PillAdapter { pill ->
            showDeleteConfirmation(pill)
        }
        binding.pillsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.pillsRecyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.addPillFab.setOnClickListener {
            showAddPillDialog()
        }
    }

    private fun observePills() {
        viewModel.allPills.observe(this) { pills ->
            adapter.submitList(pills)
            binding.emptyText.visibility = if (pills.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddPillDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_pill, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.pillNameEditText)
        val timeTextView = dialogView.findViewById<android.widget.TextView>(R.id.selectedTimeText)

        selectedHour = 8
        selectedMinute = 0
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
                    viewModel.addPill(name, selectedHour, selectedMinute)
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
