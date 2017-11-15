package io.kuzzle.sdk.util

import java.util.HashMap
import io.kuzzle.sdk.listeners.EventListener

class EventList : HashMap<EventListener, Event>() {
    var lastEmitted: Long = 0
}
