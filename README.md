<!--suppress CheckImageSize -->
<h1 align="center">Compose Broadcasts</h1>

<p align="center">
    <img src="assets/logo.svg" width=250 alt="Compose Broadcasts Logo" />
</p>

<p align="center">
    <img src="https://img.shields.io/badge/Jetpack%20Compose-purple?style=for-the-badge" alt="Compose Badge" />
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Badge" />
    <img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin Badge" />
</p>

## 🚀 Introduction

Compose Broadcasts provides lifecycle-aware, Compose-first APIs for observing Android broadcasts in
UI code. It handles dynamic registration and cleanup for you and supports both state reducers and
event streams.

## ✨ Features

- 🔄 Easy integration with Jetpack Compose
- 📡 Observe system events like battery level, airplane mode, and more
- 🎛️ Custom broadcasts without manifest boilerplate
- 🧩 Composable functions for common system events
- 🛠️ Flexible API for creating custom broadcast listeners
- ☮️ Automatic registration and unregistration with the composition

## 📦 Installation

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.shubhamsinghshubham777/composebroadcasts)

Add the following to your app's `build.gradle.kts`:

```kotlin
// Get the latest version from GitHub Releases/Tags (or the badge shown above)
implementation("io.github.shubhamsinghshubham777:composebroadcasts:x.y.z")
```

Compose Broadcasts requires Android 6.0 (API 23) or later.

## 🛠️ Usage

The library currently provides these composables:

| **Composable**                   | **Return Type**    |
|----------------------------------|--------------------|
| `rememberBroadcastEvents` | `Flow<Intent>` |
| `rememberBroadcastReceiverAsState` | `State<T>` |
| `rememberIsAirplaneModeOn` | `State<Boolean>` |
| `rememberBatteryLevel` | `State<Int>` |
| `rememberIsCharging` | `State<Boolean>` |
| `rememberPackageInfo` | `State<CBPackageInfo?>` |
| `rememberCurrentTimeMillis` | `State<Long>` |
| `rememberSystemLocale` | `State<Locale>` |
| `rememberIsScreenOn` | `State<Boolean>` |
| `rememberIsHeadsetConnected` | `State<Boolean>` |
| `rememberCurrentInputMethod` | `State<CBInputMethodInfo?>` |
| `rememberIsPowerSaveMode` | `State<Boolean>` |
| `rememberIsLocationEnabled` | `State<Boolean>` |
| `rememberIsBluetoothEnabled` | `State<Boolean>` |
| `rememberIsNfcEnabled` | `State<Boolean>` |
| `rememberIsDeviceIdleMode` | `State<Boolean>` |

And here are some examples of how to use them in your project:

### Observe Airplane Mode

```kotlin
val isAirplaneModeOn by rememberIsAirplaneModeOn()
Text("Airplane mode is ${if (isAirplaneModeOn) "ON" else "OFF"}")
```

### Monitor Battery Level

```kotlin
val batteryLevel by rememberBatteryLevel()
Text("Current battery level: $batteryLevel%")
```

### Track Device Charging Status

```kotlin
val isCharging by rememberIsCharging()
Text("Device is ${if (isCharging) "charging" else "not charging"}")
```

### Monitor Power Saver and Location Services

```kotlin
val isPowerSaveMode by rememberIsPowerSaveMode()
val isLocationEnabled by rememberIsLocationEnabled()
val isBluetoothEnabled by rememberIsBluetoothEnabled()
val isNfcEnabled by rememberIsNfcEnabled()
val isDeviceIdleMode by rememberIsDeviceIdleMode()
```

These convenience methods only observe and expose current system state. The library never requests
permissions. The host application must declare and request any permission required by Android for
the state it observes; `rememberIsBluetoothEnabled` requires `BLUETOOTH_CONNECT` on Android 12
(API 31) and above.

### Observe Package Changes

Package and locale observers are dynamic, foreground registrations. They observe broadcasts while
the composable is active; they do not persist UI state or monitor the app while its process is
absent.

