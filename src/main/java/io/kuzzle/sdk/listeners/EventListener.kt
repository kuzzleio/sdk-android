package io.kuzzle.sdk.listeners

/**
 * The interface Event listener.
 */
interface EventListener {

    /**
     * Trigger.
     *
     * @param args the args
     */
    fun trigger(vararg args: Any)

}
