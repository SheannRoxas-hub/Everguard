package com.example.everguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.everguard.databinding.ActivityDevicePairingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DevicePairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDevicePairingBinding
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDevicePairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.proceedBtn.setOnClickListener {
            handleDevicePairing()
        }
    }

    private fun handleDevicePairing() {
        val deviceId = binding.deviceIdInput.text.toString().trim()

        // Validation
        if (deviceId.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter a Device Identification Number",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if device exists in database
        val deviceRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)

        deviceRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Device exists, link it to user
                linkDeviceToUser(userId, deviceId)
            } else {
                // Device doesn't exist, create it
                createAndLinkDevice(userId, deviceId)
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(
                this,
                "Error checking device: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun linkDeviceToUser(userId: String, deviceId: String) {
        // Update user's deviceId
        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.child("deviceId").setValue(deviceId)
            .addOnSuccessListener {
                Toast.makeText(this, "Device paired successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to Home
                val intent = Intent(this, HomeNotificationsContactsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to link device: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun createAndLinkDevice(userId: String, deviceId: String) {
        // Create new device with default values
        val device = Device(
            deviceId = deviceId,
            batteryStatus = "100%",
            sensitivity = 3,
            isSos = false,
            emergencyContacts = mapOf()
        )

        val deviceRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)

        deviceRef.setValue(device)
            .addOnSuccessListener {
                // Device created, now link to user
                linkDeviceToUser(userId, deviceId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to create device: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}