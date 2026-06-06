package com.tagu.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.tagu.app.data.db.AppDatabase
import com.tagu.app.ble.BleScanner

class TaguApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var bleScanner: BleScanner
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        bleScanner = BleScanner(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Scanning service channel
            manager.createNotificationChannel(
                NotificationChannel("scanning", "Tag Scanning", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Background BLE scanning for SmartTags"
                }
            )
            // Notice
            manager.createNotificationChannel(
                NotificationChannel("alert_notice", "Notice Alerts", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Silent notifications when tags start moving away"
                }
            )
            // Warn
            manager.createNotificationChannel(
                NotificationChannel("alert_warn", "Warning Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Vibration when tags are getting distant"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200)
                }
            )
            // Alert
            manager.createNotificationChannel(
                NotificationChannel("alert_alert", "Alert", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Sound and vibration when tags approach threshold"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                }
            )
            // Alarm
            manager.createNotificationChannel(
                NotificationChannel("alert_alarm", "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Loud alarm when tags exceed threshold"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                }
            )
            // Panic
            manager.createNotificationChannel(
                NotificationChannel("alert_panic", "PANIC", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Maximum alert — tag lost from BLE range"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 200, 1000, 200, 1000)
                    setBypassDnd(true)
                }
            )
        }
    }
}
