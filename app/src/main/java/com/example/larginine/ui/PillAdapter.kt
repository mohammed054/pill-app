package com.example.larginine.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.larginine.data.Pill
import com.example.larginine.databinding.ItemPillBinding
import java.util.Locale

class PillAdapter(
    private val onDeleteClick: (Pill) -> Unit
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
            binding.deleteButton.setOnClickListener {
                onDeleteClick(pill)
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
