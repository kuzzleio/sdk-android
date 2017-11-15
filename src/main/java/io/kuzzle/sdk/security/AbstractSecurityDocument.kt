package io.kuzzle.sdk.security

import org.json.JSONException
import org.json.JSONObject

import io.kuzzle.sdk.core.Kuzzle
import io.kuzzle.sdk.core.Options
import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener

/**
 * Base class for the Role, Profile and User classes
 */
open class AbstractSecurityDocument
/**
 * Instantiates a new Abstract kuzzle security document.
 *
 * @param kuzzle  Kuzzle instance to link
 * @param id      Security document unique ID
 * @param content Security document content
 * @throws JSONException
 */
@Throws(JSONException::class)
constructor(protected val kuzzle: Kuzzle,
            /**
             * Getter for the "id" property
             *
             * @return the document id
             */
            val id: String, content: JSONObject?, meta: JSONObject?) {
    protected val kuzzleSecurity: Security
    protected var deleteActionName: String? = null
    protected var updateActionName: String? = null
    var content: JSONObject
    var meta: JSONObject

    init {
        if (id == null) {
            throw IllegalArgumentException("Cannot initialize with a null ID")
        }
        this.kuzzleSecurity = kuzzle.security

        if (content != null) {
            setContent(content)
        } else {
            this.content = JSONObject()
        }

        if (meta != null) {
            setMeta(meta)
        } else {
            this.meta = JSONObject()
        }
    }

    /**
     * Sets the content of this object
     *
     * @param content New content
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun setContent(content: JSONObject): AbstractSecurityDocument {
        if (content == null) {
            throw IllegalArgumentException("AbstractSecurityDocument.setContent: cannot set null content")
        }

        this.content = JSONObject(content.toString())

        return this
    }

    /**
     * Sets the metadata of this object
     *
     * @param meta New metadata
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun setMeta(meta: JSONObject): AbstractSecurityDocument {
        if (meta == null) {
            throw IllegalArgumentException("AbstractSecurityDocument.setMeta: cannot set null metadata")
        }

        this.meta = JSONObject(meta.toString())

        return this
    }

    /**
     * Serializes this object to a plain-old JSON object
     *
     * @return The serialized version of this object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    open fun serialize(): JSONObject {
        val data: JSONObject

        data = JSONObject()
                .put("_id", this.id)
                .put("body", content)

        return data
    }

    /**
     * Delete this role/profile/user from Kuzzle
     *
     * @param options  Optional configuration
     * @param listener Optional response callback
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun delete(options: Options?, listener: ResponseListener<String>?) {
        val data = JSONObject().put("_id", this.id)

        if (listener != null) {
            this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.deleteActionName!!), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getString("_id"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.deleteActionName!!), data, options!!)
        }
    }

    /**
     * [.delete]
     */
    @Throws(JSONException::class)
    fun delete(listener: ResponseListener<String>) {
        this.delete(null, listener)
    }

    /**
     * [.delete]
     */
    @Throws(JSONException::class)
    fun delete(options: Options) {
        this.delete(options, null)
    }

    /**
     * [.delete]
     */
    @Throws(JSONException::class)
    fun delete() {
        this.delete(null, null)
    }

    /**
     * Getter for the "content" property
     *
     * @return the document content
     */
    fun getContent(): JSONObject {
        return this.content
    }

    /**
     * Getter for the "meta" property
     *
     * @return the document metadata
     */
    fun getMeta(): JSONObject {
        return this.meta
    }

    /**
     * [.update]
     */
    @Throws(JSONException::class)
    fun update(content: JSONObject): AbstractSecurityDocument {
        return this.update(content, null, null)
    }

    /**
     * [.update]
     */
    @Throws(JSONException::class)
    fun update(content: JSONObject, options: Options): AbstractSecurityDocument {
        return this.update(content, options, null)
    }

    /**
     * [.update]
     */
    @Throws(JSONException::class)
    fun update(content: JSONObject, listener: ResponseListener<AbstractSecurityDocument>): AbstractSecurityDocument {
        return this.update(content, null, listener)
    }

    /**
     * Perform a partial update on this object
     *
     * @param content Content used to update the object
     * @param options Request optional parameters
     * @param listener Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun update(content: JSONObject, options: Options?, listener: ResponseListener<AbstractSecurityDocument>?): AbstractSecurityDocument {
        val data = JSONObject()
                .put("_id", this.id)
                .put("body", content)

        if (listener != null) {
            this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.updateActionName!!), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val updatedContent = response.getJSONObject("result").getJSONObject("_source")
                        this@AbstractSecurityDocument.setContent(updatedContent)
                        listener.onSuccess(this@AbstractSecurityDocument)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.updateActionName!!), data, options!!)
        }

        return this
    }
}
