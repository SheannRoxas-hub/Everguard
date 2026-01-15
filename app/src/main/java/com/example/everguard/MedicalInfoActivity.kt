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
import com.example.everguard.databinding.ActivityMedicalInfoBinding

class MedicalInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicalInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMedicalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupSpinners()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.nextBtn.setOnClickListener {
            handleValidationAndNext()
        }
    }
    private fun handleValidationAndNext() {
        // Collect text from AutoCompleteTextViews
        val condition = binding.coonditionInput.text.toString()
        val bloodType = binding.bloodTypeInput.text.toString()
        val mobility = binding.mobilityInput.text.toString()

        // BASIC VALIDATION
        if (condition.isEmpty() || bloodType.isEmpty() || mobility.isEmpty()) {
            Toast.makeText(this, "Please select all medical details", Toast.LENGTH_SHORT).show()
            return
        }

        // Logic to move to next screen (Replace with your actual next Activity)
        val intent = Intent(this, EmergencyContactsActivity::class.java)
        startActivity(intent)
    }


    private fun setupSpinners() {
        val medicalConditions = arrayOf(
            "Medical Conditions",
            "Diabetes",
            "Hypertension",
            "Asthma",
            "Heart Condition",
            "None"
        )
        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val mobilityStatus =
            arrayOf("Walking", "Wheelchair", "Bedridden", "Cane")

        // Use the new setupDropdown function for all your AutoCompleteTextViews
        setupDropdown(binding.coonditionInput, medicalConditions)
        setupDropdown(binding.bloodTypeInput, bloodTypes)
        setupDropdown(binding.mobilityInput, mobilityStatus)
    }


    // Updated to use AutoCompleteTextView instead of Spinner
    private fun setupDropdown(view: AutoCompleteTextView, options: Array<String>) {
        // We use the standard system layout for the list items
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)

        // Optional: This ensures the dropdown opens when the user clicks the box
        view.setOnClickListener { view.showDropDown() }
    }
}
