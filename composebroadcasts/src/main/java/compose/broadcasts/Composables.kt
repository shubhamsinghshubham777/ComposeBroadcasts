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
import android.content.BroadcastReceiver
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
 * Provides an easier way to consume Android's [BroadcastReceiver]s from composables. Accepts a
 * special variant of the BroadcastReceiver named [CBBroadcastReceiver] (`CB` stands for
 * `Compose Broadcasts`) along with a list of intent filters used to filter out the exact
 * type/event of values you want to receive.
 *
 * @param initialValue value to initialise the returned state object with.
 * @param intentFilters used to filter out the type of content required from the provided
 * [broadcastReceiver].
 * @param broadcastReceiver this library's variant of [BroadcastReceiver] to observe values from and
 * provide to the returned state object.
 * @param receiverExported whether [broadcastReceiver] can receive broadcasts from other apps. Only
 * applicable on API 31 (Tiramisu) and above.
 * @param mapToState fetches value from the received intent and transforms/maps it to the value
 * that is provided to the returned state object.
 *
 * @return A compose [State] object that contains the values received by the provided
 * [broadcastReceiver].
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

/**
 * Util composable function that observes [Intent.ACTION_AIRPLANE_MODE_CHANGED] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * @return A boolean that depicts whether the device's Airplane Mode is on.
 */
@Composable
fun rememberIsAirplaneModeOn(): State<Boolean> {
    val context = LocalContext.current
    return rememberBroadcastReceiverAsState(
        initialValue = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON
        ) != 0,
        intentFilters = listOf(CBIntentFilter(action = CBIntentAction.AirplaneModeChanged)),
        broadcastReceiver = CBBroadcastReceiver(CBConstants.AIRPLANE_MODE.value),
    ) { receiverContext, _ ->
        Settings.Global.getInt(
            receiverContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON
        ) != 0
    }
}

/**
 * Util composable function that observes [Intent.ACTION_BATTERY_CHANGED] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * @return An integer ranging between 1 and 100 depicting the current battery percentage.
 */
