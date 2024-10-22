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

package compose.broadcasts.sample

import android.content.Context
import android.content.Intent
import android.util.Log
import compose.broadcasts.CBBroadcastReceiver
import java.util.Locale

class LocaleReceiver : CBBroadcastReceiver(tag = "current_locale_receiver") {
    override fun onReceive(context: Context?, intent: Intent?) {
        // It is important to call `super.onReceive` for the library to do its job. Ideally, you
        // should write all your custom logic inside the composable's `mapToState` lambda.
        super.onReceive(context, intent)
        if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
            Log.d("SAMPLE_APP", "New locale: ${Locale.getDefault().toLanguageTag()}")
        }
    }
}
