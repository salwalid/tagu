package com.tagu.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagu.app.ble.DistanceCalculator
import com.tagu.app.data.db.TagEntity
import com.tagu.app.data.model.ScannedTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    tag: TagEntity,
    scannedTag: ScannedTag?,
    onBack: () -> Unit,
    onUpdateTag: (TagEntity) -> Unit,
) {
    val distance = scannedTag?.let {
        DistanceCalculator.estimateDistance(it.rssi, tag.measuredPowerAt1m, tag.pathLossExponent)
    }
    val isNearby = scannedTag != null

    var maxDistance by remember { mutableFloatStateOf(tag.maxDistanceMeters) }
    var noticePercent by remember { mutableFloatStateOf(tag.noticePercent.toFloat()) }
    var warnPercent by remember { mutableFloatStateOf(tag.warnPercent.toFloat()) }
    var alertPercent by remember { mutableFloatStateOf(tag.alertPercent.toFloat()) }
    var alarmPercent by remember { mutableFloatStateOf(tag.alarmPercent.toFloat()) }
    var sensitivity by remember { mutableFloatStateOf(tag.sensitivity.toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag.nickname) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onUpdateTag(tag.copy(
                            maxDistanceMeters = maxDistance,
                            noticePercent = noticePercent.toInt(),
                            warnPercent = warnPercent.toInt(),
                            alertPercent = alertPercent.toInt(),
                            alarmPercent = alarmPercent.toInt(),
                            sensitivity = sensitivity.toInt()
                        ))
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Live Status ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNearby) Color(0xFF1B3A26) else Color(0xFF3A1B1B)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isNearby) DistanceCalculator.formatDistance(distance ?: -1f) else "Not Detected",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNearby) Color(0xFF4CAF50) else Color(0xFFEF5350)
                    )
                    if (isNearby && scannedTag != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Signal: ${scannedTag.rssi} dBm · ${DistanceCalculator.signalQuality(scannedTag.rssi)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "Battery: ${scannedTag.batteryLevel.name} · Motion: ${if (scannedTag.motionDetected) "Yes" else "No"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── Alert Distance ──
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alert Distance", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "%.0f meters".format(maxDistance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = maxDistance,
                        onValueChange = { maxDistance = it },
                        valueRange = 1f..100f,
                        steps = 98,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Alert when tag exceeds this distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // ── Alert Levels ──
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alert Escalation", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))

                    AlertLevelSlider("1. Notice (silent)", noticePercent, { noticePercent = it }, Color(0xFF90CAF9))
                    AlertLevelSlider("2. Warn (vibrate)", warnPercent, { warnPercent = it }, Color(0xFFFFF176))
                    AlertLevelSlider("3. Alert (vibrate+sound)", alertPercent, { alertPercent = it }, Color(0xFFFFA726))
                    AlertLevelSlider("4. Alarm (ring+vibrate)", alarmPercent, { alarmPercent = it }, Color(0xFFEF5350))

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "5. Panic — triggers when tag disappears from BLE entirely",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF5350).copy(alpha = 0.7f)
                    )
                }
            }

            // ── Sensitivity ──
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sensitivity", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val sensLabel = when (sensitivity.toInt()) {
                        1 -> "Hair trigger (1 reading)"
                        3 -> "Normal (3 readings)"
                        5 -> "Relaxed (5 readings)"
                        8 -> "Chill (8 readings)"
                        else -> "${sensitivity.toInt()} readings"
                    }
                    Text(sensLabel, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 1f..8f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Higher = fewer false alarms from signal fluctuations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AlertLevelSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
            Text("${value.toInt()}%", style = MaterialTheme.typography.bodySmall, color = color)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 10f..100f,
            steps = 17,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            ),
            modifier = Modifier.height(32.dp)
        )
    }
}
