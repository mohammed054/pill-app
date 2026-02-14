package com.example.larginine.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.larginine.R
import com.example.larginine.data.NotificationPhrase
import com.example.larginine.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: PillViewModel by viewModels()
    private lateinit var customPhraseAdapter: PhraseAdapter
    private lateinit var defaultPhraseAdapter: PhraseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupButtons()
        observePhrases()
    }

    private fun setupRecyclerViews() {
        // Custom phrases adapter (with delete button)
        customPhraseAdapter = PhraseAdapter(
            showDelete = true,
            onToggle = { phrase -> viewModel.togglePhraseEnabled(phrase) },
            onDelete = { phrase -> viewModel.deletePhrase(phrase) }
        )
        binding.phrasesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.phrasesRecyclerView.adapter = customPhraseAdapter

        // Default phrases adapter (no delete button)
        defaultPhraseAdapter = PhraseAdapter(
            showDelete = false,
            onToggle = { phrase -> viewModel.togglePhraseEnabled(phrase) },
            onDelete = { }
        )
        binding.defaultPhrasesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.defaultPhrasesRecyclerView.adapter = defaultPhraseAdapter
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addPhraseButton.setOnClickListener {
            showAddPhraseDialog()
        }
    }

    private fun observePhrases() {
        lifecycleScope.launch {
            viewModel.allPhrases.collectLatest { phrases ->
                val customPhrases = phrases.filter { it.isCustom }
                val defaultPhrases = phrases.filter { !it.isCustom }

                customPhraseAdapter.submitList(customPhrases)
                defaultPhraseAdapter.submitList(defaultPhrases)

                binding.noPhrasesText.visibility = if (customPhrases.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddPhraseDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.enter_phrase)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_phrase)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val phrase = editText.text.toString().trim()
                if (phrase.isNotEmpty()) {
                    viewModel.addCustomPhrase(phrase)
                    Toast.makeText(this, R.string.phrase_added, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
