/*
 * Copyright (c) 2024 Shubham Singh
 *
 * This library is licensed under the Apache 2.0 License.
 */

package compose.broadcasts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Returns a stream of broadcasts received while this composable is in the composition.
 * Registration is dynamic and in-process; it does not replace a manifest receiver needed for
 * background work. The returned flow is cold from the caller's perspective and is backed by a
 * subscription owned by this composition.
 */
@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
fun rememberBroadcastEvents(
    intentFilters: List<CBIntentFilter>,
    broadcastReceiver: CBBroadcastReceiver = remember { CBBroadcastReceiver("compose-broadcasts-${Any()}") },
    receiverExported: Boolean = false,
): Flow<Intent> {
    val context = LocalContext.current.applicationContext
    val normalizedFilters = remember(intentFilters) { intentFilters.toList() }
    val subscription = remember(context, broadcastReceiver, normalizedFilters, receiverExported) {
        broadcastReceiver.subscribe()
    }

    DisposableEffect(context, broadcastReceiver, normalizedFilters, receiverExported, subscription) {
        val intentFiltersForRegistration = buildIntentFilters(normalizedFilters)
        val registrationReceiver = broadcastReceiver.registrationReceiver(subscription)
        try {
            intentFiltersForRegistration.forEach { filter ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        registrationReceiver,
                        filter,
                        if (receiverExported) Context.RECEIVER_EXPORTED
                        else Context.RECEIVER_NOT_EXPORTED,
                    )
                } else {
                    context.registerReceiver(registrationReceiver, filter)
                }
            }
        } catch (error: RuntimeException) {
            subscription.close()
            throw IllegalArgumentException(
                "Could not register broadcast receiver for filters $normalizedFilters. " +
                        "Check that each MIME type and data scheme is valid.", error,
            )
        }

        onDispose {
            runCatching { context.unregisterReceiver(registrationReceiver) }
            subscription.close()
        }
    }

    return subscription.events
}

private fun buildIntentFilters(filters: List<CBIntentFilter>): List<IntentFilter> {
    require(filters.isNotEmpty()) { "At least one CBIntentFilter is required." }
    return filters.groupBy { it.dataType?.rawValue to it.dataScheme?.rawValue }.map { (data, entries) ->
        IntentFilter().apply {
            entries.forEach { addAction(it.action.rawValue) }
            data.first?.let {
                try {
                    addDataType(it)
                } catch (error: IntentFilter.MalformedMimeTypeException) {
                    throw IllegalArgumentException("Invalid MIME type '$it' in CBIntentFilter.", error)
                }
            }
            data.second?.let { addDataScheme(it) }
        }
    }
}

/** State reducer built on [rememberBroadcastEvents]. `initialValue` applies when the receiver
 * instance changes or on the first composition; mapper changes take effect immediately. */
@Composable
fun <T> rememberBroadcastReceiverAsState(
    initialValue: T,
    intentFilters: List<CBIntentFilter>,
    broadcastReceiver: CBBroadcastReceiver,
    receiverExported: Boolean = false,
    mapToState: (Context, Intent) -> T,
): State<T> {
    if (!broadcastReceiver.isLibraryProvidedTag) {
        check(!broadcastReceiver.tag.isComposeBroadcastsTag) {
            "The provided `tag` value (${broadcastReceiver.tag}) is already in-use by a " +
                    "library-provided function. Please choose another unique tag."
        }
    }
    val context = LocalContext.current
    val events = rememberBroadcastEvents(intentFilters, broadcastReceiver, receiverExported)
    val state = remember(broadcastReceiver) { mutableStateOf(initialValue) }
    val currentMapper by rememberUpdatedState(mapToState)

    LaunchedEffect(events, context, broadcastReceiver) {
        events.collect { intent -> state.value = currentMapper(context, intent) }
    }
    return state
}

/** Convenience overload for dynamic, in-process observation without defining a receiver class. */
@Composable
fun <T> rememberBroadcastReceiverAsState(
    initialValue: T,
    intentFilters: List<CBIntentFilter>,
    receiverExported: Boolean = false,
    mapToState: (Context, Intent) -> T,
): State<T> = rememberBroadcastReceiverAsState(
    initialValue = initialValue,
    intentFilters = intentFilters,
    broadcastReceiver = remember { CBBroadcastReceiver("compose-broadcasts-${intentFilters.hashCode()}") },
    receiverExported = receiverExported,
    mapToState = mapToState,
)

@Composable
fun rememberIsAirplaneModeOn(): State<Boolean> {
    val context = LocalContext.current
    return rememberBroadcastReceiverAsState(
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON) != 0,
        listOf(CBIntentFilter(CBIntentAction.AirplaneModeChanged)),
        CBBroadcastReceiver(CBConstants.AIRPLANE_MODE.value, true),
    ) { receiverContext, _ ->
        Settings.Global.getInt(receiverContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON) != 0
    }
}

@Composable
fun rememberBatteryLevel(): State<Int> = rememberBroadcastReceiverAsState(
    fetchBatteryLevel(LocalContext.current),
    listOf(CBIntentFilter(CBIntentAction.BatteryChanged)),
    CBBroadcastReceiver(CBConstants.BATTERY_LEVEL.value, true),
) { _, intent ->
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) 0 else ((level / scale.toFloat()) * 100).roundToInt()
}

private fun fetchBatteryLevel(context: Context): Int =
    (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

@Composable
fun rememberIsCharging(): State<Boolean> = rememberBroadcastReceiverAsState(
    fetchIsCharging(LocalContext.current),
    listOf(CBIntentFilter(CBIntentAction.BatteryChanged)),
    CBBroadcastReceiver(CBConstants.IS_CHARGING.value, true),
) { _, intent ->
    intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) in
            setOf(BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL)
}

