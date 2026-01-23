package com.example.everguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
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

        // Call/SMS logic
        binding.btnCall2.setOnClickListener {
            Toast.makeText(requireContext(), "Calling emergency contact...", Toast.LENGTH_SHORT).show()
        }

        binding.btnSms2.setOnClickListener {
            Toast.makeText(requireContext(), "Sending SMS...", Toast.LENGTH_SHORT).show()
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

    private fun loadNotifications() {
        if (deviceId.isEmpty()) return

        val notifsRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("notifs")

        notifsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return // Add this check

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
                if (!isAdded || _binding == null) return // Add this check

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
            binding.AlertLocationPop2.setTextColor(0xFF0066CC.toInt()) // Blue color for link
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

        binding.AlertBellPop2.setColorFilter(color)
        binding.AlertTitlePop2.setTextColor(color)

        binding.alertDetailsPopup2.visibility = View.VISIBLE
        binding.dimOverlay2.visibility = View.VISIBLE
    }

    private fun openMapLocation(locationUrl: String) {
        try {
            var url = locationUrl.trim()

            // Remove any whitespace or newlines
            url = url.replace("\\s+".toRegex(), "")

            // Ensure URL has proper scheme
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            // Create intent with the URL
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)

            // Try to start the activity
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No app available to open this link", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid URL: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


    private fun isNotificationOld(dateStr: String): Boolean {
        val notificationDate = parseNotificationDate(dateStr)
        val now = Date()
        val diffInMillis = now.time - notificationDate.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return diffInDays >= 1 // 1 day or older
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