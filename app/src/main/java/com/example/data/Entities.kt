package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val time: String, // e.g. "08:00"
    val isDaily: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val alarmsEnabled: Boolean = false
)

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val date: String, // e.g. "2024-01-01"
    val isTaken: Boolean,
    val timestamp: String? = null
)
