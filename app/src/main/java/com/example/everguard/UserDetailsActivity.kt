package com.example.everguard

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.everguard.databinding.ActivityUserDetailsBinding

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
            // Handle next action
        }
    }

    private fun setupSpinners() {
        // Sample data for spinners
        val genderOptions = arrayOf("Gender", "Male", "Female", "Other")
        val cityOptions = arrayOf("City", "Manila", "Quezon City", "Cebu City", "Davao City")
        val provinceOptions = arrayOf("Province", "Metro Manila", "Cebu", "Davao del Sur")
        val dobOptions = arrayOf("Date of Birth") // Usually a DatePicker would be better, but following the UI

        setupSpinner(binding.genderSpinner, genderOptions)
        setupSpinner(binding.citySpinner, cityOptions)
        setupSpinner(binding.provinceSpinner, provinceOptions)
        setupSpinner(binding.dobSpinner, dobOptions)
    }

    private fun setupSpinner(spinner: android.widget.Spinner, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
}
