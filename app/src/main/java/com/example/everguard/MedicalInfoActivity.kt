package com.example.everguard

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
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
            val intent = Intent(this, EmergencyContactsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSpinners() {
        val medicalConditions = arrayOf("Medical Conditions", "Diabetes", "Hypertension", "Asthma", "None")
        val bloodTypes = arrayOf("Blood Type", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val mobilityStatus = arrayOf("Mobility Status", "Walking", "Wheelchair", "Bedridden")

        setupSpinner(binding.medicalConditionsSpinner, medicalConditions)
        setupSpinner(binding.bloodTypeSpinner, bloodTypes)
        setupSpinner(binding.mobilityStatusSpinner, mobilityStatus)
    }

    private fun setupSpinner(spinner: android.widget.Spinner, options: Array<String>) {
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent)
                val tv = view as android.widget.TextView
                if (position == 0) {
                    tv.setTextColor(android.graphics.Color.GRAY)
                } else {
                    tv.setTextColor(android.graphics.Color.BLACK)
                }
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
}