private fun fetchIsCharging(context: Context): Boolean =
    (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).isCharging

/** Observes package changes while the UI is composed. A manifest receiver is not required for this
 * foreground observation and cannot be used to persist UI state while the process is absent. */
@Composable
fun rememberPackageInfo(broadcastReceiver: CBBroadcastReceiver): State<CBPackageInfo?> =
    rememberBroadcastReceiverAsState(
        null,
        listOf(
            CBIntentFilter(CBIntentAction.PackageAdded, dataScheme = CBIntentDataScheme.Package),
            CBIntentFilter(CBIntentAction.PackageRemoved, dataScheme = CBIntentDataScheme.Package),
            CBIntentFilter(CBIntentAction.PackageReplaced, dataScheme = CBIntentDataScheme.Package),
        ),
        broadcastReceiver,
    ) { _, intent ->
        val packageName = intent.data?.schemeSpecificPart ?: return@rememberBroadcastReceiverAsState null
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        CBPackageInfo(
            packageName,
            when {
                replacing -> CBPackageAction.Replaced
                intent.action == CBIntentAction.PackageAdded.rawValue -> CBPackageAction.Added
                intent.action == CBIntentAction.PackageRemoved.rawValue -> CBPackageAction.Removed
                else -> CBPackageAction.Unknown
            },
        )
    }

@Composable
fun rememberCurrentTimeMillis(): State<Long> = rememberBroadcastReceiverAsState(
    System.currentTimeMillis(), listOf(CBIntentFilter(CBIntentAction.TimeTick)),
    CBBroadcastReceiver(CBConstants.CURRENT_TIME_MILLIS.value, true),
) { _, _ -> System.currentTimeMillis() }

@Composable
fun rememberSystemLocale(broadcastReceiver: CBBroadcastReceiver): State<Locale> =
    rememberBroadcastReceiverAsState(
        LocalLocale.current.platformLocale,
        listOf(CBIntentFilter(CBIntentAction.LocaleChanged)),
        broadcastReceiver,
    ) { _, _ -> Locale.getDefault() }

@Composable
fun rememberIsScreenOn(): State<Boolean> {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    return rememberBroadcastReceiverAsState(
        powerManager.isInteractive,
        listOf(CBIntentFilter(CBIntentAction.ScreenOn), CBIntentFilter(CBIntentAction.ScreenOff)),
        CBBroadcastReceiver(CBConstants.IS_SCREEN_ON.value, true),
    ) { _, _ -> powerManager.isInteractive }
}

@Composable
fun rememberIsHeadsetConnected(): State<Boolean> {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    return rememberBroadcastReceiverAsState(
        fetchIsHeadsetConnected(audioManager), listOf(CBIntentFilter(CBIntentAction.HeadsetPlug)),
        CBBroadcastReceiver(CBConstants.HEADSET_INFO.value, true),
    ) { _, _ -> fetchIsHeadsetConnected(audioManager) }
}

private fun fetchIsHeadsetConnected(audioManager: AudioManager): Boolean = audioManager
    .getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
        val usb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.type == AudioDeviceInfo.TYPE_USB_HEADSET
        val bleHeadset = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        val bleSpeaker = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
        device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || usb || bleHeadset || bleSpeaker
    }

@Composable
fun rememberCurrentInputMethod(): State<CBInputMethodInfo?> {
    val context = LocalContext.current
    return rememberBroadcastReceiverAsState(
        fetchCurrentInputMethodInfo(context), listOf(CBIntentFilter(CBIntentAction.InputMethodChanged)),
        CBBroadcastReceiver(CBConstants.INPUT_METHOD.value, true),
    ) { receiverContext, _ -> fetchCurrentInputMethodInfo(receiverContext) }
}

/** Observes whether Android's Battery Saver / Power Save mode is enabled. */
@Composable
fun rememberIsPowerSaveMode(): State<Boolean> {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    return rememberBroadcastReceiverAsState(
        powerManager.isPowerSaveMode,
        listOf(CBIntentFilter(CBIntentAction.PowerSaveModeChanged)),
        CBBroadcastReceiver(CBConstants.POWER_SAVE_MODE.value, true),
    ) { _, _ -> powerManager.isPowerSaveMode }
}

/** Observes whether location services are enabled. */
@Composable
fun rememberIsLocationEnabled(): State<Boolean> {
    val context = LocalContext.current
    val locationManager = remember(context) {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    return rememberBroadcastReceiverAsState(
        fetchIsLocationEnabled(context, locationManager),
        listOf(CBIntentFilter(CBIntentAction.LocationModeChanged)),
        CBBroadcastReceiver(CBConstants.LOCATION_MODE.value, true),
    ) { receiverContext, _ -> fetchIsLocationEnabled(receiverContext, locationManager) }
}

@Suppress("DEPRECATION")
private fun fetchIsLocationEnabled(context: Context, locationManager: LocationManager): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF,
        ) != Settings.Secure.LOCATION_MODE_OFF
    }

private fun fetchCurrentInputMethodInfo(context: Context): CBInputMethodInfo? {
    val qualifier = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        ?: return null
    val packageName = qualifier.substringBefore('/')
    return try {
        val info = context.packageManager.getPackageInfo(packageName, 0)
        CBInputMethodInfo(
            name = context.packageManager.getApplicationLabel(info.applicationInfo!!).toString(),
            packageName = info.packageName,
            fullQualifier = qualifier,
            appIcon = context.packageManager.getApplicationIcon(info.packageName),
        )
    } catch (_: NameNotFoundException) {
        null
    }
}
