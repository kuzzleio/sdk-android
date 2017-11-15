package io.kuzzle.sdk.util


class Scroll {
    var scrollId: String? = null

    fun hasScrollId(): Boolean {
        return scrollId != null
    }
}
