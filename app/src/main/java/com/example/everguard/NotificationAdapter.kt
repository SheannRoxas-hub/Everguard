package com.example.everguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.notificationIcon)
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val description: TextView = view.findViewById(R.id.notificationDescription)
        val time: TextView = view.findViewById(R.id.notificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.title.text = notification.title
        holder.description.text = notification.description
        holder.time.text = getTimeAgo(notification.date)

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
                else -> 0xFFFF914D.toInt()
            }
        }

        // Apply color to title and icon
        holder.title.setTextColor(color)
        holder.icon.setColorFilter(color)

        // Set icon based on type
        val iconRes = when {
            notification.type.contains("Fall", ignoreCase = true) ||
                    notification.type.contains("Accident", ignoreCase = true) -> R.drawable.notifications_icon
            notification.type.contains("SOS", ignoreCase = true) -> R.drawable.notifications_icon
            notification.type.contains("Battery", ignoreCase = true) -> R.drawable.ic_battery
            else -> R.drawable.notifications_icon
        }
        holder.icon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onItemClick(notification)
        }
    }

    override fun getItemCount() = notifications.size

    private fun parseNotificationDate(dateStr: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(dateStr) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }

    private fun isNotificationOld(dateStr: String): Boolean {
        val notificationDate = parseNotificationDate(dateStr)
        val now = Date()
        val diffInMillis = now.time - notificationDate.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return diffInDays >= 1
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
}