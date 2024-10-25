<!--suppress CheckImageSize -->
<h1 align="center">Compose Broadcasts</h1>

<p align="center">
    <img src="assets/logo.webp" width=250 alt="Compose Broadcasts Logo" />
</p>

<p align="center">
    <img src="https://img.shields.io/badge/Jetpack%20Compose-purple?style=for-the-badge" alt="Compose Badge" />
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Badge" />
    <img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin Badge" />
</p>

## 🚀 Introduction

Compose Broadcasts is a powerful Jetpack Compose library that simplifies the process of working with
Android's BroadcastReceivers in a composable environment. It provides an intuitive API to observe
and react to system-wide events and changes in your Compose UI.

## ✨ Features

- 🔄 Easy integration with Jetpack Compose
- 📡 Observe system events like battery level, airplane mode, and more
- 🎛️ Custom BroadcastReceiver support
- 🧩 Composable functions for common system events
- 🛠️ Flexible API for creating custom broadcast listeners
- ☮️ No need to worry about registering / unregistering listeners anymore

## 📦 Installation

<img alt="Sonatype Nexus (Releases)" src="https://img.shields.io/nexus/r/io.github.shubhamsinghshubham777/composebroadcasts?server=https%3A%2F%2Fs01.oss.sonatype.org&style=for-the-badge">

Add the following to your app's `build.gradle.kts`:

```kotlin
// Get the latest version from GitHub Releases/Tags
implementation("io.github.shubhamsinghshubham777:composebroadcasts:0.0.1")
```

### For SNAPSHOT versions

Add the following to your project level `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        ...
        // Add this
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
```

## 🛠️ Usage

Here's the complete list of composables Compose Broadcasts provides at the moment:

| **Composable**                   | **Return Type**    |
|----------------------------------|--------------------|
| rememberBroadcastReceiverAsState | Generic (T)        |
| rememberIsAirplaneModeOn         | Boolean            |
| rememberBatteryLevel             | Int                |
| rememberIsCharging               | Boolean            |
| rememberPackageInfo              | CBPackageInfo?     |
| rememberCurrentTimeMillis        | Long               |
| rememberSystemLocale             | Locale             |
| rememberIsScreenOn               | Boolean            |
| rememberIsHeadsetConnected       | Boolean            |
| rememberCurrentInputMethod       | CBInputMethodInfo? |

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

### Observe Package Changes

Check out the [🧩 Custom BroadcastReceivers](#-custom-broadcastreceivers) section below to learn
how to create PackageInfoReceiver.

```kotlin
val packageInfoReceiver = PackageInfoReceiver()
val packageInfo by rememberPackageInfo(packageInfoReceiver)
Text("Last package change: $packageInfo")
```

### Monitor System Time

```kotlin
val currentTimeMillis by rememberCurrentTimeMillis()
Text("Current time: ${convertMillisToTimeString(currentTimeMillis)}")
```

### Track System Locale Changes

Check out the [🧩 Custom BroadcastReceivers](#-custom-broadcastreceivers) section below to learn
how to create LocaleReceiver.

```kotlin
val localeReceiver = LocaleReceiver()
val currentLocale by rememberSystemLocale(localeReceiver)
Text("Current system locale: ${currentLocale.toLanguageTag()}")
```

## 🧩 Custom BroadcastReceivers

You can create custom BroadcastReceivers by extending the `CBBroadcastReceiver` class:

```kotlin
class MyCustomReceiver : CBBroadcastReceiver(tag = "my_custom_receiver") {
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        // Your custom logic here (if you like the old way of writing receivers)
        // Ideally, your logic should be a part of the composable itself
        // This class should just be left blank, for example:
        // class MyCustomReceiver : CBBroadcastReceiver(tag = "my_custom_receiver")
    }
}
```

Then, register the receiver in your AndroidManifest.xml file:

```xml

<manifest>
    <application>
        <receiver android:name=".MyCustomReceiver" android:exported="false">
            <intent-filter>
                <!-- Example: android.intent.action.PACKAGE_ADDED -->
                <action android:name="YOUR_CUSTOM_ACTION" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

Finally, use it in your composable:

```kotlin
val customState by rememberBroadcastReceiverAsState(
    initialValue = initialState,
    // Example: CBIntentAction.Custom(Intent.ACTION_PACKAGE_ADDED)
    intentFilters = listOf(CBIntentFilter(CBIntentAction.Custom("YOUR_CUSTOM_ACTION"))),
    broadcastReceiver = MyCustomReceiver(),
) { context, intent ->
    // Map the received intent to your state
}
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This library is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
