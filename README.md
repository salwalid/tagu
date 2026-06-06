# Tagu

Android BLE proximity monitor for Samsung SmartTags. Track your tags on any Android phone (including Pixel), get alerts when they move away, and monitor battery and motion — all without a Samsung account.

## Why Tagu?

Samsung SmartTags only work with Samsung Galaxy phones via SmartThings. Existing workarounds (like uTag) require installing modified Samsung APKs with removed security checks — running your Samsung credentials through patched proprietary code.

Tagu takes a different approach: **pure BLE scanning, no Samsung dependency**. It reads the tag's Bluetooth advertisements directly, giving you proximity monitoring and departure alerts without touching Samsung's servers or apps.

## Features

| Feature | How It Works |
|---------|-------------|
| **Tag Discovery** | Scans for Samsung SmartTag BLE advertisements (UUID FD5A) |
| **Proximity Alert** | Configurable distance threshold per tag — alerts when a tag moves beyond your set range |
| **Distance Monitoring** | Real-time RSSI-based distance estimation with signal strength graph |
| **Per-Tag Settings** | Nickname each tag, set individual alert distances (1m to 100m) |
| **Background Monitoring** | Foreground service keeps scanning when app is backgrounded |
| **Battery Status** | Reads battery level from BLE advertisement (Full/Medium/Low/Very Low) |
| **Motion Detection** | Shows when a tag is being moved (from advertisement payload) |
| **Tag State** | Shows connectivity state — connected, recently disconnected, or long-offline |
| **Multi-Tag Support** | Monitor multiple tags simultaneously with independent alert settings |

## What Tagu Cannot Do

These features require Samsung account authentication and encrypted GATT communication, which Tagu intentionally avoids for security:

- Ring/buzz the tag remotely
- Read exact battery percentage via GATT
- Use Samsung's Find My Everything crowd-sourced network
- Show tag location on a map (no GPS in tags — location comes from Samsung's network)
- Identify tags by their Samsung-registered name

## How It Works

### BLE Protocol

Samsung SmartTags broadcast on BLE service UUID `0000FD5A-0000-1000-8000-00805F9B34FB` with a 20-byte advertising payload:

```
Byte 0:     [Version:4][AdvType:1][TagState:3]
Bytes 1-3:  Aging Counter (time since disconnect)
Bytes 4-11: Privacy ID (8-byte rotating identifier)
Byte 12:    [Region:4][Encryption:1][UWB:1][Battery:2]
Byte 13:    [Motion:1][Reserved:7]
Bytes 14-15: Reserved / Activity
Bytes 16-19: Cryptographic Signature
```

### Tag Identification

SmartTags rotate their Privacy ID every **15 minutes** (or every 24 hours if disconnected >24h). Tagu handles this by:

1. Tracking tags by Privacy ID initially
2. When a Privacy ID disappears and a new one appears with similar RSSI, correlating them as the same physical tag
3. Storing the BLE MAC address pattern (also rotates, but on a different schedule)
4. User confirms re-association when prompted

### Distance Estimation

RSSI (signal strength) is converted to approximate distance using the log-distance path loss model:

```
distance = 10 ^ ((measuredPower - RSSI) / (10 * pathLossExponent))
```

- `measuredPower`: RSSI at 1 meter (calibrated per tag, default -59 dBm)
- `pathLossExponent`: Environment factor (2.0 = open air, 2.5 = indoor, 3.5 = obstructed)

Users can calibrate `measuredPower` by placing the tag 1 meter away and tapping "Calibrate."

### Alert System

When a monitored tag's estimated distance exceeds the user-configured threshold:

1. **Soft alert** (threshold × 0.8): Notification with tag name and current distance
2. **Hard alert** (threshold × 1.0): Alarm sound + vibration pattern
3. **Critical alert** (tag disappears from scan): Full alarm — tag is out of BLE range entirely

Alert sensitivity is configurable:
- **High**: Alert on single reading beyond threshold
- **Medium** (default): Alert after 3 consecutive readings beyond threshold (~6 seconds)
- **Low**: Alert after 5 consecutive readings (~10 seconds)

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Tagu Android App                      │
├─────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose + Material 3)                │
│  ├── TagListScreen      — All discovered/saved tags     │
│  ├── TagDetailScreen    — Single tag monitoring + config │
│  ├── AlertSettingsScreen — Per-tag alert thresholds      │
│  └── ScanSettingsScreen — BLE scan parameters            │
├─────────────────────────────────────────────────────────┤
│  ViewModel Layer                                         │
│  ├── TagListViewModel   — Tag discovery + state          │
│  └── TagDetailViewModel — Single tag monitoring          │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                           │
│  └── TagMonitorService  — Foreground service for         │
│      │                    background BLE scanning        │
│      ├── BleScanner     — Android BLE scan management    │
│      ├── TagParser      — Parse FD5A advertisement data  │
│      ├── DistanceCalc   — RSSI → distance conversion     │
│      └── AlertManager   — Threshold checks + alarms      │
├─────────────────────────────────────────────────────────┤
│  Data Layer                                              │
│  ├── Room Database      — Saved tags, settings, history  │
│  │   ├── TagEntity      — Tag config (name, threshold)   │
│  │   ├── ScanRecord     — RSSI/distance history          │
│  │   └── AlertEvent     — Alert log                      │
│  └── TagRepository      — Data access abstraction        │
└─────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (Material You) |
| Navigation | Compose Navigation |
| DI | Hilt (Dagger) |
| Database | Room (SQLite) |
| BLE | Android BluetoothLeScanner API |
| Background | Foreground Service with persistent notification |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle (Kotlin DSL) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

