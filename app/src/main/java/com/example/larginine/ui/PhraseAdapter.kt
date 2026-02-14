package com.example.larginine.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.larginine.data.NotificationPhrase
import com.example.larginine.databinding.ItemPhraseBinding

class PhraseAdapter(
    private val showDelete: Boolean,
    private val onToggle: (NotificationPhrase) -> Unit,
    private val onDelete: (NotificationPhrase) -> Unit
) : ListAdapter<NotificationPhrase, PhraseAdapter.PhraseViewHolder>(PhraseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhraseViewHolder {
        val binding = ItemPhraseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhraseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhraseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhraseViewHolder(
        private val binding: ItemPhraseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(phrase: NotificationPhrase) {
            binding.phraseText.text = phrase.phrase
            binding.phraseCheckBox.isChecked = phrase.isEnabled

            binding.phraseCheckBox.setOnClickListener {
                onToggle(phrase)
            }

            binding.deleteButton.visibility = if (showDelete) View.VISIBLE else View.GONE
            binding.deleteButton.setOnClickListener {
                onDelete(phrase)
            }
        }
    }

    class PhraseDiffCallback : DiffUtil.ItemCallback<NotificationPhrase>() {
        override fun areItemsTheSame(oldItem: NotificationPhrase, newItem: NotificationPhrase): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationPhrase, newItem: NotificationPhrase): Boolean {
            return oldItem == newItem
        }
    }
}
