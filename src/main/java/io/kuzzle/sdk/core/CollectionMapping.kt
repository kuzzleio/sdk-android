package io.kuzzle.sdk.core

import org.json.JSONException
import org.json.JSONObject

import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener

class CollectionMapping
/**
 * Constructor
 *
 * @param kuzzleDataCollection - Parent data collection
 * @param mapping - Mapping content
 */
@JvmOverloads constructor(private val dataCollection: Collection, mapping: JSONObject? = null) {

    private var headers: JSONObject? = null
    /**
     * Get this mapping raw content
     *
     * @return mapping raw content
     */
    var mapping: JSONObject? = null
        private set
    private val kuzzle: Kuzzle
    private val collection: String

    init {
        try {
            this.headers = JSONObject(dataCollection.getHeaders().toString())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        this.kuzzle = dataCollection.kuzzle
        this.collection = dataCollection.collection
        this.mapping = mapping ?: JSONObject()
    }

    /**
     * Copy constructor
     *
     * @param kuzzleDataMapping - The CollectionMapping object to copy
     */
    constructor(kuzzleDataMapping: CollectionMapping) : this(kuzzleDataMapping.dataCollection, kuzzleDataMapping.mapping) {}

    /**
     * [.apply]
     */
    fun apply(): CollectionMapping {
        return this.apply(null, null)
    }

    /**
     * [.apply]
     */
    fun apply(options: Options): CollectionMapping {
        return this.apply(options, null)
    }

    /**
     * [.apply]
     */
    fun apply(listener: ResponseListener<CollectionMapping>): CollectionMapping {
        return this.apply(null, listener)
    }

    /**
     * Applies this mapping content to the data collection.
     *
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    fun apply(options: Options?, listener: ResponseListener<CollectionMapping>?): CollectionMapping {
        val data = JSONObject()
        val properties = JSONObject()
        try {
            properties.put("properties", this.mapping)
            data.put("body", properties)
            this.kuzzle.addHeaders(data, this.headers)
            this.kuzzle.query(this.dataCollection.makeQueryArgs("collection", "updateMapping"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener?.onSuccess(this@CollectionMapping)
                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.refresh]
     */
    fun refresh(listener: ResponseListener<CollectionMapping>) {
        refresh(null, listener)
    }

    /**
     * Gets a refreshed copy of the current object
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun refresh(options: Options?, listener: ResponseListener<CollectionMapping>) {
        if (listener == null) {
            throw IllegalArgumentException("CollectionMapping.refresh: listener callback missing")
        }

        val data = JSONObject()
        try {
            this.kuzzle.addHeaders(data, this.headers)
            this.kuzzle.query(this.dataCollection.makeQueryArgs("collection", "getMapping"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(args: JSONObject) {
                    try {
                        val newMapping = CollectionMapping(this@CollectionMapping.dataCollection)
                        val mappings = args.getJSONObject(this@CollectionMapping.dataCollection.index).getJSONObject("mappings")
                        newMapping.mapping = mappings.getJSONObject(this@CollectionMapping.collection)

                        listener.onSuccess(newMapping)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(`object`: JSONObject) {
                    listener.onError(`object`)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Remove a field from this mapping
     * Changes made by this function won't be applied until you call the apply method
     *
     * @param field - Field name to remove
     * @return this
     */
    fun remove(field: String): CollectionMapping {
        if (this.mapping!!.has(field)) {
            this@CollectionMapping.mapping!!.remove(field)
        }

        return this
    }

    /**
     * Attach a mapping to a field
     *
     * @param field - Field name
     * @param mapping - Field mapping
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    operator fun set(field: String, mapping: JSONObject): CollectionMapping {
        this.mapping!!.put(field, mapping)
        return this
    }

    /**
     * Get the global headers for this object
     *
     * @return global headers for this object
     */
    fun getHeaders(): JSONObject? {
        return headers
    }

    /**
     * [.setHeaders]
     */
    fun setHeaders(content: JSONObject): CollectionMapping {
        return this.setHeaders(content, false)
    }

    /**
     * Helper function allowing to set headers while chaining calls.
     * If the replace argument is set to true, replace the current headers with the provided content.
     * Otherwise, it appends the content to the current headers, only replacing already existing values
     *
     * @param content - Headers to append or replace
     * @param replace - false (default): append the content, true: replace the current headers
     * @return this
     */
    fun setHeaders(content: JSONObject?, replace: Boolean): CollectionMapping {
        try {
            if (content == null) {
                if (replace) {
                    this.headers = JSONObject()
                }

                return this
            }

            if (replace) {
                this.headers = JSONObject(content.toString())
            } else {
                val ite = content.keys()
                while (ite.hasNext()) {
                    val key = ite.next() as String
                    this.headers!!.put(key, content.get(key))
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }
}
/**
 * Constructor
 *
 * @param kuzzleDataCollection - Parent data collection
 */
