package com.tagu.app.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.tagu.app.data.model.ScannedTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BleScanner(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    private val _discoveredTags = MutableStateFlow<Map<String, ScannedTag>>(emptyMap())
    val discoveredTags: StateFlow<Map<String, ScannedTag>> = _discoveredTags

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    fun startScan() {
        if (_isScanning.value) return
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        scanner = adapter.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceData(
                ParcelUuid.fromString(TagParser.SMARTTAG_SERVICE_UUID),
                null
            )
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleResult(result)
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { handleResult(it) }
            }
        }

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback!!)
            _isScanning.value = true
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun stopScan() {
        try {
            scanCallback?.let { scanner?.stopScan(it) }
        } catch (_: SecurityException) {}
        scanCallback = null
        _isScanning.value = false
    }

    private fun handleResult(result: ScanResult) {
        val record = result.scanRecord ?: return
        // Try to get service data for FD5A
        val serviceData = record.getServiceData(TagParser.SMARTTAG_SERVICE_DATA_UUID) ?: return

        val tag = TagParser.parse(
            deviceAddress = result.device.address,
            serviceData = serviceData,
            rssi = result.rssi
        ) ?: return

        val current = _discoveredTags.value.toMutableMap()
        current[tag.privacyId] = tag
        _discoveredTags.value = current
    }

    fun clearStale(maxAgeMs: Long = 30_000) {
        val now = System.currentTimeMillis()
        val current = _discoveredTags.value.toMutableMap()
        current.entries.removeAll { now - it.value.timestamp > maxAgeMs }
        _discoveredTags.value = current
    }
}
