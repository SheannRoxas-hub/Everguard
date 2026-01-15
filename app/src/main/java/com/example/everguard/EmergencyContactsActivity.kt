package com.example.everguard

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.everguard.databinding.ActivityEmergencyContactsBinding

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupDropdowns()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.nextBtn.setOnClickListener {
            handleValidationAndNext()
        }
    }

    private fun handleValidationAndNext() {
        val name = binding.firstNameInput.text.toString().trim()
        val phone = binding.mobileNumberInput.text.toString().trim()
        val relationship = binding.relationshipInput.text.toString()

        // BASIC VALIDATION
        if (name.isEmpty() || phone.isEmpty() || relationship.isEmpty()) {
            Toast.makeText(this, "Please fill in all contact details", Toast.LENGTH_SHORT).show()
            return
        }

        // Navigate to next screen (No putExtra as requested)
        val intent = Intent(this, DevicePairingActivity::class.java)
        startActivity(intent)
    }

    private fun setupDropdowns() {
        val relationships = arrayOf("Parent", "Sibling", "Spouse", "Child", "Friend", "Other")

        // Use the helper to set up the AutoCompleteTextView
        setupAdapter(binding.relationshipInput, relationships)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)

        // Ensures the dropdown opens immediately when the user taps the field
        view.setOnClickListener { view.showDropDown() }
    }
}


