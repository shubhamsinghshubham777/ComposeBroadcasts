package compose.broadcasts

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class CBBroadcastReceiverTest {
    @Test
    fun `onReceive emits the intent to the flow associated with its tag`() = runBlocking {
        val tag = "package-events"
        val receiver = CBBroadcastReceiver(tag)
        val intent =
            Intent(CBIntentAction.PackageAdded.rawValue).setData(Uri.parse("package:example.app"))
        val subscription = receiver.subscribe()

        receiver.onReceive(null, intent)

        assertEquals(intent, withTimeout(1_000.milliseconds) { subscription.events.first() })
        subscription.close()
    }

    @Test
    fun `subscriptions are independent and preserve broadcast order`() = runBlocking {
        val receiver = CBBroadcastReceiver("shared")
        val first = receiver.subscribe()
        val second = receiver.subscribe()
        val intents = listOf(
            Intent("example.FIRST"),
            Intent("example.SECOND"),
        )

        intents.forEach { receiver.onReceive(null, it) }

        assertEquals(intents, listOf(first.events.first(), first.events.first()))
        second.close()
        first.close()
    }

    @Test
    fun `onReceive without a registered flow is a no-op`() = runBlocking {
        CBBroadcastReceiver("unregistered").onReceive(null, Intent(CBIntentAction.LocaleChanged.rawValue))
        withTimeout(1_000.milliseconds) { kotlinx.coroutines.delay(25.milliseconds) }
    }
}
