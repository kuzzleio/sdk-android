package io.kuzzle.sdk.util

import io.kuzzle.sdk.listeners.EventListener

abstract class Event
/**
 * Instantiates a new Event.
 *
 * @param type Event type
 */
(
        /**
         * @return Event type value
         */
        val type: io.kuzzle.sdk.enums.Event) : EventListener {

    abstract override fun trigger(vararg args: Any)
}
