package compose.broadcasts

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class CBBroadcastReceiverTest {
    @After
    fun tearDown() {
        CBDataContainer.dispose()
    }

    @Test
    fun `onReceive emits the intent to the flow associated with its tag`() = runBlocking {
        val tag = "package-events"
        val receiver = CBBroadcastReceiver(tag)
        val intent =
            Intent(CBIntentAction.PackageAdded.rawValue).setData(Uri.parse("package:example.app"))
        CBDataContainer.addFlow(tag)

        receiver.onReceive(null, intent)

        assertEquals(
            intent,
            withTimeout(1_000.milliseconds) {
                CBDataContainer.flows.getValue(tag).filterNotNull().first()
            },
        )
    }

    @Test
    fun `onReceive without a registered flow is a no-op`() = runBlocking {
        CBBroadcastReceiver("unregistered").onReceive(
            null,
            Intent(CBIntentAction.LocaleChanged.rawValue)
        )

        withTimeout(1_000.milliseconds) { kotlinx.coroutines.delay(25.milliseconds) }
        assertNull(CBDataContainer.flows["unregistered"])
    }
}
