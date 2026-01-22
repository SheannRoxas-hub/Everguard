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
import com.example.everguard.databinding.ActivityUserDetailsBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSpinners()

        binding.nextBtn.setOnClickListener {
            val firstName = binding.firstNameInput.text.toString().trim()
            val lastName = binding.lastNameInput.text.toString().trim()
            val gender = binding.genderInput.text.toString()
            val birthday = binding.birthdayInput.text.toString()
            val mobileNumber = binding.mobileNumberInput.text.toString().trim()

            // Validation
            if (firstName.isEmpty() || lastName.isEmpty() || birthday.isEmpty() ||
                gender.isEmpty() || mobileNumber.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please fill in all required fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Save care person details to Firebase
            saveCarePersonDetails(firstName, lastName, birthday, gender, mobileNumber)
        }
    }

    private fun saveCarePersonDetails(
        firstName: String,
        lastName: String,
        birthday: String,
        gender: String,
        contact: String
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId).child("carePerson")

        val carePerson = CarePerson(
            fname = firstName,
            lname = lastName,
            bdate = birthday,
            gender = gender.lowercase(),
            contact = contact
        )

        database.setValue(carePerson)
            .addOnSuccessListener {
                Toast.makeText(this, "Care person details saved!", Toast.LENGTH_SHORT).show()

                // Navigate to Emergency Contacts
                val intent = Intent(this, EmergencyContactsActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSpinners() {
        val genderOptions = arrayOf("Male", "Female", "Other")
        setupDropdown(binding.genderInput, genderOptions)
        setupDatePicker()
    }

    private fun setupDropdown(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        binding.birthdayInput.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date of Birth")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.show(supportFragmentManager, "BIRTHDAY_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.birthdayInput.setText(format.format(calendar.time))
            }
        }
    }
}