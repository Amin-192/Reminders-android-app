package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getStringExtra("task") ?: "Task"
        val option = intent.getStringExtra("option") ?: "Once"

        try {
            // Create and show notification
            val notification = NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Reminder")
                .setContentText(task)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            NotificationManagerCompat.from(context).notify(task.hashCode(), notification)

            // Remove the task from the list if it's a "Once" reminder
            if (context is MainActivity && option == "Once") {
                context.removeTaskFromList(task)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}