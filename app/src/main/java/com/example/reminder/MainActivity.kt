package com.example.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_NOTIFICATION_PERMISSION = 1
    }

    private lateinit var etTask: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnAddReminder: Button
    private lateinit var lvReminders: ListView
    private val reminders = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check permissions
        checkNotificationPermission()
        checkExactAlarmPermission()

        // Initialize views
        etTask = findViewById(R.id.etTask)
        timePicker = findViewById(R.id.timePicker)
        radioGroup = findViewById(R.id.radioGroup)
        btnAddReminder = findViewById(R.id.btnAddReminder)
        lvReminders = findViewById(R.id.lvReminders)

        // Set up ListView
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, reminders)
        lvReminders.adapter = adapter

        // Notification channel setup
        createNotificationChannel()

        // Add reminder
        btnAddReminder.setOnClickListener {
            val task = etTask.text.toString()
            if (task.isEmpty()) {
                Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hour = timePicker.hour
            val minute = timePicker.minute
            val selectedOption = when (radioGroup.checkedRadioButtonId) {
                R.id.rbOnce -> "Once"
                R.id.rbDaily -> "Daily"
                R.id.rbWeekly -> "Weekly"
                else -> "Once"
            }

            addReminder(task, hour, minute, selectedOption)
        }
    }

    private fun addReminder(task: String, hour: Int, minute: Int, option: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // Ensure the time is in the future
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Check for exact alarm permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Exact alarm permission is required", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val intent = Intent(this, ReminderReceiver::class.java).apply {
                putExtra("task", task)
                putExtra("option", option) // Pass the type of task (Once, Daily, Weekly)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                task.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            when (option) {
                "Once" -> alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                "Daily" -> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
                "Weekly" -> alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY * 7, pendingIntent)
            }

            reminders.add("$task at $hour:$minute ($option)")
            adapter.notifyDataSetChanged()
            etTask.text.clear()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun removeTaskFromList(task: String) {
        reminders.removeIf { it.startsWith(task) } // Remove only the matching task
        adapter.notifyDataSetChanged()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for reminders"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}