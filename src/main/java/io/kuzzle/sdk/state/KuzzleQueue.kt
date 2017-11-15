package io.kuzzle.sdk.state

import java.util.ArrayDeque
import java.util.Queue

/**
 * The type KuzzleQueue.
 *
 * @param <T> the type parameter
</T> */
class KuzzleQueue<T> : Iterable<T> {

    private val _queue = ArrayDeque<T>()

    private var _currentState = States.DISCONNECTED

    /**
     * @return queue content
     */
    val queue: Queue<*>
        get() = _queue

    /**
     * Add to queue.
     *
     * @param object Item to queue
     */
    @Synchronized
    fun addToQueue(`object`: T) {
        _queue.add(`object`)
    }

    /**
     * Dequeue t.
     *
     * @return Dequeued item
     */
    @Synchronized
    fun dequeue(): T {
        return _queue.poll()
    }

    /**
     * @param states New connection state value
     */
    fun setState(states: States) {
        _currentState = states
    }

    /**
     * @return Connection state value
     */
    fun state(): States {
        return _currentState
    }

    override fun iterator(): Iterator<T> {
        return _queue.iterator()
    }
}
