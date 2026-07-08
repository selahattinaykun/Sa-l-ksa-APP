package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.util.AlarmHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java, "meditrack-db"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    
                    val medications = db.medicationDao().getAllMedicationsList()
                    for (med in medications) {
                        if (med.notificationsEnabled) {
                            AlarmHelper.scheduleMedicationAlarm(
                                context,
                                med.id,
                                med.name,
                                med.dosage,
                                med.time,
                                med.notificationsEnabled,
                                med.alarmsEnabled
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
