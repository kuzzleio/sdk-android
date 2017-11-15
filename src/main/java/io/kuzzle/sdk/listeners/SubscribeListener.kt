package io.kuzzle.sdk.listeners

import org.json.JSONObject

import java.util.ArrayList

import io.kuzzle.sdk.core.Room

class SubscribeListener {
    var error: JSONObject? = null
        private set
    var room: Room? = null
        private set
    private val cbs = ArrayList<ResponseListener<Room>>()

    fun onDone(cb: ResponseListener<Room>) {
        if (this.error != null) {
            cb.onError(this.error)
        } else if (this.room != null) {
            cb.onSuccess(this.room)
        } else {
            this.cbs.add(cb)
        }
    }

    fun done(error: JSONObject, room: Room): SubscribeListener {
        this.error = error
        this.room = room

        for (cb in cbs) {
            if (this.error != null) {
                cb.onError(this.error)
            } else if (this.room != null) {
                cb.onSuccess(this.room)
            }
        }
        return this
    }
}
