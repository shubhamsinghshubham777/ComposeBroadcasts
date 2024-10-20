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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

open class CBBroadcastReceiver(internal val tag: String) : BroadcastReceiver(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    override fun onReceive(context: Context?, intent: Intent?) {
        launch {
            CBDataContainer.flows[tag]?.let { flow ->
                flow.emit(intent)
                Log.d(LOG_TAG, "Emitting to flow with tag ($tag): $intent")
            }
        }
    }
}
