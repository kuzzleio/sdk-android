package io.kuzzle.sdk.enums

/**
 * The enum Event type.
 */
enum class Event {
    disconnected,
    reconnected,
    connected,
    error,
    tokenExpired,
    loginAttempt,
    offlineQueuePush,
    offlineQueuePop
}
