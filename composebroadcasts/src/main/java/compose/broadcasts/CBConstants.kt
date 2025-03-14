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

internal enum class CBConstants(val value: String) {
    AIRPLANE_MODE("airplane_mode"),
    BATTERY_LEVEL("battery_level"),
    IS_CHARGING("is_charging"),
    CURRENT_TIME_MILLIS("current_time_millis"),
    IS_SCREEN_ON("is_screen_on"),
    HEADSET_INFO("headset_info"),
    INPUT_METHOD("input_method"),
}
