package io.kuzzle.sdk.security

import org.json.JSONException
import org.json.JSONObject

import io.kuzzle.sdk.core.Kuzzle
import io.kuzzle.sdk.core.Options
import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener

/**
 * This class handles roles management in Kuzzle
 */
class Role
/**
 * Instantiates a new Kuzzle role.
 *
 * @param kuzzle  Kuzzle instance to attach
 * @param id      Role unique identifier
 * @param content Role content
 * @param meta Role metadata
 * @throws JSONException
 */
@Throws(JSONException::class)
constructor(kuzzle: Kuzzle, id: String, content: JSONObject, meta: JSONObject) : AbstractSecurityDocument(kuzzle, id, content, meta) {
    init {
        this.deleteActionName = "deleteRole"
        this.updateActionName = "updateRole"
    }

    /**
     * Save this role in Kuzzle
     *
     * @param options  Request optional configuration
     * @param listener Optional callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun save(options: Options?, listener: ResponseListener<Role>?): Role {
        val data = this.serialize()

        if (listener != null) {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceRole"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener.onSuccess(this@Role)
                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceRole"), data, options!!)
        }

        return this
    }

    /**
     * [.save]
     */
    @Throws(JSONException::class)
    fun save(listener: ResponseListener<Role>): Role {
        return this.save(null, listener)
    }

    /**
     * [.save]
     */
    @Throws(JSONException::class)
    fun save(options: Options): Role {
        return this.save(options, null)
    }

    /**
     * [.save]
     */
    @Throws(JSONException::class)
    fun save(): Role {
        return this.save(null, null)
    }
}
