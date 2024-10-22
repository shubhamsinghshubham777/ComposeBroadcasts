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

/**
 * A drop-in replacement for Android's [BroadcastReceiver]s that allows users to easily create
 * custom broadcast receivers with the Compose Broadcasts set of APIs.
 *
 * > **Note**: In case you're creating an instance of this class, it is important to know that
 * calling `super.onReceive` is important for the library to be able to do its job. Ideally, you
 * should write all your custom logic inside the composable's `mapToState` lambda and not override
 * `onReceive` of this class at all.
 *
 * @param tag The class accepts a unique string value that it uses to identify different instances
 * of [CBBroadcastReceiver]s while trying to fetch its configuration-persisted data. Please make
 * sure to always use a unique `tag` for all your custom CBBroadcastReceiver instances to avoid
 * seeing any unforeseen state/value mismatches.
 *
 * @throws IllegalStateException if a pre-existing tag is used. Use [String.isComposeBroadcastsTag]
 * util extension to check if your tag is already used by the library.
 */
open class CBBroadcastReceiver(internal val tag: String) : BroadcastReceiver(), CoroutineScope {
    internal var isLibraryProvidedTag: Boolean = false

    internal constructor(tag: String, isLibraryProvidedTag: Boolean) : this(tag) {
        this.isLibraryProvidedTag = isLibraryProvidedTag
    }

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
