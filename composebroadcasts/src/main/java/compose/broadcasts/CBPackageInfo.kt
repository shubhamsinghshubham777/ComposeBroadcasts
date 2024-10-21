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
 * An object of CBPackageInfo depicts some basic details about a package interaction (like
 * installing, uninstalling, and updating an app) with the help of package-related
 * [CBIntentAction]s.
 */
data class CBPackageInfo(
    val packageName: String,
    val action: CBPackageAction,
)

/**
 * A CBPackageAction depicts the type of event that happened to the last interacted-with app by the
 * operating system (i.e. either a new app was installed, a previously installed app was removed,
 * an old app was replaced with its newer version, and UNKNOWN for all other unexpected cases).
 */
enum class CBPackageAction { Added, Removed, Replaced, Unknown }
