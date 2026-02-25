package com.vrsa.app

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.File

class MainActivity : Activity() {

    private lateinit var editor: EditText
    private lateinit var configFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configFile = File(getExternalFilesDir(null), "reminders.txt")
        editor = findViewById(R.id.editor)

        createNotificationChannel()
        createConfigFile()
        editor.setText(configFile.readText())

        findViewById<Button>(R.id.btn_save).setOnClickListener { onSaveClicked() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onSaveClicked() {
        val text = editor.text.toString()
        configFile.writeText(text)
        scheduleAlarms()
        val count = text.lines().count { it.isNotBlank() && !it.startsWith("#") && parseLine(it) != null }
        Toast.makeText(this, "Saved · $count reminder${if (count == 1) "" else "s"} scheduled", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleAlarms() {
        configFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                val reminder = parseLine(line) ?: return@forEach
                scheduleAlarm(this, reminder, line.hashCode())
            }
    }

    private fun createConfigFile() {
        if (!configFile.exists()) {
            configFile.writeText(
                "# Reminders config file\n" +
                "# One reminder per line: HH:MM  <days>  <label>\n" +
                "# Days: daily  OR  Mon,Tue,Wed,Thu,Fri,Sat,Sun (comma-separated)\n" +
                "#\n" +
                "# Examples:\n" +
                "# 08:00  daily                Morning alarm\n" +
                "# 09:00  Mon,Tue,Wed,Thu,Fri  Weekday reminder\n" +
                "# 22:30  Fri,Sat              Weekend late reminder\n"
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
