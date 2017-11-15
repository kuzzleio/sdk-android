package io.kuzzle.sdk.util

import org.json.JSONException
import org.json.JSONObject

class KuzzleJSONObject : JSONObject() {

    override fun put(name: String, value: Any): KuzzleJSONObject {
        try {
            super.put(name, value)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    override fun put(name: String, value: Long): KuzzleJSONObject {
        try {
            super.put(name, value)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    override fun put(name: String, value: Int): KuzzleJSONObject {
        try {
            super.put(name, value)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    override fun put(name: String, value: Double): KuzzleJSONObject {
        try {
            super.put(name, value)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    override fun put(name: String, value: Boolean): KuzzleJSONObject {
        try {
            super.put(name, value)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

}