@Composable
fun rememberBatteryLevel(): State<Int> = rememberBroadcastReceiverAsState(
    initialValue = fetchBatteryLevel(LocalContext.current),
    intentFilters = listOf(CBIntentFilter(action = CBIntentAction.BatteryChanged)),
    broadcastReceiver = CBBroadcastReceiver(CBConstants.BATTERY_LEVEL.value),
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

/**
 * Util composable function that observes [Intent.ACTION_BATTERY_CHANGED] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * @return A boolean depicting whether the device is currently charging.
 */
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun rememberIsCharging(): State<Boolean> = rememberBroadcastReceiverAsState(
    initialValue = fetchIsCharging(LocalContext.current),
    intentFilters = listOf(CBIntentFilter(CBIntentAction.BatteryChanged)),
    broadcastReceiver = CBBroadcastReceiver(CBConstants.IS_CHARGING.value),
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

/**
 * Util composable function that observes [Intent.ACTION_PACKAGE_ADDED],
 * [Intent.ACTION_PACKAGE_REMOVED], and [Intent.ACTION_PACKAGE_REPLACED] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * Since it is an implicit broadcast, it requires the user to explicitly set it up in his
 * AndroidManifest.xml file which he can easily do by following these steps:
 *
 * 1. Create an empty [CBBroadcastReceiver], for example:
 *      ```kotlin
 *      class PackageInfoReceiver : CBBroadcastReceiver(tag = "my_package_info_receiver")
 *      // Make sure to use a unique `tag` value that you DO NOT use with any other CBBroadcastReceiver.
 *      ```
 * 2. Register this receiver in your AndroidManifest.xml file, for example:
 *      ```xml
 *      <manifest>
 *          <!-- A <queries> declaration should generally be used instead of QUERY_ALL_PACKAGES; see https://g.co/dev/packagevisibility for details -->
 *          <uses-permission
 *              android:name="android.permission.QUERY_ALL_PACKAGES"
 *              tools:ignore="QueryAllPackagesPermission" />
 *
 *          <application>
 *              <activity>
 *                  ...
 *              <activity/>
 *
 *              <receiver
 *                  android:name=".PackageInfoReceiver"
 *                  android:exported="false">
 *                  <intent-filter>
 *                          <action android:name="android.intent.action.PACKAGE_ADDED" />
 *                          <action android:name="android.intent.action.PACKAGE_REMOVED" />
 *                          <action android:name="android.intent.action.PACKAGE_REPLACED" />
 *                  </intent-filter>
 *              </receiver>
 *          <application/>
 *     <manifest />
 *      ```
 * 3. Finally, create an object of the receiver in your Activity (or Composable), for example:
 *      ```kotlin
 *      class MainActivity : ComponentActivity() {
 *       private val packageInfoReceiver = PackageInfoReceiver()
 *
 *       override fun onCreate(savedInstanceState: Bundle?) {
 *               ...
 *               MaterialTheme {
 *                   val packageInfo by rememberSystemLocale(packageInfoReceiver)
 *                   Text("Last interacted-with app's packageInfo is: $packageInfo")
 *               }
 *           }
 *       }
 *      ```
 * > **Note**: No need to call `packageInfoReceiver.registerReceiver()` or
 * `packageInfoReceiver.unregisterReceiver()`, the composable does it for you.
 *
 * Read more about which Broadcast Receivers you will need to do this for
 * [here](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions).
 *
 * @return An instance of [CBPackageInfo] depicting the addition, removal, or replacement of any
 * app on-device.
 */
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

/**
 * Util composable function that observes [Intent.ACTION_TIME_TICK] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * @return The current system time (in milliseconds) which is updated every minute when the OS
 * sends a broadcast event.
 */
@Composable
fun rememberCurrentTimeMillis(): State<Long> = rememberBroadcastReceiverAsState(
    initialValue = System.currentTimeMillis(),
    intentFilters = listOf(CBIntentFilter(CBIntentAction.TimeTick)),
    broadcastReceiver = CBBroadcastReceiver(CBConstants.CURRENT_TIME_MILLIS.value),
) { _, _ -> System.currentTimeMillis() }

/**
 * Util composable function that observes [Intent.ACTION_LOCALE_CHANGED] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * Since it is an implicit broadcast, it requires the user to explicitly set it up in his
 * AndroidManifest.xml file which he can easily do by following these steps:
 *
 * 1. Create an empty [CBBroadcastReceiver], for example:
 *      ```kotlin
 *      class LocaleReceiver : CBBroadcastReceiver(tag = "my_locale_receiver")
 *      // Make sure to use a unique `tag` value that you DO NOT use with any other CBBroadcastReceiver.
 *      ```
 * 2. Register this receiver in your AndroidManifest.xml file, for example:
 *      ```xml
 *      <manifest>
 *          <application>
 *              <activity>
 *                  ...
 *              <activity/>
 *
 *              <receiver
 *                  android:name=".LocaleReceiver"
 *                  android:exported="false">
 *                  <intent-filter>
 *                          <action android:name="android.intent.action.LOCALE_CHANGED" />
 *                  </intent-filter>
 *              </receiver>
 *          <application/>
 *     <manifest />
 *      ```
 * 3. Finally, create an object of the receiver in your Activity (or Composable), for example:
 *      ```kotlin
 *      class MainActivity : ComponentActivity() {
 *       private val localeReceiver = LocaleReceiver()
 *
 *       override fun onCreate(savedInstanceState: Bundle?) {
 *               ...
 *               MaterialTheme {
 *                   val locale by rememberSystemLocale(localeReceiver)
 *                   Text("Current locale is: ${locale.toLanguageTag()}")
 *               }
 *           }
 *       }
 *      ```
 * > **Note**: No need to call `localeReceiver.registerReceiver()` or
 * `localeReceiver.unregisterReceiver()`, the composable does it for you.
 *
 * Read more about which Broadcast Receivers you will need to do this for
 * [here](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions).
 *
 * @return The current system (not per-app) [Locale] which is updated every time the user changes
 * system language/region.
 */
@Composable
fun rememberSystemLocale(broadcastReceiver: CBBroadcastReceiver): State<Locale> =
    rememberBroadcastReceiverAsState(
        initialValue = Locale.getDefault(),
        intentFilters = listOf(CBIntentFilter(CBIntentAction.LocaleChanged)),
        broadcastReceiver = broadcastReceiver,
    ) { _, _ -> Locale.getDefault() }

/**
 * Util composable function that observes [Intent.ACTION_SCREEN_ON] and [Intent.ACTION_SCREEN_OFF]
 * with the help of [rememberBroadcastReceiverAsState].
 *
 * @return Whether the device's screen is currently turned on (and interactive).
 */
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
        broadcastReceiver = CBBroadcastReceiver(CBConstants.IS_SCREEN_ON.value),
    ) { _, _ -> powerManager.isInteractive }
}

/**
 * Util composable function that observes [Intent.ACTION_HEADSET_PLUG] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * @return Whether the user has currently connected a headset/headphone/earphone to the device.
 */
@Composable
fun rememberIsHeadsetConnected(): State<Boolean> {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    return rememberBroadcastReceiverAsState(
        initialValue = fetchIsHeadsetConnected(audioManager),
        intentFilters = listOf(CBIntentFilter(CBIntentAction.HeadsetPlug)),
        broadcastReceiver = CBBroadcastReceiver(CBConstants.HEADSET_INFO.value),
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

                val isBLESpeaker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                } else false

                return@filter device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || hasBluetoothHeadset
                        || hasUsbHeadset
                        || isBLESpeaker
            }
            .isNotEmpty()
    } else {
        // TODO(Shubham): Need to verify/test this logic
        @Suppress("DEPRECATION")
        audioManager.isWiredHeadsetOn
    }
}

/**
 * Util composable function that observes [Intent.ACTION_INPUT_METHOD_CHANGED] with the help of
 * [rememberBroadcastReceiverAsState].
 *
 * @return An instance of [CBInputMethodInfo] that provides basic information about the current
 * input method (like the `name`, `packageName`, and `icon` of the app it belongs to). Returns
 * `null` in case no input method is available in the operating system.
 */
@Composable
fun rememberCurrentInputMethod(): State<CBInputMethodInfo?> {
    val context = LocalContext.current
    return rememberBroadcastReceiverAsState(
        initialValue = fetchCurrentInputMethodInfo(context),
        intentFilters = listOf(CBIntentFilter(CBIntentAction.InputMethodChanged)),
        broadcastReceiver = CBBroadcastReceiver(CBConstants.INPUT_METHOD.value),
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
