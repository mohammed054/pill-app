package com.example.larginine.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.larginine.R
import com.example.larginine.data.PillHistory
import com.example.larginine.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<PillHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: PillHistory) {
            binding.pillName.text = history.pillName

            val context = binding.root.context

            when (history.status) {
                PillHistory.STATUS_TAKEN -> {
                    binding.statusIcon.text = "✓"
                    binding.statusIcon.setTextColor(ContextCompat.getColor(context, R.color.status_taken))
                    binding.statusText.text = context.getString(R.string.i_took_it)
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_taken))
                }
                PillHistory.STATUS_SKIPPED -> {
                    binding.statusIcon.text = "⊘"
                    binding.statusIcon.setTextColor(ContextCompat.getColor(context, R.color.status_skipped))
                    binding.statusText.text = context.getString(R.string.skip)
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_skipped))
                }
                else -> {
                    binding.statusIcon.text = "?"
                    binding.statusIcon.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    binding.statusText.text = "Pending"
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }

            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            binding.timestamp.text = dateFormat.format(Date(history.timestamp))
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<PillHistory>() {
        override fun areItemsTheSame(oldItem: PillHistory, newItem: PillHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PillHistory, newItem: PillHistory): Boolean {
            return oldItem == newItem
        }
    }
}
