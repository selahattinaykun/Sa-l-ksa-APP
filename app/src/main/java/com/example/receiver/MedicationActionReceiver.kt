package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.MedicationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val medId = intent.getIntExtra("MED_ID", -1)
        val notifId = intent.getIntExtra("NOTIF_ID", -1)
        
        if (medId != -1 && notifId != -1 && action == "ACTION_TAKEN") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "meditrack-db"
                ).build()
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                
                val existing = db.medicationLogDao().getLog(medId, currentDate)
                if (existing != null) {
                    db.medicationLogDao().updateLogStatus(medId, currentDate, true)
                } else {
                    db.medicationLogDao().insertLog(MedicationLog(medicationId = medId, date = currentDate, isTaken = true))
                }
                
                NotificationManagerCompat.from(context).cancel(notifId)
                pendingResult.finish()
            }
        }
    }
}
