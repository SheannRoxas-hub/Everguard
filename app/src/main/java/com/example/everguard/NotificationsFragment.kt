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
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.everguard.databinding.FragmentNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var notifications = mutableListOf<Notification>()
    private lateinit var notificationAdapter: NotificationAdapter
    private var deviceId: String = ""
    private var lastNotifiedDate: String = ""
    private var recipientPhoneNumber: String = "" // CHANGED: Use recipient number instead
    private val CALL_PERMISSION_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        getUserDeviceId()

        // Dim Overlay
        binding.dimOverlay2.setOnClickListener {
            binding.alertDetailsPopup2.visibility = View.GONE
            binding.dimOverlay2.visibility = View.GONE
        }

        // Call/SMS logic - Call the recipient (care person)
        binding.btnCall2.setOnClickListener {
            if (recipientPhoneNumber.isNotEmpty()) {
                makePhoneCall(recipientPhoneNumber)
            } else {
                Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSms2.setOnClickListener {
            if (recipientPhoneNumber.isNotEmpty()) {
                sendSMS(recipientPhoneNumber, "Emergency alert from Everguard app!")
            } else {
                Toast.makeText(requireContext(), "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        // Kebab Menu Logic
        binding.homeKebabMenu.setOnClickListener { v ->
            showPopupMenu(v)
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(notifications) { notification ->
            showNotificationDetails(notification)
        }

        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }

    private fun getUserDeviceId() {
        val userId = auth.currentUser?.uid ?: return

        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceId = snapshot.child("deviceId").getValue(String::class.java) ?: ""

                if (deviceId.isNotEmpty()) {
                    loadNotifications()
                    loadRecipientContact() // CHANGED: Load recipient contact instead
                } else {
                    Toast.makeText(requireContext(), "No device paired", Toast.LENGTH_SHORT).show()
                    binding.notificationsRecyclerView.visibility = View.GONE
                    binding.emptyStateText.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load device info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // CHANGED: Load recipient (care person) contact
    private fun loadRecipientContact() {
        val userId = auth.currentUser?.uid ?: return

        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                val user = snapshot.getValue(User::class.java)
                user?.carePerson?.let { carePerson ->
                    recipientPhoneNumber = carePerson.contact
                    android.util.Log.d("NotificationsFragment", "Recipient contact loaded: $recipientPhoneNumber")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                android.util.Log.e("NotificationsFragment", "Failed to load recipient contact: ${error.message}")
            }
        })
    }

    private fun loadNotifications() {
        if (deviceId.isEmpty()) return

        val notifsRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("notifs")

        notifsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                notifications.clear()

                for (notifSnapshot in snapshot.children) {
                    val type = notifSnapshot.child("type").getValue(String::class.java) ?: ""
                    val title = notifSnapshot.child("title").getValue(String::class.java) ?: ""
                    val description = notifSnapshot.child("description").getValue(String::class.java) ?: ""
                    val location = notifSnapshot.child("location").getValue(String::class.java) ?: ""
                    val date = notifSnapshot.child("date").getValue(String::class.java) ?: ""
                    val notifDeviceId = notifSnapshot.child("deviceId").getValue(String::class.java) ?: ""

                    if (notifDeviceId == deviceId) {
                        val notification = Notification(type, title, description, location, date, deviceId = notifDeviceId, readBy = listOf())
                        notifications.add(notification)

                        val notificationDate = parseNotificationDate(date)
                        val diffInSeconds = (Date().time - notificationDate.time) / 1000

                        if (diffInSeconds < 30 && date != lastNotifiedDate) {
                            lastNotifiedDate = date
                            val notificationHelper = NotificationHelper(requireContext())
                            notificationHelper.sendLocalNotification(title, description)
                        }
                    }
                }

                notifications.sortByDescending { parseNotificationDate(it.date) }

                if (notifications.isEmpty()) {
                    binding.notificationsRecyclerView.visibility = View.GONE
                    binding.emptyStateText.visibility = View.VISIBLE
                } else {
                    binding.notificationsRecyclerView.visibility = View.VISIBLE
                    binding.emptyStateText.visibility = View.GONE
                }

                notificationAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showNotificationDetails(notification: Notification) {
        // Update popup with notification details
        binding.AlertTitlePop2.text = notification.title
        binding.AlertTimePop2.text = getTimeAgo(notification.date)
        binding.AlertDescriptionPop2.text = notification.description

        // Display location as clickable link with user-friendly text
        if (notification.location.isNotEmpty()) {
            binding.AlertLocationPop2.text = "View Location on Map"
            binding.AlertLocationPop2.setTextColor(0xFF0066CC.toInt())
            binding.AlertLocationPop2.paintFlags = binding.AlertLocationPop2.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            binding.AlertLocationPop2.setOnClickListener {
                openMapLocation(notification.location)
            }
        } else {
            binding.AlertLocationPop2.text = "Location not available"
            binding.AlertLocationPop2.setTextColor(0xFF666666.toInt())
            binding.AlertLocationPop2.paintFlags = binding.AlertLocationPop2.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
            binding.AlertLocationPop2.setOnClickListener(null)
        }

        // Check if notification is older than 1 day
        val isOld = isNotificationOld(notification.date)

        // Set color based on type and age
        val color = if (isOld) {
            0xFF808080.toInt()
        } else {
            when {
                notification.type.contains("Fall", ignoreCase = true) ||
                        notification.type.contains("Accident", ignoreCase = true) -> 0xFFDC3030.toInt()
                notification.type.contains("SOS", ignoreCase = true) -> 0xFFDC3030.toInt()
                notification.type.contains("Battery", ignoreCase = true) -> 0xFF910000.toInt()
                else -> 0xFFFF914D.toInt()
            }
        }

        binding.AlertBellPop2.setColorFilter(color)
        binding.AlertTitlePop2.setTextColor(color)

        binding.alertDetailsPopup2.visibility = View.VISIBLE
        binding.dimOverlay2.visibility = View.VISIBLE
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

    private fun openMapLocation(locationUrl: String) {
        try {
            var url = locationUrl
                .trim()
                .replace("\\\"", "\"")
                .replace("\"", "")
                .replace("\\", "")
                .replace("\\s+".toRegex(), "")

            android.util.Log.d("MapLocation", "Cleaned URL: $url")

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            android.util.Log.d("MapLocation", "Final URL: $url")

            if (url.isEmpty() || url == "https://") {
                Toast.makeText(requireContext(), "Invalid location URL", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.parse(url)

            if (url.contains("google.com/maps") || url.contains("maps.google.com")) {
                val latLngPattern = "[@?](-?\\d+\\.\\d+),(-?\\d+\\.\\d+)".toRegex()
                val match = latLngPattern.find(url)

                if (match != null) {
                    val lat = match.groupValues[1]
                    val lng = match.groupValues[2]

                    val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))
                    geoIntent.setPackage("com.google.android.apps.maps")

                    if (geoIntent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(geoIntent)
                        return
                    }
                }
            }

            val mapsIntent = Intent(Intent.ACTION_VIEW, uri)
            mapsIntent.setPackage("com.google.android.apps.maps")

            if (mapsIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapsIntent)
                return
            }

            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            if (browserIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(browserIntent)
                return
            }

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

        return diffInDays >= 1
    }

    private fun parseNotificationDate(dateStr: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(dateStr) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
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