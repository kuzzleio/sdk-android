package io.kuzzle.sdk.responses

interface KuzzleList<T> {
    val documents: List<T>
    val total: Long
}
