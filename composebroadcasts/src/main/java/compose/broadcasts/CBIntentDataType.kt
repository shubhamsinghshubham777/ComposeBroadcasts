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

/**
 * An object of CBIntentDataType is added to a [CBIntentFilter] object along with an object of
 * [CBIntentDataScheme] to further specify precisely what kind of data the user needs filtered.
 */
sealed class CBIntentDataType(internal val rawValue: String) {
    data class Custom(val value: String) : CBIntentDataType(value)
}
