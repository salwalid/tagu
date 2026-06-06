package com.tagu.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tagu.app.data.db.TagEntity
import com.tagu.app.service.TagMonitorService
import com.tagu.app.ui.screens.TagDetailScreen
import com.tagu.app.ui.screens.TagListScreen
import com.tagu.app.ui.theme.TaguTheme
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val app by lazy { application as TaguApp }
    private val dao by lazy { app.database.tagDao() }
    private val scanner by lazy { app.bleScanner }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            scanner.startScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TaguTheme {
                val savedTags by dao.getAllTags().collectAsStateWithLifecycle(emptyList())
                val discoveredTags by scanner.discoveredTags.collectAsStateWithLifecycle()
                val isScanning by scanner.isScanning.collectAsStateWithLifecycle()
                var selectedTag by remember { mutableStateOf<TagEntity?>(null) }
                var isServiceRunning by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                if (selectedTag != null) {
                    val tag = selectedTag!!
                    val scanned = discoveredTags[tag.lastPrivacyId]
                    TagDetailScreen(
                        tag = tag,
                        scannedTag = scanned,
                        onBack = { selectedTag = null },
                        onUpdateTag = { updated ->
                            scope.launch {
                                dao.updateTag(updated)
                                selectedTag = null
                            }
                        }
                    )
                } else {
                    TagListScreen(
                        savedTags = savedTags,
                        discoveredTags = discoveredTags,
                        isScanning = isScanning,
                        isServiceRunning = isServiceRunning,
                        onStartScan = { requestPermissionsAndScan() },
                        onStopScan = { scanner.stopScan() },
                        onStartService = {
                            requestPermissionsAndScan()
                            startForegroundService(Intent(this@MainActivity, TagMonitorService::class.java))
                            isServiceRunning = true
                        },
                        onStopService = {
                            stopService(Intent(this@MainActivity, TagMonitorService::class.java))
                            isServiceRunning = false
                        },
                        onSaveTag = { scanned, nickname ->
                            scope.launch {
                                dao.insertTag(TagEntity(
                                    id = UUID.randomUUID().toString(),
                                    nickname = nickname,
                                    lastPrivacyId = scanned.privacyId,
                                    lastDeviceAddress = scanned.deviceAddress,
                                    lastSeenTimestamp = scanned.timestamp,
                                    lastRssi = scanned.rssi
                                ))
                            }
                        },
                        onTagClick = { selectedTag = it },
                        onDeleteTag = { tag ->
                            scope.launch { dao.deleteTag(tag) }
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissionsAndScan() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            scanner.startScan()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
