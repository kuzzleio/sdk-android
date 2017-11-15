package io.kuzzle.sdk.listeners

import org.json.JSONObject

/**
 * The interface Response listener.
 */
interface ResponseListener<T> {
    /**
     * On success.
     *
     * @param response Raw Kuzzle API response
     */
    fun onSuccess(response: T)

    /**
     * On error.
     *
     * @param error Raw Kuzzle API error content
     */
    fun onError(error: JSONObject)
}
