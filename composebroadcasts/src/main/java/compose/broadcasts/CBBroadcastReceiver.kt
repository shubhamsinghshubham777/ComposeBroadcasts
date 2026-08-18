/*
 * Copyright (c) 2024 Shubham Singh
 *
 * This library is licensed under the Apache 2.0 License.
 */

package compose.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A broadcast receiver that can be observed by Compose Broadcasts. Each registration gets its own
 * subscription, so the same receiver (and tag) may safely be used by multiple call sites.
 *
 * Put application logic in the composable mapper rather than overriding [onReceive].
 */
open class CBBroadcastReceiver(internal val tag: String) : BroadcastReceiver() {
    internal var isLibraryProvidedTag: Boolean = false

    internal constructor(tag: String, isLibraryProvidedTag: Boolean) : this(tag) {
        this.isLibraryProvidedTag = isLibraryProvidedTag
    }

    private val subscriptions = CopyOnWriteArraySet<Subscription>()

    internal fun subscribe(): Subscription {
        return Subscription().also(subscriptions::add)
    }

    internal fun registrationReceiver(subscription: Subscription): BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                subscription.offer(intent)
            }
        }

    override fun onReceive(context: Context?, intent: Intent?) {
        // Channel.trySend is synchronous and FIFO. This avoids reordering broadcasts by launching
        // an unstructured coroutine for every callback.
        subscriptions.forEach { it.offer(intent) }
        Log.d(LOG_TAG, "Delivered broadcast for receiver tag ($tag): $intent")
    }

    internal inner class Subscription : AutoCloseable {
        private val channel = Channel<Intent>(Channel.UNLIMITED)
        private var closed = false

        val events: Flow<Intent> = channel.receiveAsFlow()

        fun offer(intent: Intent?) {
            if (intent != null && !closed) channel.trySend(intent)
        }

        override fun close() {
            if (!closed) {
                closed = true
                subscriptions.remove(this)
                channel.close()
            }
        }
    }
}
