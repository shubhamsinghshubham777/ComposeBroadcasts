/*
 * Copyright (c) 2024 Shubham Singh
 *
 * This library is licensed under the Apache 2.0 License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package compose.broadcasts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * @param receiverExported Only applicable to Android 31 (Tiramisu) and above
 */
@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
fun <T> rememberBroadcastReceiverAsState(
    initialValue: T,
    intentFilters: List<CBIntentFilter>,
    broadcastReceiver: CBBroadcastReceiver,
    receiverExported: Boolean = false,
    mapToState: (Context, Intent) -> T,
): State<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentLifecycleState by rememberCurrentLifecycleState()
    val state = remember { mutableStateOf(initialValue) }

    LaunchedEffect(currentLifecycleState) {
        if (currentLifecycleState == Lifecycle.State.DESTROYED) {
            CBDataContainer.dispose()
        }
    }

    DisposableEffect(Unit) {
        CBDataContainer.addFlow(broadcastReceiver.tag)
        coroutineScope.launch {
            val actions = intentFilters.map { it.action.rawValue }
            CBDataContainer.flows[broadcastReceiver.tag]?.collect { intent ->
                if (intent != null && actions.contains(intent.action)) {
                    state.value = mapToState(context, intent)
                }
            }
        }

        val combinedIntentFilter = IntentFilter().apply {
            for (filter in intentFilters) {
                addAction(filter.action.rawValue)
                filter.dataType?.rawValue?.let(::addDataType)
                filter.dataScheme?.rawValue?.let(::addDataScheme)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                broadcastReceiver,
                combinedIntentFilter,
                if (receiverExported) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context.registerReceiver(broadcastReceiver, combinedIntentFilter)
        }
        Log.d(LOG_TAG, "Registered receiver for tag: ${broadcastReceiver.tag}")

        onDispose {
            context.unregisterReceiver(broadcastReceiver)
            Log.d(LOG_TAG, "Unregistered receiver for tag: ${broadcastReceiver.tag}")
        }
    }

    return state
}

@Composable
fun rememberIsAirplaneModeOn(): State<Boolean> {
    val context = LocalContext.current
    return rememberBroadcastReceiverAsState(
        initialValue = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON
        ) != 0,
        intentFilters = listOf(CBIntentFilter(action = CBIntentAction.AirplaneModeChanged)),
        broadcastReceiver = CBBroadcastReceiver("airplane_mode"),
    ) { receiverContext, _ ->
        Settings.Global.getInt(
            receiverContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON
        ) != 0
    }
}

@Composable
fun rememberBatteryLevel(): State<Int> = rememberBroadcastReceiverAsState(
    initialValue = fetchBatteryLevel(LocalContext.current),
    intentFilters = listOf(CBIntentFilter(action = CBIntentAction.BatteryChanged)),
    broadcastReceiver = CBBroadcastReceiver("battery_level"),
) { _, intent ->
    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val batteryLevel = (level / scale.toFloat()) * 100
    return@rememberBroadcastReceiverAsState batteryLevel.roundToInt()
}

private fun fetchBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun rememberIsCharging(): State<Boolean> = rememberBroadcastReceiverAsState(
    initialValue = fetchIsCharging(LocalContext.current),
    intentFilters = listOf(CBIntentFilter(CBIntentAction.BatteryChanged)),
    broadcastReceiver = CBBroadcastReceiver("is_charging"),
) { _, intent ->
    val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return@rememberBroadcastReceiverAsState status == BatteryManager.BATTERY_STATUS_CHARGING
            || status == BatteryManager.BATTERY_STATUS_FULL
}

@RequiresApi(Build.VERSION_CODES.M)
private fun fetchIsCharging(context: Context): Boolean {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.isCharging
}

