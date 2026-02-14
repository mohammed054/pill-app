package com.example.larginine.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.larginine.R
import com.example.larginine.data.Pill
import com.example.larginine.data.PillHistory
import com.example.larginine.databinding.ItemPillBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Adapter for displaying pills in a RecyclerView.
 * Shows pill name, time, interval, and current status.
 */
class PillAdapter(
    private val onDeleteClick: (Pill) -> Unit,
    private val onEditClick: (Pill) -> Unit,
    private val onStatusClick: (Pill, String) -> Unit,
    private val getTodayStatus: suspend (Long) -> PillHistory?
) : ListAdapter<Pill, PillAdapter.PillViewHolder>(PillDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PillViewHolder {
        val binding = ItemPillBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PillViewHolder(
        private val binding: ItemPillBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pill: Pill) {
            binding.pillName.text = pill.name
            binding.pillTime.text = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                pill.reminderHour,
                pill.reminderMinute
            )

            // Display interval
            val intervalText = formatInterval(pill.intervalMinutes)
            binding.pillInterval.text = intervalText

            // Show enabled/disabled state
            binding.enabledIndicator.visibility = if (pill.isEnabled) View.VISIBLE else View.GONE

            // Load today's status asynchronously
            loadTodayStatus(pill.id)

            // Set click listeners
            binding.deleteButton.setOnClickListener {
                onDeleteClick(pill)
            }

            binding.editButton.setOnClickListener {
                onEditClick(pill)
            }

            binding.takenButton.setOnClickListener {
                onStatusClick(pill, PillHistory.STATUS_TAKEN)
            }

            binding.skippedButton.setOnClickListener {
                onStatusClick(pill, PillHistory.STATUS_SKIPPED)
            }
        }

        private fun loadTodayStatus(pillId: Long) {
            CoroutineScope(Dispatchers.Main).launch {
                val status = getTodayStatus(pillId)
                updateStatusUI(status)
            }
        }

        private fun updateStatusUI(status: PillHistory?) {
            when (status?.status) {
                PillHistory.STATUS_TAKEN -> {
                    binding.pillStatus.text = binding.root.context.getString(R.string.taken_today)
                    binding.pillStatus.setTextColor(
                        binding.root.context.getColor(R.color.status_taken)
                    )
                    binding.takenButton.visibility = View.GONE
                    binding.skippedButton.visibility = View.GONE
                }
                PillHistory.STATUS_SKIPPED -> {
                    binding.pillStatus.text = binding.root.context.getString(R.string.skipped_today)
                    binding.pillStatus.setTextColor(
                        binding.root.context.getColor(R.color.status_skipped)
                    )
                    binding.takenButton.visibility = View.GONE
                    binding.skippedButton.visibility = View.GONE
                }
                else -> {
                    binding.pillStatus.text = binding.root.context.getString(R.string.not_taken_yet)
                    binding.pillStatus.setTextColor(
                        binding.root.context.getColor(R.color.status_not_taken)
                    )
                    binding.takenButton.visibility = View.VISIBLE
                    binding.skippedButton.visibility = View.VISIBLE
                }
            }
        }

        private fun formatInterval(minutes: Int): String {
            return when {
                minutes >= 1440 -> "${minutes / 1440} day(s)"
                minutes >= 60 -> "${minutes / 60} hour(s)"
                else -> "$minutes min"
            }
        }
    }

    class PillDiffCallback : DiffUtil.ItemCallback<Pill>() {
        override fun areItemsTheSame(oldItem: Pill, newItem: Pill): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pill, newItem: Pill): Boolean {
            return oldItem == newItem
        }
    }
}
