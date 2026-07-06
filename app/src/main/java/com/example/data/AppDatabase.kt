package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Medication::class, MedicationLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationLogDao(): MedicationLogDao
}
