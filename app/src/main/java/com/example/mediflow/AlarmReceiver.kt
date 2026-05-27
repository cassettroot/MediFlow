package com.example.mediflow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("MED_NAME") ?: "Medicamento"
        val isReminder = intent.getBooleanOfDefault("IS_REMINDER", false)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mediflow_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de MediFlow",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para la toma de medicamentos"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val prefs = context.getSharedPreferences("mediflow_prefs", Context.MODE_PRIVATE)
        val soundEnabled = prefs.getBoolean("notif_sound", true)
        val vibrateEnabled = prefs.getBoolean("notif_vibrate", true)
        val reminderMins = prefs.getInt("reminder_time", 15)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isReminder) "Recordatorio Próximo" else "Hora de tu Medicamento")
            .setContentText(if (isReminder) "En $reminderMins minutos: $medName" else "Es momento de tomar: $medName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        if (soundEnabled) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(alarmSound)
        }

        if (vibrateEnabled) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        notificationManager.notify(medName.hashCode(), builder.build())
    }

    private fun Intent.getBooleanOfDefault(key: String, default: Boolean): Boolean {
        return getBooleanExtra(key, default)
    }
}
