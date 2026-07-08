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
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)
        val alarmsEnabled = intent.getBooleanExtra("ALARMS_ENABLED", false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = if (alarmsEnabled) "med_alarm_channel" else "med_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (alarmsEnabled) "İlaç Alarmları (Sesli)" else "İlaç Hatırlatıcı"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                if (alarmsEnabled) {
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM), audioAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action for Taken (Aldım)
        val takenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = "ACTION_TAKEN"
            putExtra("MED_ID", medId)
            putExtra("NOTIF_ID", medName.hashCode())
            putExtra("IS_TAKEN", true)
        }
        val takenPendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            medId,
            takenIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Action for Not Taken (Almadım)
        val notTakenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = "ACTION_NOT_TAKEN"
            putExtra("MED_ID", medId)
            putExtra("NOTIF_ID", medName.hashCode())
            putExtra("IS_TAKEN", false)
        }
        val notTakenPendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            medId + 500000,
            notTakenIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (alarmsEnabled) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        if (alarmsEnabled) {
            builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM))
            builder.setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
        }

        if (isPreReminder) {
            builder.setContentTitle("İlaç Zamanınız Yaklaşıyor!")
                .setContentText("İlacınızı ($medName - $medDosage) almanıza 10 dakika kaldı. Lütfen hazırlanın.")
                .addAction(R.mipmap.ic_launcher, "Şimdi Aldım", takenPendingIntent)
        } else {
            builder.setContentTitle("İlaç Hatırlatıcı: $medName")
                .setContentText("Dozaj: $medDosage alma vaktiniz geldi. İlacı aldınız mı?")
                .addAction(R.mipmap.ic_launcher, "Aldım", takenPendingIntent)
                .addAction(R.mipmap.ic_launcher, "Almadım", notTakenPendingIntent)
        }

        notificationManager.notify(medName.hashCode(), builder.build())
    }
}
