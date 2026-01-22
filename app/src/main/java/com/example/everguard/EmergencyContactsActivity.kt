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
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

class EmergencyContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmergencyContactsBinding
    private lateinit var auth: FirebaseAuth

    // Store contacts temporarily
    private val emergencyContacts = mutableMapOf<String, EmergencyContact>()
    private var currentContactNumber = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
        val contactFirstName = binding.firstNameInput.text.toString().trim()
        val contactLastName = binding.lastNameInput.text.toString().trim()
        val relationship = binding.relationshipInput.text.toString()
        val mobile = binding.mobileNumberInput.text.toString().trim()

        // Validation
        if (contactFirstName.isEmpty() || contactLastName.isEmpty() ||
            relationship.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Please fill in all contact details", Toast.LENGTH_SHORT).show()
            return
        }

        // Add current contact to the map
        val contact = EmergencyContact(
            fname = contactFirstName,
            lname = contactLastName,
            relationship = relationship,
            contact = mobile
        )

        emergencyContacts["emgperson$currentContactNumber"] = contact

        // Ask if they want to add more contacts (max 3)
        if (currentContactNumber < 3) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Another Contact?")
                .setMessage("You can add up to ${3 - currentContactNumber} more emergency contact(s).")
                .setPositiveButton("Add Another") { _, _ ->
                    currentContactNumber++
                    clearInputs()
                }
                .setNegativeButton("Continue") { _, _ ->
                    proceedToDevicePairing()
                }
                .show()
        } else {
            proceedToDevicePairing()
        }
    }

    private fun clearInputs() {
        binding.firstNameInput.text?.clear()
        binding.lastNameInput.text?.clear()
        binding.relationshipInput.text?.clear()
        binding.mobileNumberInput.text?.clear()
        Toast.makeText(this, "Enter contact ${currentContactNumber} details", Toast.LENGTH_SHORT).show()
    }

    private fun proceedToDevicePairing() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Pass the emergency contacts to DevicePairingActivity
        val intent = Intent(this, DevicePairingActivity::class.java)

        // Convert the map to JSON string to pass via intent
        val gson = Gson()
        val contactsJson = gson.toJson(emergencyContacts)
        intent.putExtra("emergencyContacts", contactsJson)

        startActivity(intent)
    }

    private fun setupDropdowns() {
        val relationships = arrayOf("Parent", "Sibling", "Spouse", "Child", "Friend", "Doctor", "Guardian", "Other")
        setupAdapter(binding.relationshipInput, relationships)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
    }
}