```kotlin
// CBBroadcastReceiver can be supplied by your application.
val packageInfoReceiver = remember { CBBroadcastReceiver("package-events") }
val packageInfo by rememberPackageInfo(packageInfoReceiver)
Text("Last package change: $packageInfo")
```

### Monitor System Time

```kotlin
val currentTimeMillis by rememberCurrentTimeMillis()
Text("Current time: ${convertMillisToTimeString(currentTimeMillis)}")
```

### Track System Locale Changes

```kotlin
val localeReceiver = remember { CBBroadcastReceiver("locale-events") }
val currentLocale by rememberSystemLocale(localeReceiver)
Text("Current system locale: ${currentLocale.toLanguageTag()}")
```

## 🧩 Custom broadcasts

For most use cases, no receiver subclass is needed. The state overload creates and owns a receiver
for the current composition:

```kotlin
val customState by rememberBroadcastReceiverAsState(
    initialValue = 0,
    intentFilters = listOf(
        CBIntentFilter(CBIntentAction.Custom("com.example.SYNC_COMPLETED")),
    ),
) { _, _ ->
    1
}
```

Use the receiver-owned overload when the same receiver needs to be shared or configured outside
the composable:

```kotlin
val receiver = remember { CBBroadcastReceiver("sync-events") }
val customState by rememberBroadcastReceiverAsState(
    initialValue = false,
    intentFilters = listOf(CBIntentFilter(CBIntentAction.Custom("com.example.SYNC_COMPLETED"))),
    broadcastReceiver = receiver,
) { _, _ -> true }
```

For event-style broadcasts, collect the generic stream instead of reducing it to state:

```kotlin
val events = rememberBroadcastEvents(
    intentFilters = listOf(CBIntentFilter(CBIntentAction.Custom("YOUR_CUSTOM_ACTION"))),
)
LaunchedEffect(events) {
    events.collect { intent -> /* handle intent */ }
}
```

### Test a real Android system broadcast

Power Saver changes send `PowerManager.ACTION_POWER_SAVE_MODE_CHANGED`. Compose Broadcasts does not
provide a convenience composable for this action, so it is a useful end-to-end example:

```kotlin
import android.os.PowerManager
import androidx.compose.runtime.getValue

val context = LocalContext.current
val powerManager = remember {
    context.getSystemService(Context.POWER_SERVICE) as PowerManager
}
val isPowerSaveMode by rememberBroadcastReceiverAsState(
    initialValue = powerManager.isPowerSaveMode,
    intentFilters = listOf(
        CBIntentFilter(
            CBIntentAction.Custom(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        ),
    ),
) { _, _ -> powerManager.isPowerSaveMode }

Text("Power Saver: ${if (isPowerSaveMode) "ON" else "OFF"}")
```

Run the app on an emulator or connected device, then toggle the setting from a terminal:

```shell
adb shell settings put global low_power 1  # turn on
adb shell settings put global low_power 0  # turn off
```

You can also toggle Battery Saver from the device's Quick Settings panel. This is a dynamically
registered, foreground observation; it does not require a manifest receiver.

### Lifecycle and Android restrictions

- Registrations exist only while the composable is active. They are suitable for foreground UI
  observation, not background work or durable event storage.
- `receiverExported = false` is the default. Set it to `true` only when broadcasts from other apps
  are required, and validate the security implications of doing so.
- A manifest receiver and a dynamically registered receiver have separate lifecycles. Use a
  manifest receiver, WorkManager, or another background mechanism when work must continue after the
  process or UI is gone.
- Android may restrict implicit, protected, sticky, and background broadcasts. The library does not
  bypass platform permissions or broadcast restrictions; consult the Android broadcast documentation
  for the action you observe.
- Each `CBIntentFilter` keeps its own data scheme and MIME type constraints. Invalid MIME types are
  rejected during registration with an explanatory exception.

Keep receiver instances stable with `remember` when passing them to a composable. The library
supports multiple active subscriptions, including multiple call sites using the same receiver.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This library is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
