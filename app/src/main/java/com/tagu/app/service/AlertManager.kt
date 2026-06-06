package com.tagu.app.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.tagu.app.R
import com.tagu.app.data.db.TagEntity
import com.tagu.app.data.model.AlertLevel

class AlertManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Track consecutive readings beyond threshold per tag
    private val consecutiveReadings = mutableMapOf<String, Int>()
    // Track current alert level per tag
    private val currentAlertLevel = mutableMapOf<String, AlertLevel>()

    fun checkAndAlert(tag: TagEntity, distanceMeters: Float, tagGone: Boolean) {
        val tagId = tag.id

        if (tagGone && tag.panicEnabled) {
            triggerAlert(tag, AlertLevel.PANIC, -1f, true)
            return
        }

        // Determine which level this distance triggers
        val triggeredLevel = getTriggeredLevel(tag, distanceMeters)

        if (triggeredLevel == null) {
            // Tag is within safe range — reset
            consecutiveReadings.remove(tagId)
            val prev = currentAlertLevel.remove(tagId)
            if (prev != null) cancelAlert(tag)
            return
        }

        // Increment consecutive readings
        val count = (consecutiveReadings[tagId] ?: 0) + 1
        consecutiveReadings[tagId] = count

        // Check sensitivity threshold
        if (count >= tag.sensitivity) {
            val prevLevel = currentAlertLevel[tagId]
            if (prevLevel == null || triggeredLevel.value > prevLevel.value) {
                triggerAlert(tag, triggeredLevel, distanceMeters, false)
                currentAlertLevel[tagId] = triggeredLevel
            }
        }
    }

    private fun getTriggeredLevel(tag: TagEntity, distance: Float): AlertLevel? {
        val max = tag.maxDistanceMeters
        // Check from highest to lowest
        if (tag.alarmEnabled && distance >= max * tag.alarmPercent / 100f) return AlertLevel.ALARM
        if (tag.alertEnabled && distance >= max * tag.alertPercent / 100f) return AlertLevel.ALERT
        if (tag.warnEnabled && distance >= max * tag.warnPercent / 100f) return AlertLevel.WARN
        if (tag.noticeEnabled && distance >= max * tag.noticePercent / 100f) return AlertLevel.NOTICE
        return null
    }

    private fun triggerAlert(tag: TagEntity, level: AlertLevel, distance: Float, isGone: Boolean) {
        val notifId = tag.id.hashCode()
        val channelId = when (level) {
            AlertLevel.NOTICE -> "alert_notice"
            AlertLevel.WARN -> "alert_warn"
            AlertLevel.ALERT -> "alert_alert"
            AlertLevel.ALARM -> "alert_alarm"
            AlertLevel.PANIC -> "alert_panic"
        }

        val title = when (level) {
            AlertLevel.NOTICE -> "${tag.nickname} is moving away"
            AlertLevel.WARN -> "${tag.nickname} getting distant"
            AlertLevel.ALERT -> "${tag.nickname} — ALERT"
            AlertLevel.ALARM -> "${tag.nickname} — ALARM!"
            AlertLevel.PANIC -> "${tag.nickname} — PANIC! TAG LOST!"
        }

        val text = if (isGone) {
            "Tag is no longer detected! Someone may have taken it."
        } else {
            "Distance: %.1fm (threshold: %.0fm)".format(distance, tag.maxDistanceMeters)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_tag)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(level != AlertLevel.PANIC)
            .setOngoing(level == AlertLevel.PANIC || level == AlertLevel.ALARM)
            .setPriority(when (level) {
                AlertLevel.NOTICE -> NotificationCompat.PRIORITY_LOW
                AlertLevel.WARN -> NotificationCompat.PRIORITY_DEFAULT
                else -> NotificationCompat.PRIORITY_HIGH
            })

        if (level == AlertLevel.ALARM || level == AlertLevel.PANIC) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            builder.setSound(alarmUri, AudioManager.STREAM_ALARM)
        }

        notificationManager.notify(notifId, builder.build())

        // Vibrate based on level
        when (level) {
            AlertLevel.WARN -> vibrate(longArrayOf(0, 200))
            AlertLevel.ALERT -> vibrate(longArrayOf(0, 300, 200, 300))
            AlertLevel.ALARM -> vibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            AlertLevel.PANIC -> vibrate(longArrayOf(0, 1000, 200, 1000, 200, 1000, 200, 1000))
            else -> {} // Notice = silent
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun cancelAlert(tag: TagEntity) {
        notificationManager.cancel(tag.id.hashCode())
        consecutiveReadings.remove(tag.id)
        currentAlertLevel.remove(tag.id)
    }

    fun cancelAll() {
        notificationManager.cancelAll()
        consecutiveReadings.clear()
        currentAlertLevel.clear()
    }
}
