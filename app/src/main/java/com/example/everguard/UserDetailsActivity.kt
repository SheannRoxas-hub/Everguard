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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val city = binding.cityInput.text.toString()

            // 1. Basic Validation
            if (firstName.isEmpty() || lastName.isEmpty() || birthday.isEmpty() || gender.isEmpty() || city.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please fill in all required fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            //2. Logic to move to the next screen
            val intent = Intent(this, MedicalInfoActivity::class.java).apply {
                putExtra("FIRST_NAME", firstName)
                putExtra("LAST_NAME", lastName)
                putExtra("GENDER", gender)
                putExtra("BIRTHDAY", birthday)
                putExtra("CITY", city)
            }
            startActivity(intent)
        }
    }
    private fun setupSpinners() {
        // 1. Setup Dropdowns (Exposed Dropdown Menus)
        // Ensure your XML uses AutoCompleteTextView inside a TextInputLayout
        val genderOptions = arrayOf("Male", "Female", "Other")
        val cityOptions = arrayOf("Manila", "Quezon City", "Caloocan City", "Cebu City", "Davao City")

        setupDropdown(binding.genderInput, genderOptions)
        setupDropdown(binding.cityInput, cityOptions)

        // 2. Setup Date Picker
        setupDatePicker()
    }

    private fun setupDropdown(view: AutoCompleteTextView, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        view.setAdapter(adapter)
    }
    private fun setupDatePicker() {
        binding.birthdayInput.setOnClickListener {
            // Build the Date Picker
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date of Birth")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.show(supportFragmentManager, "BIRTHDAY_PICKER")

            // Handle selection
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format =
                    SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                binding.birthdayInput.setText(format.format(calendar.time))
            }
        }
    }


}