@Composable
fun rememberPackageInfo(broadcastReceiver: CBBroadcastReceiver): State<CBPackageInfo?> =
    rememberBroadcastReceiverAsState(
        initialValue = null,
        intentFilters = listOf(
            CBIntentFilter(
                action = CBIntentAction.PackageAdded,
                dataScheme = CBIntentDataScheme.Package,
            ),
            CBIntentFilter(
                action = CBIntentAction.PackageRemoved,
                dataScheme = CBIntentDataScheme.Package,
            ),
            CBIntentFilter(
                action = CBIntentAction.PackageReplaced,
                dataScheme = CBIntentDataScheme.Package,
            ),
        ),
        broadcastReceiver = broadcastReceiver,
    ) { _, intent ->
        val packageName = intent.data?.schemeSpecificPart
            ?: throw Exception("Package name not found!")
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        return@rememberBroadcastReceiverAsState CBPackageInfo(
            packageName = packageName,
            action = when {
                replacing -> CBPackageAction.Replaced
                intent.action == CBIntentAction.PackageAdded.rawValue -> CBPackageAction.Added
                intent.action == CBIntentAction.PackageRemoved.rawValue -> CBPackageAction.Removed
                else -> CBPackageAction.Unknown
            },
        )
    }

@Composable
fun rememberCurrentTimeMillis(): State<Long> = rememberBroadcastReceiverAsState(
    initialValue = System.currentTimeMillis(),
    intentFilters = listOf(CBIntentFilter(CBIntentAction.TimeTick)),
    broadcastReceiver = CBBroadcastReceiver("current_time_millis"),
) { _, _ -> System.currentTimeMillis() }

/**
 * We have observed that this composable does not work without an explicitly provided receiver.
 */
@Composable
fun rememberCurrentLocale(broadcastReceiver: CBBroadcastReceiver): State<Locale> =
    rememberBroadcastReceiverAsState(
        initialValue = Locale.getDefault(),
        intentFilters = listOf(CBIntentFilter(CBIntentAction.LocaleChanged)),
        broadcastReceiver = broadcastReceiver,
    ) { _, _ -> Locale.getDefault() }

@Composable
fun rememberIsScreenOn(): State<Boolean> {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    return rememberBroadcastReceiverAsState(
        initialValue = powerManager.isInteractive,
        intentFilters = listOf(
            CBIntentFilter(CBIntentAction.ScreenOn),
            CBIntentFilter(CBIntentAction.ScreenOff),
        ),
        broadcastReceiver = CBBroadcastReceiver("is_screen_on"),
    ) { _, _ -> powerManager.isInteractive }
}

@Composable
fun rememberIsHeadsetConnected(): State<Boolean> {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    return rememberBroadcastReceiverAsState(
        initialValue = fetchIsHeadsetConnected(audioManager),
        intentFilters = listOf(CBIntentFilter(CBIntentAction.HeadsetPlug)),
        broadcastReceiver = CBBroadcastReceiver("headset_info"),
    ) { _, _ -> fetchIsHeadsetConnected(audioManager) }
}

private fun fetchIsHeadsetConnected(audioManager: AudioManager): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { device ->
                val hasUsbHeadset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET
                } else false

                val hasBluetoothHeadset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                } else false

                return@filter device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || hasBluetoothHeadset
                        || hasUsbHeadset
            }
            .isNotEmpty()
    } else {
        // TODO(Shubham): Need to verify/test this logic
        @Suppress("DEPRECATION")
        audioManager.isWiredHeadsetOn
    }
}

@Composable
fun rememberCurrentInputMethod(): State<CBInputMethodInfo?> {
    val context = LocalContext.current
    return rememberBroadcastReceiverAsState(
        initialValue = fetchCurrentInputMethodInfo(context),
        intentFilters = listOf(CBIntentFilter(CBIntentAction.InputMethodChanged)),
        broadcastReceiver = CBBroadcastReceiver("input_method"),
    ) { _, _ -> fetchCurrentInputMethodInfo(context) }
}

private fun fetchCurrentInputMethodInfo(context: Context): CBInputMethodInfo? {
    val inputMethodFullQualifier = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )

    val extractedPackageName = inputMethodFullQualifier.substringBefore('/')

    return try {
        if (context.packageManager == null) return null

        val info = context.packageManager.getPackageInfo(extractedPackageName, 0)
            ?: return null

        CBInputMethodInfo(
            name = context.packageManager.getApplicationLabel(info.applicationInfo).toString(),
            packageName = info.packageName,
            fullQualifier = inputMethodFullQualifier,
            appIcon = context.packageManager.getApplicationIcon(info.packageName),
        )
    } catch (e: NameNotFoundException) {
        null
    }
}
