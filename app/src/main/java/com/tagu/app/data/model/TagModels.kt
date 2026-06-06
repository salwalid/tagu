package com.tagu.app.data.model

// Parsed from BLE advertisement
data class ScannedTag(
    val deviceAddress: String,  // BLE MAC (rotates)
    val privacyId: String,      // 8-byte hex from bytes 4-11
    val rssi: Int,
    val tagState: TagState,
    val batteryLevel: BatteryLevel,
    val hasUwb: Boolean,
    val hasEncryption: Boolean,
    val motionDetected: Boolean,
    val agingCounter: Int,      // seconds since disconnect
    val timestamp: Long = System.currentTimeMillis()
)

enum class TagState(val value: Int) {
    UNKNOWN(0),
    PREMATURE_OFFLINE(1),    // <15 min disconnected
    OFFLINE(2),              // 15min - 24h
    OVERMATURE_OFFLINE(3),   // >24h
    CONNECTED_OWNER(4),
    CONNECTED_OTHER(5),
    CONNECTED_TWO(6);
    companion object {
        fun fromValue(v: Int) = entries.find { it.value == v } ?: UNKNOWN
    }
}

enum class BatteryLevel(val value: Int) {
    VERY_LOW(0), LOW(1), MEDIUM(2), FULL(3);
    companion object {
        fun fromValue(v: Int) = entries.find { it.value == v } ?: FULL
    }
}

enum class AlertLevel(val value: Int) {
    NOTICE(0), WARN(1), ALERT(2), ALARM(3), PANIC(4);
}

enum class AlertSensitivity(val readings: Int) {
    HAIR_TRIGGER(1), NORMAL(3), RELAXED(5), CHILL(8);
}
