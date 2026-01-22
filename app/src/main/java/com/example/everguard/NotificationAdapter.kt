package com.example.everguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

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

        // Calculate time difference for color coding
        val notificationDate = parseNotificationDate(notification.date)
        val now = Date()
        val diffInHours = (now.time - notificationDate.time) / (1000 * 60 * 60)

        // Determine if notification is "old" (>24 hours)
        val isOld = diffInHours >= 24

        // Set icon and color based on type and age
        when {
            notification.type.contains("Fall", ignoreCase = true) ||
                    notification.type.contains("Accident", ignoreCase = true) -> {
                holder.icon.setImageResource(R.drawable.notifications_icon)
                if (isOld) {
                    // Gray for old notifications
                    holder.icon.setColorFilter(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.title.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.description.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                } else {
                    // Orange for recent fall/accident alerts
                    holder.icon.setColorFilter(0xFFFF914D.toInt()) // #ff914d
                    holder.title.setTextColor(0xFFFF914D.toInt())
                    holder.description.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                }
            }
            notification.type.contains("SOS", ignoreCase = true) -> {
                holder.icon.setImageResource(R.drawable.notifications_icon)
                if (isOld) {
                    // Gray for old notifications
                    holder.icon.setColorFilter(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.title.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.description.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                } else {
                    // Red for recent SOS
                    holder.icon.setColorFilter(0xFFDC3030.toInt()) // #dc3030
                    holder.title.setTextColor(0xFFDC3030.toInt())
                    holder.description.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                }
            }
            notification.type.contains("Battery", ignoreCase = true) -> {
                holder.icon.setImageResource(R.drawable.notifications_icon)
                if (isOld) {
                    // Gray for old notifications
                    holder.icon.setColorFilter(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.title.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                    holder.description.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                } else {
                    // Dark red for recent battery alerts
                    holder.icon.setColorFilter(0xFF910000.toInt()) // #910000
                    holder.title.setTextColor(0xFF910000.toInt())
                    holder.description.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                }
            }
            else -> {
                // Default styling
                holder.icon.setImageResource(R.drawable.notifications_icon)
                holder.icon.setColorFilter(holder.itemView.context.getColor(android.R.color.darker_gray))
                holder.title.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                holder.description.setTextColor(holder.itemView.context.getColor(android.R.color.black))
            }
        }

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