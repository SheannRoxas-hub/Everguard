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
import com.google.firebase.database.FirebaseDatabase

class EmergencyContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmergencyContactsBinding
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

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

        // BASIC VALIDATION
        if (contactFirstName.isEmpty() || contactLastName.isEmpty() ||
            relationship.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Please fill in all contact details", Toast.LENGTH_SHORT).show()
            return
        }

        // Get senior details from previous activity
        val seniorFirstName = intent.getStringExtra("FIRST_NAME") ?: ""
        val seniorLastName = intent.getStringExtra("LAST_NAME") ?: ""
        val gender = intent.getStringExtra("GENDER") ?: ""
        val birthday = intent.getStringExtra("BIRTHDAY") ?: ""

        // Save to Firebase
        saveToFirebase(
            seniorFirstName, seniorLastName, gender, birthday,
            contactFirstName, contactLastName, relationship, mobile
        )
    }

    private fun saveToFirebase(
        seniorFirstName: String,
        seniorLastName: String,
        gender: String,
        birthday: String,
        contactFirstName: String,
        contactLastName: String,
        relationship: String,
        mobile: String
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance(databaseUrl).getReference("users").child(userId)

        // Create senior details object
        val seniorDetails = SeniorDetails(
            firstName = seniorFirstName,
            lastName = seniorLastName,
            gender = gender,
            birthday = birthday
        )

        // Create emergency contact object
        val emergencyContact = EmergencyContact(
            firstName = contactFirstName,
            lastName = contactLastName,
            relationship = relationship,
            mobile = mobile
        )

        // Save both to Firebase
        val updates = hashMapOf<String, Any>(
            "seniorDetails" to seniorDetails,
            "emergencyContact" to emergencyContact
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Details saved successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to next screen
                val intent = Intent(this, DevicePairingActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDropdowns() {
        val relationships = arrayOf("Parent", "Sibling", "Spouse", "Child", "Friend", "Other")
        setupAdapter(binding.relationshipInput, relationships)
    }

    private fun setupAdapter(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
    }
}