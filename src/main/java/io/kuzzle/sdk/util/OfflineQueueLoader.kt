package io.kuzzle.sdk.util

import io.kuzzle.sdk.state.KuzzleQueue

interface OfflineQueueLoader {
    fun load(): KuzzleQueue<QueryObject>
}
