package io.kuzzle.sdk.util

import org.json.JSONObject

interface QueueFilter {
    fun filter(`object`: JSONObject): Boolean
}
