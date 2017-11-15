package io.kuzzle.sdk.listeners

import org.json.JSONObject

interface OnConnectionEvent {
    fun onSuccess(success: JSONObject)
    fun onError(error: JSONObject)
}