## BLE Protocol Reference

### Service UUID
```
Samsung SmartTag: 0000FD5A-0000-1000-8000-00805F9B34FB
```

### GATT Characteristics (for reference — requires encryption)

| UUID | Purpose |
|------|---------|
| DEE30001 | Ring control (write 0x01 start, 0x00 stop) |
| DEE30002 | Ring volume (0x01 low, 0x02 high) |
| DEE30004 | Battery level |
| DEE3000B | Firmware version |
| DEE3001C | Motion detection events |

*Note: GATT operations require Samsung's encryption layer. Tagu reads these values from the advertisement payload instead where possible.*

### Tag State Values
| Value | State | Meaning |
|-------|-------|---------|
| 1 | Premature Offline | Disconnected < 15 minutes |
| 2 | Offline | Disconnected 15 min to 24 hours |
| 3 | Overmature Offline | Disconnected > 24 hours |
| 4 | Connected (Owner) | Paired and connected to owner |
| 5 | Connected (Other) | Connected to non-owner |
| 6 | Connected (Two) | Connected to two devices |

### Battery Level Values
| Value | Level |
|-------|-------|
| 0 | Very Low |
| 1 | Low |
| 2 | Medium |
| 3 | Full |

### Privacy ID Rotation
| Tag State | Rotation Interval |
|-----------|-------------------|
| Premature/Offline | Every 15 minutes |
| Overmature Offline | Every 24 hours |

## Permissions Required

```xml
<!-- BLE scanning -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Location (required for BLE scanning on Android) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- Background service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Keep scanning while screen off -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## Building

```bash
# Clone
git clone https://github.com/salwalid/tagu.git
cd tagu

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Privacy & Security

- **No Samsung account required** — no credentials stored or transmitted
- **No network calls** — all processing is local, on-device
- **No cloud dependency** — works in airplane mode (BLE only)
- **No modified APKs** — pure Android BLE APIs
- **Open source** — full code audit possible
- **Minimal permissions** — only BLE scan, location (Android requirement for BLE), and foreground service

## Roadmap

- [ ] Core BLE scanner with FD5A filter
- [ ] Tag discovery and listing
- [ ] Privacy ID tracking and rotation handling
- [ ] RSSI-based distance estimation
- [ ] Per-tag alert thresholds (configurable meters)
- [ ] Background monitoring via foreground service
- [ ] Alert system (notification + alarm + vibration)
- [ ] RSSI calibration tool
- [ ] Signal strength history graph
- [ ] Tag re-association after Privacy ID rotation
- [ ] Export alert history
- [ ] Widget for quick tag status

## License

MIT

## Credits

BLE protocol documentation derived from the [uTag wiki](https://github.com/KieronQuinn/uTag/wiki) by Kieron Quinn (GPL-3.0). Tagu is an independent implementation that does not use any uTag code.
