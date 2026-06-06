package com.tagu.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tagu.app.R
import com.tagu.app.TaguApp
import com.tagu.app.ble.DistanceCalculator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class TagMonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var alertManager: AlertManager

    override fun onCreate() {
        super.onCreate()
        alertManager = AlertManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        val app = application as TaguApp
        val scanner = app.bleScanner
        val dao = app.database.tagDao()

        scanner.startScan()

        // Monitor loop
        scope.launch {
            scanner.discoveredTags.collectLatest { tags ->
                val monitoredTags = dao.getMonitoredTags()
                for (savedTag in monitoredTags) {
                    val scanned = tags[savedTag.lastPrivacyId]
                    if (scanned != null) {
                        val distance = DistanceCalculator.estimateDistance(
                            rssi = scanned.rssi,
                            measuredPower = savedTag.measuredPowerAt1m,
                            pathLossExponent = savedTag.pathLossExponent
                        )
                        dao.updateTagScan(
                            id = savedTag.id,
                            privacyId = scanned.privacyId,
                            address = scanned.deviceAddress,
                            timestamp = scanned.timestamp,
                            rssi = scanned.rssi
                        )
                        alertManager.checkAndAlert(savedTag, distance, tagGone = false)
                    } else {
                        // Check if tag was recently seen but now gone
                        val timeSinceLastSeen = System.currentTimeMillis() - savedTag.lastSeenTimestamp
                        if (savedTag.lastSeenTimestamp > 0 && timeSinceLastSeen > 30_000) {
                            alertManager.checkAndAlert(savedTag, 0f, tagGone = true)
                        }
                    }
                }
            }
        }

        // Periodically clean stale scan results
        scope.launch {
            while (isActive) {
                delay(10_000)
                scanner.clearStale()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        val app = application as TaguApp
        app.bleScanner.stopScan()
        alertManager.cancelAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "scanning")
            .setSmallIcon(R.drawable.ic_tag)
            .setContentTitle("Tagu")
            .setContentText("Monitoring your SmartTags")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
