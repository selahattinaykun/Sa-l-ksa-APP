package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

class MedicationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getIntExtra("MED_ID", -1)
        val medName = intent.getStringExtra("MED_NAME") ?: "İlacınız"
        val medDosage = intent.getStringExtra("MED_DOSAGE") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("med_channel", "İlaç Hatırlatıcı", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val takenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = "ACTION_TAKEN"
            putExtra("MED_ID", medId)
            putExtra("NOTIF_ID", medName.hashCode())
        }
        val takenPendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            medId,
            takenIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "med_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("İlaç Hatırlatıcı: $medName")
            .setContentText("Dozaj: $medDosage alma vaktiniz geldi. İlacı aldınız mı?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.mipmap.ic_launcher, "Aldım", takenPendingIntent)
            .build()

        notificationManager.notify(medName.hashCode(), notification)
    }
}
