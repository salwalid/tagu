package com.tagu.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagu.app.ble.DistanceCalculator
import com.tagu.app.data.db.TagEntity
import com.tagu.app.data.model.ScannedTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagListScreen(
    savedTags: List<TagEntity>,
    discoveredTags: Map<String, ScannedTag>,
    isScanning: Boolean,
    isServiceRunning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onSaveTag: (ScannedTag, String) -> Unit,
    onTagClick: (TagEntity) -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
) {
    var showSaveDialog by remember { mutableStateOf<ScannedTag?>(null) }
    var nickname by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tagu", fontWeight = FontWeight.Bold)
                        Text(
                            if (isScanning) "Scanning..." else "Tap to scan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    // Background monitoring toggle
                    IconButton(onClick = {
                        if (isServiceRunning) onStopService() else onStartService()
                    }) {
                        Icon(
                            if (isServiceRunning) Icons.Filled.Shield else Icons.Filled.Shield,
                            contentDescription = "Background monitoring",
                            tint = if (isServiceRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (isScanning) onStopScan() else onStartScan() },
                icon = {
                    Icon(
                        if (isScanning) Icons.Filled.Stop else Icons.Filled.BluetoothSearching,
                        contentDescription = null
                    )
                },
                text = { Text(if (isScanning) "Stop Scan" else "Scan for Tags") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Saved Tags ──
            if (savedTags.isNotEmpty()) {
                item {
                    Text(
                        "MY TAGS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(savedTags, key = { it.id }) { tag ->
                    val scanned = discoveredTags[tag.lastPrivacyId]
                    SavedTagCard(
                        tag = tag,
                        scannedTag = scanned,
                        onClick = { onTagClick(tag) },
                        onDelete = { onDeleteTag(tag) }
                    )
                }
            }

            // ── Discovered (unsaved) Tags ──
            val unsavedTags = discoveredTags.filter { (privacyId, _) ->
                savedTags.none { it.lastPrivacyId == privacyId }
            }
            if (unsavedTags.isNotEmpty()) {
                item {
                    Text(
                        "NEARBY TAGS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(unsavedTags.entries.toList(), key = { it.key }) { (_, tag) ->
                    DiscoveredTagCard(
                        tag = tag,
                        onSave = { showSaveDialog = tag }
                    )
                }
            }

            // Empty state
            if (savedTags.isEmpty() && discoveredTags.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No tags found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                "Tap 'Scan for Tags' to find nearby SmartTags",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Save dialog
    if (showSaveDialog != null) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = null; nickname = "" },
            title = { Text("Save Tag") },
            text = {
                Column {
                    Text(
                        "Give this tag a name so you can monitor it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Tag name") },
                        placeholder = { Text("e.g. Keys, Wallet, Backpack") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog?.let { onSaveTag(it, nickname.ifBlank { "Tag" }) }
                        showSaveDialog = null
                        nickname = ""
                    },
                    enabled = nickname.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = null; nickname = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SavedTagCard(
    tag: TagEntity,
    scannedTag: ScannedTag?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val isNearby = scannedTag != null
    val distance = scannedTag?.let {
        DistanceCalculator.estimateDistance(it.rssi, tag.measuredPowerAt1m, tag.pathLossExponent)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isNearby) Color(0xFF4CAF50) else Color(0xFFEF5350)
                    )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tag.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isNearby) {
                        "${DistanceCalculator.formatDistance(distance ?: -1f)} · ${DistanceCalculator.signalQuality(scannedTag!!.rssi)} · ${scannedTag.batteryLevel.name}"
                    } else {
                        "Not detected"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Alert threshold indicator
            if (isNearby && distance != null) {
                val pct = (distance / tag.maxDistanceMeters * 100).toInt().coerceIn(0, 100)
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        pct >= 100 -> Color(0xFFEF5350)
                        pct >= 75 -> Color(0xFFFFA726)
                        else -> Color(0xFF4CAF50)
                    }
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun DiscoveredTagCard(
    tag: ScannedTag,
    onSave: () -> Unit,
) {
    val distance = DistanceCalculator.estimateDistance(tag.rssi)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SmartTag",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${DistanceCalculator.formatDistance(distance)} · ${tag.batteryLevel.name} · ${tag.tagState.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            FilledTonalButton(onClick = onSave) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save")
            }
        }
    }
}
