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
    data class Custom(val value: String) : CBIntentAction(value)
}
