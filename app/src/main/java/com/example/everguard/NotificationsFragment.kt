package com.example.everguard

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.everguard.databinding.FragmentNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val databaseUrl = "https://everguard-2ea86-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var notifications = mutableListOf<Notification>()
    private var selectedNotification: Notification? = null

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

        binding.btnCreateTestData.setOnClickListener {
            createTestNotificationsInFirebase()
            Toast.makeText(requireContext(), "Test notifications created!", Toast.LENGTH_SHORT).show()
        }

        // Load notifications from Firebase
        loadNotifications()

        // Dim Overlay
        binding.dimOverlay2.setOnClickListener {
            binding.alertDetailsPopup2.visibility = View.GONE
            binding.dimOverlay2.visibility = View.GONE
        }

        binding.dimOverlay2.isClickable = true
        binding.dimOverlay2.isFocusable = true

        // Call/SMS logic
        binding.btnCall2.setOnClickListener {
            // Will implement with emergency contacts
            Toast.makeText(requireContext(), "Calling emergency contact...", Toast.LENGTH_SHORT).show()
        }

        binding.btnSms2.setOnClickListener {
            // Will implement with emergency contacts
            Toast.makeText(requireContext(), "Sending SMS...", Toast.LENGTH_SHORT).show()
        }

        // Kebab Menu Logic
        binding.homeKebabMenu.setOnClickListener { v ->
            showPopupMenu(v)
        }
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return

        // Get user's device ID first
        val userRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                val deviceId = user?.deviceId

                if (!deviceId.isNullOrEmpty() && deviceId != "") {
                    loadNotificationsForDevice(deviceId)
                } else {
                    // No device paired yet, create dummy notifications for testing
                    createDummyNotifications()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadNotificationsForDevice(deviceId: String) {
        val notifsRef = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("notifs")

        // Query notifications (in a real app, you'd filter by deviceId)
        notifsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()

                for (notifSnapshot in snapshot.children) {
                    val notification = notifSnapshot.getValue(Notification::class.java)
                    if (notification != null) {
                        notifications.add(notification)
                    }
                }

                // Sort by date (most recent first)
                notifications.sortByDescending { it.date }

                if (notifications.isEmpty()) {
                    // No notifications yet, show dummy data
                    createDummyNotifications()
                } else {
                    displayNotifications()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createDummyNotifications() {
        // Create dummy notifications for testing UI
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Calendar.getInstance()

        notifications.clear()

        // Notification 1: 30 mins ago
        notifications.add(Notification(
            type = "Fall Alert",
            title = "Accident Alert",
            description = "A fall has been detected!",
            location = "https://maps.google.com",
            date = dateFormat.format(now.time),
            readBy = listOf()
        ))

        // Notification 2: 1 hour ago
        now.add(Calendar.HOUR, -1)
        notifications.add(Notification(
            type = "SOS Alert",
            title = "SOS Alert",
            description = "Juan Dela Cruz needs immediate help!",
            location = "https://maps.google.com",
            date = dateFormat.format(now.time),
            readBy = listOf()
        ))

        // Notification 3: 3 hours ago
        now.add(Calendar.HOUR, -2)
        notifications.add(Notification(
            type = "Battery Alert",
            title = "Low Battery Alert",
            description = "The device only has 20% charge left!",
            location = "",
            date = dateFormat.format(now.time),
            readBy = listOf()
        ))

        // Notification 4: 1 day ago
        now.add(Calendar.DAY_OF_MONTH, -1)
        notifications.add(Notification(
            type = "Fall Alert",
            title = "Accident Alert",
            description = "A fall has been detected!",
            location = "https://maps.google.com",
            date = dateFormat.format(now.time),
            readBy = listOf()
        ))

        // Notification 5: 5 days ago
        now.add(Calendar.DAY_OF_MONTH, -4)
        notifications.add(Notification(
            type = "SOS Alert",
            title = "SOS Alert",
            description = "Juan Dela Cruz needs immediate help!",
            location = "https://maps.google.com",
            date = dateFormat.format(now.time),
            readBy = listOf()
        ))

        // Notification 6: 5 days ago
        notifications.add(Notification(
            type = "Battery Alert",
            title = "Low Battery Alert",
            description = "The device only has 20% charge left!",
            location = "",
            date = dateFormat.format(now.time),
            readBy = listOf()
        ))

        displayNotifications()
    }

    private fun displayNotifications() {
        // Update UI with notifications
        if (notifications.isEmpty()) {
            // Show "No notifications" message
            return
        }

        // For now, we'll manually set the visible notifications
        // In a real implementation, you'd use a RecyclerView

        // You can display them in your existing card views
        // This is a simplified version - you should implement a RecyclerView adapter

        // Just showing that notifications are loaded
        Toast.makeText(
            requireContext(),
            "Loaded ${notifications.size} notifications",
            Toast.LENGTH_SHORT
        ).show()
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

    // Add this to NotificationsFragment temporarily for testing
    private fun createTestNotificationsInFirebase() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val notifsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("notifs")

        val testNotifications = listOf(
            Notification(
                type = "Fall Alert TEST 123",
                title = "Accident Alert",
                description = "A fall has been detected! REPEAT REPEAT",
                location = "https://maps.google.com",
                date = dateFormat.format(Date()),
                readBy = listOf()
            ),
            Notification(
                type = "SOS Alert",
                title = "SOS Alert",
                description = "STEPHEN CURRY needs immediate help!",
                location = "https://maps.google.com",
                date = dateFormat.format(Date()),
                readBy = listOf()
            )
        )

        testNotifications.forEachIndexed { index, notification ->
            notifsRef.child("N_00${index + 1}").setValue(notification)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}