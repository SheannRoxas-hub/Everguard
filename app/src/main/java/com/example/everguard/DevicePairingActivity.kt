package com.example.everguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.everguard.databinding.ActivityDevicePairingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DevicePairingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDevicePairingBinding
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    // Store emergency contacts passed from previous activity
    private var emergencyContacts: MutableMap<String, EmergencyContact> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevicePairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Retrieve emergency contacts from intent
        val contactsJson = intent.getStringExtra("emergencyContacts")
        if (contactsJson != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableMap<String, EmergencyContact>>() {}.type
            emergencyContacts = gson.fromJson(contactsJson, type)
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.proceedBtn.setOnClickListener {
            pairDevice()
        }
    }

    private fun pairDevice() {
        val deviceId = binding.deviceIdInput.text.toString().trim()

        if (deviceId.isEmpty()) {
            Toast.makeText(this, "Please enter a Device ID", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Create device with emergency contacts
        val device = Device(
            deviceId = deviceId,
            batteryStatus = "100%",
            sensitivity = 3,
            isSos = false,
            emergencyContacts = emergencyContacts
        )

        val deviceRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)

        // Save device with emergency contacts
        deviceRef.setValue(device)
            .addOnSuccessListener {
                // Update user's deviceId
                val userRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("users").child(userId)

                userRef.child("deviceId").setValue(deviceId)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Device paired successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate to HomeNotificationsContactsActivity (which contains the fragments)
                        val intent = Intent(this, HomeNotificationsContactsActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to update user: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to pair device: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}