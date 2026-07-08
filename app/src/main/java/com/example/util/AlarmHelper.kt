package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.MedicationReceiver
import java.util.Calendar

object AlarmHelper {
    fun scheduleMedicationAlarm(
        context: Context,
        medId: Int,
        name: String,
        dosage: String,
        time: String,
        notificationsEnabled: Boolean,
        alarmsEnabled: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Intent for main alarm (actual time)
        val mainIntent = Intent(context, MedicationReceiver::class.java).apply {
            putExtra("MED_ID", medId)
            putExtra("MED_NAME", name)
            putExtra("MED_DOSAGE", dosage)
            putExtra("IS_PRE_REMINDER", false)
            putExtra("ALARMS_ENABLED", alarmsEnabled)
        }
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            medId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for pre-reminder (10 mins before)
        val preIntent = Intent(context, MedicationReceiver::class.java).apply {
            putExtra("MED_ID", medId)
            putExtra("MED_NAME", name)
            putExtra("MED_DOSAGE", dosage)
            putExtra("IS_PRE_REMINDER", true)
            putExtra("ALARMS_ENABLED", alarmsEnabled)
        }
        val prePendingIntent = PendingIntent.getBroadcast(
            context,
            medId + 100000,
            preIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // If notifications are disabled, cancel both alarms and exit
        if (!notificationsEnabled) {
            alarmManager.cancel(mainPendingIntent)
            alarmManager.cancel(prePendingIntent)
            return
        }

        val parts = time.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: 8
        val minute = parts[1].toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Schedule main exact alarm
        scheduleExact(alarmManager, calendar.timeInMillis, mainPendingIntent)

        // Schedule pre-reminder exact alarm (10 minutes before actual time)
        val preCalendar = calendar.clone() as Calendar
        preCalendar.add(Calendar.MINUTE, -10)
        if (preCalendar.timeInMillis <= System.currentTimeMillis()) {
            preCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        scheduleExact(alarmManager, preCalendar.timeInMillis, prePendingIntent)
    }

    private fun scheduleExact(alarmManager: AlarmManager, timeInMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelMedicationAlarms(context: Context, medId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            medId,
            Intent(context, MedicationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (mainPendingIntent != null) {
            alarmManager.cancel(mainPendingIntent)
        }
        val prePendingIntent = PendingIntent.getBroadcast(
            context,
            medId + 100000,
            Intent(context, MedicationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (prePendingIntent != null) {
            alarmManager.cancel(prePendingIntent)
        }
    }
}
