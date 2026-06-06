package com.tagu.app.ble

import com.tagu.app.data.model.*

object TagParser {
    // Samsung SmartTag service UUID
    const val SMARTTAG_SERVICE_UUID = "0000fd5a-0000-1000-8000-00805f9b34fb"
    // Service data key (16-bit UUID)
    val SMARTTAG_SERVICE_DATA_UUID: android.os.ParcelUuid =
        android.os.ParcelUuid.fromString(SMARTTAG_SERVICE_UUID)

    fun parse(deviceAddress: String, serviceData: ByteArray, rssi: Int): ScannedTag? {
        if (serviceData.size < 16) return null

        val byte0 = serviceData[0].toInt() and 0xFF
        val tagStateValue = byte0 and 0x07
        val tagState = TagState.fromValue(tagStateValue)

        // Aging counter: bytes 1-3 (big-endian, 24-bit)
        val aging = ((serviceData[1].toInt() and 0xFF) shl 16) or
                    ((serviceData[2].toInt() and 0xFF) shl 8) or
                    (serviceData[3].toInt() and 0xFF)

        // Privacy ID: bytes 4-11 (8 bytes as hex)
        val privacyId = serviceData.slice(4..11)
            .joinToString("") { "%02x".format(it) }

        // Byte 12: battery (bits 0-1), UWB (bit 2), encryption (bit 3)
        val byte12 = if (serviceData.size > 12) serviceData[12].toInt() and 0xFF else 0
        val batteryValue = byte12 and 0x03
        val hasUwb = (byte12 and 0x04) != 0
        val hasEncryption = (byte12 and 0x08) != 0

        // Byte 13: motion (bit 7)
        val byte13 = if (serviceData.size > 13) serviceData[13].toInt() and 0xFF else 0
        val motionDetected = (byte13 and 0x80) != 0

        return ScannedTag(
            deviceAddress = deviceAddress,
            privacyId = privacyId,
            rssi = rssi,
            tagState = tagState,
            batteryLevel = BatteryLevel.fromValue(batteryValue),
            hasUwb = hasUwb,
            hasEncryption = hasEncryption,
            motionDetected = motionDetected,
            agingCounter = aging
        )
    }
}
