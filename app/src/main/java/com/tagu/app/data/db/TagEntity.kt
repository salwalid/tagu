package com.tagu.app.data.db

import androidx.room.*

@Entity(tableName = "saved_tags")
data class TagEntity(
    @PrimaryKey val id: String,  // user-assigned or auto UUID
    val nickname: String,
    val lastPrivacyId: String,
    val lastDeviceAddress: String,
    val maxDistanceMeters: Float = 10f,
    // 5 alert level thresholds (percentage of maxDistance)
    val noticePercent: Int = 60,
    val warnPercent: Int = 75,
    val alertPercent: Int = 90,
    val alarmPercent: Int = 100,
    // Individual levels enabled
    val noticeEnabled: Boolean = true,
    val warnEnabled: Boolean = true,
    val alertEnabled: Boolean = true,
    val alarmEnabled: Boolean = true,
    val panicEnabled: Boolean = true,
    // Calibration
    val measuredPowerAt1m: Int = -59,  // RSSI at 1 meter
    val pathLossExponent: Float = 2.5f,  // indoor default
    // Sensitivity
    val sensitivity: Int = 3,  // consecutive readings (AlertSensitivity.NORMAL)
    // State
    val isMonitoring: Boolean = true,
    val lastSeenTimestamp: Long = 0,
    val lastRssi: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
