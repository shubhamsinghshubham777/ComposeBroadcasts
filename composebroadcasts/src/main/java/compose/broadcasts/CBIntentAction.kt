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

import android.content.Intent
import android.bluetooth.BluetoothAdapter
import android.location.LocationManager
import android.nfc.NfcAdapter
import android.os.PowerManager
import compose.broadcasts.CBIntentAction.Custom

/**
 * A CBIntentAction depicts the type of event a user would want to filter his broadcast receiver's
 * emitted intents for.
 *
 * Some examples are: `AirplaneModeChanged`, `BatteryChanged`, `LocaleChanged`,
 * and many more. The ones that are mentioned in this list are the ones for which this library has
 * created util functions for.
 *
 * For the ones that are not listed yet, the user can make use of the [Custom] action and provide
 * it with his own `Intent.ACTION_*` value (where `*` can be any valid action intent like
 * `Intent.ACTION_APP_ERROR`, `Intent.ACTION_ASSIST`, `Intent.ACTION_ATTACH_DATA`,
 * `Intent.ACTION_CALL`, and many more).
 */
sealed class CBIntentAction(val rawValue: String) {
    data object AirplaneModeChanged : CBIntentAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
    data object BatteryChanged : CBIntentAction(Intent.ACTION_BATTERY_CHANGED)
    data object PackageAdded : CBIntentAction(Intent.ACTION_PACKAGE_ADDED)
    data object PackageRemoved : CBIntentAction(Intent.ACTION_PACKAGE_REMOVED)
    data object PackageReplaced : CBIntentAction(Intent.ACTION_PACKAGE_REPLACED)
    data object TimeTick : CBIntentAction(Intent.ACTION_TIME_TICK)
    data object LocaleChanged : CBIntentAction(Intent.ACTION_LOCALE_CHANGED)
    data object ScreenOn : CBIntentAction(Intent.ACTION_SCREEN_ON)
    data object ScreenOff : CBIntentAction(Intent.ACTION_SCREEN_OFF)
    data object HeadsetPlug : CBIntentAction(Intent.ACTION_HEADSET_PLUG)
    data object InputMethodChanged : CBIntentAction(Intent.ACTION_INPUT_METHOD_CHANGED)
    data object PowerSaveModeChanged : CBIntentAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    data object LocationModeChanged : CBIntentAction(LocationManager.MODE_CHANGED_ACTION)
    data object BluetoothStateChanged : CBIntentAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    data object NfcStateChanged : CBIntentAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
    data object DeviceIdleModeChanged : CBIntentAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
    data class Custom(val value: String) : CBIntentAction(value)
}
