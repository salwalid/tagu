package com.tagu.app.ble

import kotlin.math.pow

object DistanceCalculator {
    /**
     * Estimate distance from RSSI using log-distance path loss model.
     * distance = 10 ^ ((measuredPower - rssi) / (10 * pathLossExponent))
     *
     * @param rssi Current signal strength
     * @param measuredPower RSSI at 1 meter (default -59 dBm for SmartTags)
     * @param pathLossExponent Environment factor (2.0=open, 2.5=indoor, 3.5=obstructed)
     */
    fun estimateDistance(
        rssi: Int,
        measuredPower: Int = -59,
        pathLossExponent: Float = 2.5f
    ): Float {
        if (rssi == 0) return -1f
        val ratio = (measuredPower - rssi).toFloat() / (10f * pathLossExponent)
        return 10f.pow(ratio)
    }

    /**
     * Convert a target distance to the expected RSSI threshold.
     */
    fun distanceToRssi(
        distanceMeters: Float,
        measuredPower: Int = -59,
        pathLossExponent: Float = 2.5f
    ): Int {
        if (distanceMeters <= 0) return measuredPower
        val rssi = measuredPower - (10 * pathLossExponent * kotlin.math.log10(distanceMeters.toDouble())).toInt()
        return rssi
    }

    /**
     * Get a human-readable distance string.
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters < 0 -> "Unknown"
            meters < 1 -> "< 1m"
            meters < 10 -> "%.1fm".format(meters)
            else -> "%.0fm".format(meters)
        }
    }

    /**
     * Get signal strength description.
     */
    fun signalQuality(rssi: Int): String {
        return when {
            rssi > -50 -> "Excellent"
            rssi > -65 -> "Good"
            rssi > -80 -> "Fair"
            rssi > -90 -> "Weak"
            else -> "Very Weak"
        }
    }
}
