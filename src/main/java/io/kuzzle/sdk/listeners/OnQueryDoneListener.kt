package io.kuzzle.sdk.listeners

import org.json.JSONObject

interface OnQueryDoneListener {
    fun onSuccess(response: JSONObject)
    fun onError(error: JSONObject)
}
