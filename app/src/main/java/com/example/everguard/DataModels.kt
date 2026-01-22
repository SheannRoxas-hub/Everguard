package com.example.everguard

// User account information
data class User(
    val username: String = "",
    val email: String = "",
    val createdAt: String = "",
    val deviceId: String = "",
    val carePerson: CarePerson = CarePerson()
)

// Person being cared for (senior/elderly)
data class CarePerson(
    val fname: String = "",
    val lname: String = "",
    val bdate: String = "",
    val gender: String = "",
    val contact: String = ""
)

// Device information
data class Device(
    val deviceId: String = "",
    val batteryStatus: String = "100%",
    val sensitivity: Int = 3,
    val isSos: Boolean = false,
    val emergencyContacts: Map<String, EmergencyContact> = mapOf()
)

// Emergency contact person
data class EmergencyContact(
    val fname: String = "",
    val lname: String = "",
    val relationship: String = "",
    val contact: String = ""
)

// Notification
data class Notification(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val deviceId: String = "",
    val readBy: List<String> = listOf()
)