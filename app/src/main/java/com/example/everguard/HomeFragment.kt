package com.example.everguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.example.everguard.databinding.FragmentHomeBinding
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var recipientPhoneNumber: String = ""
    private var deviceId: String = ""
    private var firstEmergencyContactNumber: String = ""
    private var currentSensitivity: Int = 2
    private val CALL_PERMISSION_CODE = 100
    private val SMS_PERMISSION_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load user data from Firebase
        loadUserData()

        // Load senior details
        loadSeniorDetails()

        // Load device data (battery, sensitivity)
        loadDeviceData()

        // Load most recent notification
        loadRecentNotification()

        // Accident Alert card clicked - opens popup
        binding.AccidentAlert.setOnClickListener {
            binding.alertDetailsPopup.visibility = View.VISIBLE
            binding.dimOverlay.visibility = View.VISIBLE
        }

        // Close popup
        binding.dimOverlay.setOnClickListener {
            binding.alertDetailsPopup.visibility = View.GONE
            binding.dimOverlay.visibility = View.GONE
        }

        // Call button in alert popup
        binding.btnCall.setOnClickListener {
            if (recipientPhoneNumber.isNotEmpty()) {
                makePhoneCall(recipientPhoneNumber)
            } else {
                Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        // SMS button in alert popup
        binding.btnSms.setOnClickListener {
            if (recipientPhoneNumber.isNotEmpty()) {
                sendSMS(recipientPhoneNumber, "Emergency alert from Everguard app!")
            } else {
                Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        // Phone icon in Recipient card - calls recipient
        binding.PhoneIcon.setOnClickListener {
            if (recipientPhoneNumber.isNotEmpty()) {
                makePhoneCall(recipientPhoneNumber)
            } else {
                Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        // Sensitivity slider listener
        binding.sensitivitySlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val sensitivityLevel = value.toInt()
                updateSensitivityInDatabase(sensitivityLevel)
            }
        }

        binding.homeKebabMenu.setOnClickListener { v ->
            showPopupMenu(v)
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return // Add this check

                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.helloUser.text = "Hello, ${it.username}!"
                    deviceId = it.deviceId
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return // Add this check

                Toast.makeText(
                    requireContext(),
                    "Failed to load user data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadSeniorDetails() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return // Add this check

                val user = snapshot.getValue(User::class.java)
                user?.carePerson?.let { carePerson ->
                    binding.recipientName.text = "${carePerson.fname} ${carePerson.lname}"
                    recipientPhoneNumber = carePerson.contact
                    val age = calculateAge(carePerson.bdate)
                    binding.recipientAge.text = "Age: $age"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return // Add this check

                Toast.makeText(
                    requireContext(),
                    "Failed to load senior details: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadDeviceData() {
        val userId = auth.currentUser?.uid ?: return

        // Get deviceId from user
        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                deviceId = user?.deviceId ?: ""

                if (deviceId.isNotEmpty() && deviceId != "") {
                    // Load device data
                    loadDeviceInfo(deviceId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load device ID", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadDeviceInfo(deviceId: String) {
        val deviceRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)

        deviceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return // Add this check

                val device = snapshot.getValue(Device::class.java)
                device?.let {
                    binding.BatteryPercent.text = it.batteryStatus
                    updateBatteryIcon(it.batteryStatus)
                    currentSensitivity = it.sensitivity
                    binding.sensitivitySlider.value = it.sensitivity.toFloat()
                    loadFirstEmergencyContact(it.emergencyContacts)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return // Add this check

                Toast.makeText(requireContext(), "Failed to load device data", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun updateBatteryIcon(batteryStatus: String) {
        // Extract percentage number from string like "85%" or "100%"
        val percentage = batteryStatus.replace("%", "").toIntOrNull() ?: 100

        when {
            percentage > 75 -> binding.BatteryIcon.setImageResource(R.drawable.ic_battery_full)
            percentage > 50 -> binding.BatteryIcon.setImageResource(R.drawable.ic_battery_full)
            percentage > 25 -> binding.BatteryIcon.setImageResource(R.drawable.ic_battery)
            else -> binding.BatteryIcon.setImageResource(R.drawable.ic_battery)
        }
    }

    private fun loadFirstEmergencyContact(contacts: Map<String, EmergencyContact>) {
        // Get the first emergency contact (emgperson1)
        val firstContact = contacts["emgperson1"]
        firstEmergencyContactNumber = firstContact?.contact ?: ""
    }

    private fun updateSensitivityInDatabase(sensitivity: Int) {
        if (deviceId.isEmpty()) {
            Toast.makeText(requireContext(), "No device paired", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("devices").child(deviceId)

        deviceRef.child("sensitivity").setValue(sensitivity)
            .addOnSuccessListener {
                currentSensitivity = sensitivity
                val levelText = when (sensitivity) {
                    1 -> "Low"
                    2 -> "Medium"
                    3 -> "High"
                    else -> "Medium"
                }
                Toast.makeText(
                    requireContext(),
                    "Sensitivity set to $levelText",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Failed to update sensitivity: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadRecentNotification() {
        val userId = auth.currentUser?.uid ?: return

        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return // Add this check

                val deviceId = userSnapshot.child("deviceId").getValue(String::class.java) ?: ""

                if (deviceId.isEmpty()) {
                    binding.AccidentAlert.visibility = View.GONE
                    binding.noAlertsText.visibility = View.VISIBLE
                    return
                }

                val notifsRef = FirebaseDatabase.getInstance(databaseUrl)
                    .getReference("notifs")

                notifsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!isAdded || _binding == null) return // Add this check

                        val notifications = mutableListOf<Notification>()

                        for (notifSnapshot in snapshot.children) {
                            val type = notifSnapshot.child("type").getValue(String::class.java) ?: ""
                            val title = notifSnapshot.child("title").getValue(String::class.java) ?: ""
                            val description = notifSnapshot.child("description").getValue(String::class.java) ?: ""
                            val location = notifSnapshot.child("location").getValue(String::class.java) ?: ""
                            val date = notifSnapshot.child("date").getValue(String::class.java) ?: ""
                            val notifDeviceId = notifSnapshot.child("deviceId").getValue(String::class.java) ?: ""

                            if (notifDeviceId == deviceId) {
                                val notification = Notification(
                                    type = type,
                                    title = title,
                                    description = description,
                                    location = location,
                                    date = date,
                                    deviceId = notifDeviceId,
                                    readBy = listOf()
                                )
                                notifications.add(notification)
                            }
                        }

                        notifications.sortByDescending { parseNotificationDate(it.date) }

                        if (notifications.isNotEmpty()) {
                            binding.AccidentAlert.visibility = View.VISIBLE
                            binding.noAlertsText.visibility = View.GONE
                            displayRecentAlert(notifications[0])
                        } else {
                            binding.AccidentAlert.visibility = View.GONE
                            binding.noAlertsText.visibility = View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (!isAdded || _binding == null) return // Add this check

                        Toast.makeText(
                            requireContext(),
                            "Failed to load notifications: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return // Add this check

                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayRecentAlert(notification: Notification) {
        // Check if notification is older than 1 day
        val isOld = isNotificationOld(notification.date)

        // Set color based on type and age
        val color = if (isOld) {
            0xFF808080.toInt() // Grey for old notifications
        } else {
            when {
                notification.type.contains("Fall", ignoreCase = true) ||
                        notification.type.contains("Accident", ignoreCase = true) -> 0xFFFF914D.toInt() // Yellow/Orange
                notification.type.contains("SOS", ignoreCase = true) -> 0xFFDC3030.toInt() // Red
                notification.type.contains("Battery", ignoreCase = true) -> 0xFF910000.toInt() // Dark Red
                else -> 0xFFFF914D.toInt() // Default yellow/orange
            }
        }

        // Update alert title with color
        binding.alertTitle.text = notification.title
        binding.alertTitle.setTextColor(color)

        // Update alert description
        binding.alertDescription.text = notification.description

        // Update time ago
        binding.alertTime.text = getTimeAgo(notification.date)

        // Update popup content
        binding.AlertTitlePop.text = notification.title
        binding.AlertTitlePop.setTextColor(color)
        binding.AlertDescriptionPop.text = notification.description
        binding.AlertTimePop.text = getTimeAgo(notification.date)

        // Make location clickable and display properly
        if (notification.location.isNotEmpty()) {
            // Display shorter, user-friendly text
            binding.AlertLocationPop.text = "View Location on Map"
            binding.AlertLocationPop.setTextColor(0xFF0066CC.toInt()) // Blue color for link
            binding.AlertLocationPop.paintFlags = binding.AlertLocationPop.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            binding.AlertLocationPop.setOnClickListener {
                openMapLocation(notification.location)
            }
        } else {
            binding.AlertLocationPop.text = "Location not available"
            binding.AlertLocationPop.setTextColor(0xFF666666.toInt())
            binding.AlertLocationPop.paintFlags = binding.AlertLocationPop.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
            binding.AlertLocationPop.setOnClickListener(null)
        }

        // Set alert icon based on type with color filter
        val iconRes = when {
            notification.type.contains("Fall", ignoreCase = true) ||
                    notification.type.contains("Accident", ignoreCase = true) -> R.drawable.notifications_icon
            notification.type.contains("SOS", ignoreCase = true) -> R.drawable.notifications_icon
            notification.type.contains("Battery", ignoreCase = true) -> R.drawable.ic_battery
            else -> R.drawable.notifications_icon
        }
        binding.alertIcon.setImageResource(iconRes)
        binding.alertIcon.setColorFilter(color)
        binding.AlertBellPop.setImageResource(iconRes)
        binding.AlertBellPop.setColorFilter(color)
    }


    private fun openMapLocation(locationUrl: String) {
        try {
            // Remove escape characters and trim
            var url = locationUrl
                .trim()
                .replace("\\\"", "\"")  // Remove escaped quotes
                .replace("\"", "")       // Remove quotes
                .replace("\\", "")       // Remove backslashes
                .replace("\\s+".toRegex(), "") // Remove whitespace

            android.util.Log.d("MapLocation", "Cleaned URL: $url")

            // Ensure URL has https://
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            android.util.Log.d("MapLocation", "Final URL: $url")

            // Validate URL
            if (url.isEmpty() || url == "https://") {
                Toast.makeText(requireContext(), "Invalid location URL", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.parse(url)

            // Method 1: Try to open in gmaps app with geo intent
            if (url.contains("google.com/maps") || url.contains("maps.google.com")) {
                // Extract coordinates if possible
                val latLngPattern = "[@?](-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                val match = latLngPattern.find(url)

                if (match != null) {
                    val lat = match.groupValues[1]
                    val lng = match.groupValues[2]

                    // Try geo intent first
                    val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))
                    geoIntent.setPackage("com.google.android.apps.maps")

                    if (geoIntent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(geoIntent)
                        return
                    }
                }
            }

            // Method 2: Open gmaps urls directly in maps
            val mapsIntent = Intent(Intent.ACTION_VIEW, uri)
            mapsIntent.setPackage("com.google.android.apps.maps")

            if (mapsIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapsIntent)
                return
            }

            // Method 3: Open in any browser
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            if (browserIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(browserIntent)
                return
            }

            // Method 4: Create chooser as last resort
            val chooserIntent = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Open location with")
            startActivity(chooserIntent)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open location: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("MapLocation", "Error opening location", e)
        }
    }

    private fun isNotificationOld(dateStr: String): Boolean {
        val notificationDate = parseNotificationDate(dateStr)
        val now = Date()
        val diffInMillis = now.time - notificationDate.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return diffInDays >= 1 // 1 day or older
    }

    private fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_CODE
            )
        } else {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:$phoneNumber")
            startActivity(callIntent)
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsIntent = Intent(Intent.ACTION_VIEW)
            smsIntent.data = Uri.parse("sms:$phoneNumber")
            smsIntent.putExtra("sms_body", message)
            startActivity(smsIntent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to open SMS app: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CALL_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (recipientPhoneNumber.isNotEmpty()) {
                        makePhoneCall(recipientPhoneNumber)
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Call permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun parseNotificationDate(dateStr: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(dateStr) ?: Date(0)
        } catch (e: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                format.parse(dateStr) ?: Date(0)
            } catch (e: Exception) {
                Date(0)
            }
        }
    }

    private fun getTimeAgo(dateStr: String): String {
        val notificationDate = parseNotificationDate(dateStr)
        val now = Date()
        val diffInMillis = now.time - notificationDate.time

        val seconds = diffInMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }

    private fun calculateAge(birthday: String): Int {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = dateFormat.parse(birthday) ?: return 0

            val birthCalendar = Calendar.getInstance()
            birthCalendar.time = birthDate

            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            return age
        } catch (e: Exception) {
            return 0
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.home_kebab, popup.menu)

        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_profile -> {
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}