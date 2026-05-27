package com.example.mediflow

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    fun scheduleAlarms(context: Context, med: Medicamento) {
        cancelAlarms(context, med) // Cancel previous to avoid duplicates
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val lastTaken = med.lastTakenTimestamp ?: med.timestampInicio
        var nextTakeTime = lastTaken + (med.frecuenciaHoras.toLong() * 60 * 60 * 1000)

        // Handle Fixed Times (Breakfast, Lunch, Dinner, Exact Hour)
        med.horaFija?.let { fixed ->
            val cal = Calendar.getInstance()
            val targetHour: Int
            val targetMin: Int

            when {
                fixed.contains("Desayuno") -> { targetHour = 7; targetMin = 0 }
                fixed.contains("Comida") -> { targetHour = 14; targetMin = 0 }
                fixed.contains("Cena") -> { targetHour = 22; targetMin = 0 }
                fixed.startsWith("Hora:") -> {
                    val parts = fixed.replace("Hora: ", "").split(":")
                    targetHour = parts[0].toInt()
                    targetMin = parts[1].toInt()
                }
                else -> { targetHour = 0; targetMin = 0 }
            }

            cal.set(Calendar.HOUR_OF_DAY, targetHour)
            cal.set(Calendar.MINUTE, targetMin)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            nextTakeTime = cal.timeInMillis
        }
        
        if (nextTakeTime <= System.currentTimeMillis()) return

        // 1. Schedule main alarm
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MED_NAME", med.nombre)
            putExtra("IS_REMINDER", false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            med.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTakeTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTakeTime, pendingIntent)
        }

        // 2. Schedule 15 min reminder
        val prefs = context.getSharedPreferences("mediflow_prefs", Context.MODE_PRIVATE)
        val reminderOffset = prefs.getInt("reminder_time", 15) * 60 * 1000L
        val reminderTime = nextTakeTime - reminderOffset

        if (reminderTime > System.currentTimeMillis()) {
            val reminderIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("MED_NAME", med.nombre)
                putExtra("IS_REMINDER", true)
            }
            val reminderPendingIntent = PendingIntent.getBroadcast(
                context,
                med.id.hashCode() + 1,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, reminderPendingIntent)
        }
    }

    fun cancelAlarms(context: Context, med: Medicamento) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            med.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }

        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            med.id.hashCode() + 1,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        reminderPendingIntent?.let { alarmManager.cancel(it) }
    }
}
