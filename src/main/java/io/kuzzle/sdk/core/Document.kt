package io.kuzzle.sdk.core

import org.json.JSONException
import org.json.JSONObject

import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.SubscribeListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener
import io.kuzzle.sdk.responses.NotificationResponse

class Document
/**
 * Kuzzle handles documents either as real-time messages or as stored documents.
 * Document is the object representation of one of these documents.
 *
 * @param kuzzleDataCollection - An instantiated Collection object
 * @param id                   - Unique document identifier
 * @param content              - The content of the document
 * @param meta                 - Document metadata
 * @throws JSONException
 */
@Throws(JSONException::class)
@JvmOverloads constructor(private val dataCollection: Collection, id: String? = null, content: JSONObject? = null, meta: JSONObject? = null) {
    /**
     * Get the parent data collection name
     *
     * @return parent data collection name
     */
    val collection: String
    private val kuzzle: Kuzzle
    private var headers: JSONObject? = null

    private var id: String? = null
    private var content: JSONObject? = null
    private var meta: JSONObject? = null
    private var version: Long = -1

    init {
        if (dataCollection == null) {
            throw IllegalArgumentException("Document: Collection argument missing")
        }
        this.collection = dataCollection.collection
        this.kuzzle = dataCollection.kuzzle
        this.setId(id)
        this.setContent(content, true)
        this.setMeta(meta, true)
        this.headers = dataCollection.getHeaders()
    }

    /**
     * Kuzzle handles documents either as real-time messages or as stored documents.
     * Document is the object representation of one of these documents.
     *
     * @param kuzzleDataCollection - An instantiated Collection object
     * @param content              - The content of the document
     * @throws JSONException
     */
    @Throws(JSONException::class)
    constructor(kuzzleDataCollection: Collection, content: JSONObject) : this(kuzzleDataCollection, null, content, null) {
    }

    /**
     * Kuzzle handles documents either as real-time messages or as stored documents.
     * Document is the object representation of one of these documents.
     *
     * @param kuzzleDataCollection - An instantiated Collection object
     * @param content              - The content of the document
     * @param meta                 - Document metadata
     * @throws JSONException
     */
    @Throws(JSONException::class)
    constructor(kuzzleDataCollection: Collection, content: JSONObject, meta: JSONObject) : this(kuzzleDataCollection, null, content, meta) {
    }

    /**
     * [.delete]
     */
    fun delete(options: Options) {
        this.delete(options, null)
    }

    /**
     * [.delete]
     */
    fun delete(listener: ResponseListener<String>) {
        this.delete(null, listener)
    }

    /**
     * [.delete]
     */
    fun delete() {
        this.delete(null, null)
    }

    /**
     * Delete this document from Kuzzle
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun delete(options: Options?, listener: ResponseListener<String>?) {
        try {
            if (this.id == null) {
                throw IllegalStateException("Document.delete: cannot delete a document without a document ID")
            }

            this.kuzzle.query(this.dataCollection.makeQueryArgs("document", "delete"), this.serialize(), options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    setId(null)
                    if (listener != null) {
                        try {
                            listener.onSuccess(`object`.getString("result"))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                    }
                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * [.exists]
     */
    fun exists(listener: ResponseListener<Boolean>) {
        this.exists(null, listener)
    }

    /**
     * Ask Kuzzle if this document exists
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun exists(options: Options?, listener: ResponseListener<Boolean>) {
        if (this.id == null) {
            throw IllegalStateException("Document.exists: cannot check if the document exists if no id has been provided")
        }

        if (listener == null) {
            throw IllegalArgumentException("Document.exists: a valid ResponseListener object is required")
        }

        try {
            this.kuzzle.query(this.dataCollection.makeQueryArgs("document", "exists"), this.serialize(), options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    try {
                        listener.onSuccess(`object`.getBoolean("result"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * [.refresh]
     */
    fun refresh(listener: ResponseListener<Document>) {
        this.refresh(null, listener)
    }

    /**
     * Gets a refreshed copy of this document from Kuzzle
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun refresh(options: Options?, listener: ResponseListener<Document>) {
        if (this.id == null) {
            throw IllegalStateException("Document.refresh: cannot retrieve a document if no id has been provided")
        }

        if (listener == null) {
            throw IllegalArgumentException("Document.refresh: a valid ResponseListener object is required")
        }

        try {
            val content = JSONObject()
            content.put("_id", this.getId())

            this.kuzzle.query(this.dataCollection.makeQueryArgs("document", "get"), content, options, object : OnQueryDoneListener {
                override fun onSuccess(args: JSONObject) {
                    try {
                        val result = args.getJSONObject("result")
                        val newDocument = Document(
                                this@Document.dataCollection,
                                result.getString("_id"),
                                result.getJSONObject("_source"),
                                result.getJSONObject("_meta")
                        )

                        newDocument.setVersion(result.getLong("_version"))
                        listener.onSuccess(newDocument)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(arg: JSONObject) {
                    listener.onError(arg)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * [.save]
     */
    fun save(options: Options): Document {
        return this.save(options, null)
    }

    /**
     * [.save]
     */
    fun save(listener: ResponseListener<Document>): Document {
        return save(null, listener)
    }

    /**
     * Saves this document into Kuzzle.
     * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
     * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
     *
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    @JvmOverloads
    fun save(options: Options? = null, listener: ResponseListener<Document>? = null): Document {
        try {
            kuzzle.query(this.dataCollection.makeQueryArgs("document", "createOrReplace"), this.serialize(), options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        this@Document.setId(result.getString("_id"))
                        this@Document.setVersion(result.getLong("_version"))

                        listener?.onSuccess(this@Document)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

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
     * [.publish]
     */
    fun publish(): Document {
        return this.publish(null)
    }

    /**
     * Sends the content of this document as a real-time message.
     *
     * @param options - Request options
     * @return this
     */
    fun publish(options: Options?): Document {
        try {
            kuzzle.query(this.dataCollection.makeQueryArgs("realtime", "publish"), this.serialize(), options, null)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.setContent]
     */
    @Throws(JSONException::class)
    fun setContent(content: JSONObject): Document {
        this.setContent(content, false)
        return this
    }

    /**
     * Sets this document content
     *
     * @param content - New content for this document
     * @param replace - true: replace the current content, false (default): update/append it
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun setContent(content: JSONObject?, replace: Boolean): Document {
        if (replace) {
            if (content != null) {
                this.content = JSONObject(content.toString())
            } else {
                this.content = JSONObject()
            }
        } else if (content != null) {
            val iterator = content.keys()
            while (iterator.hasNext()) {
                val key = iterator.next() as String
                this.content!!.put(key, content.get(key))
            }
        }

        if (this.content!!.has("version")) {
            this.version = this.content!!.getLong("version")
            this.content!!.remove("version")
        }

        return this
    }

    /**
     * Set a document field value
     *
     * @param key - Field name to set
     * @param value - New field value
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun setContent(key: String, value: Any): Document {
        if (key == null) {
            throw IllegalArgumentException("Document.setContent: key required")
        }

        this.content!!.put(key, value)
        return this
    }

    /**
     * [.setMeta]
     */
    @Throws(JSONException::class)
    fun setMeta(meta: JSONObject): Document {
        this.setMeta(meta, false)

        return this
    }

    /**
     * Set document metadata
     *
     * @param meta - Metadata content
     * @param replace - true: replace, false (default): append/update otherwise
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun setMeta(meta: JSONObject?, replace: Boolean): Document {
        if (replace) {
            if (meta != null) {
                this.meta = JSONObject(meta.toString())
            } else {
                this.meta = JSONObject()
            }
        } else if (meta != null) {
            val iterator = meta.keys()
            while (iterator.hasNext()) {
                val key = iterator.next() as String
                this.meta!!.put(key, meta.get(key))
            }
        }

        return this
    }

    /**
     * Set a metadata field value
     *
     * @param key - Metadata field name
     * @param value - Metadata field value
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun setMeta(key: String, value: Any): Document {
        if (key == null) {
            throw IllegalArgumentException("Document.setMeta: key required")
        }

        this.meta!!.put(key, value)

        return this
    }

    /**
     * [.subscribe]
     */
    fun subscribe(listener: ResponseListener<NotificationResponse>): SubscribeListener {
        return this.subscribe(null, listener)
    }

    /**
     * Subscribe to changes occuring on this document.
     * Throws an error if this document has not yet been created in Kuzzle.
     *
     * @param options - Room object constructor options
     * @param listener - Response callback listener
     * @return an object with a "onDone" callback triggered when the subscription is active
     */
    fun subscribe(options: RoomOptions?, listener: ResponseListener<NotificationResponse>): SubscribeListener {
        if (this.id == null) {
            throw IllegalStateException("Document.subscribe: cannot subscribe to a document if no ID has been provided")
        }

        val returnValue: SubscribeListener

        try {
            val filters = JSONObject("{" +
                    "\"ids\": {" +
                    "\"values\": [\"" + this.id + "\"]" +
                    "}" +
                    "}")
            returnValue = this.dataCollection.subscribe(filters, options, listener)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return returnValue
    }

    /**
     * Document content getter
     *
     * @return current document content
     */
    fun getContent(): JSONObject? {
        return this.content
    }


    /**
     * Get document content field
     *
     * @param key - Field name to get
     * @return field value
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun getContent(key: String): Any? {
        return if (this.content!!.has(key)) {
            this.content!!.get(key)
        } else null

    }

    /**
     * Document metadata getter
     *
     * @return this document metadata
     */
    fun getMeta(): JSONObject? {
        return this.meta
    }


    /**
     * Get a metadata field value
     *
     * @param key - Metadata field name to get
     * @return metadata field value
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun getMeta(key: String): Any? {
        return if (this.meta!!.has(key)) {
            this.meta!!.get(key)
        } else null

    }

    /**
     * [.setHeaders]
     */
    fun setHeaders(content: JSONObject): Document {
        return this.setHeaders(content, false)
    }

    /**
     * Replace or append/update global headers for this object
     *
     * @param content - new headers content
     * @param replace - true: replace the current headers, false (default): append/update
     * @return this
     */
    fun setHeaders(content: JSONObject?, replace: Boolean): Document {
        try {
            if (content == null) {
                if (replace) {
                    this.content = JSONObject()
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

    /**
     * Get the global headers for this object
     *
     * @return global headers for this object
     */
    fun getHeaders(): JSONObject? {
        return this.headers
    }

    /**
     * Unique identifier getter
     *
     * @return this document unique identifier
     */
    fun getId(): String? {
        return this.id
    }

    /**
     * Set this document unique identifier
     *
     * @param id - New document unique identifier
     * @return this
     */
    fun setId(id: String?): Document {
        this.id = id
        return this
    }

    /**
     * Get this document version
     *
     * @return this document version
     */
    fun getVersion(): Long {
        return this.version
    }

    /**
     * Serializes this document into a plain old JSON object
     *
     * @return JSON object representing this document
     */
    fun serialize(): JSONObject {
        val data = JSONObject()

        try {
            if (this.id != null) {
                data.put("_id", this.getId())
            }

            if (this.version != -1) {
                data.put("_version", this.version)
            }

            data.put("body", this.getContent())
            this.kuzzle.addHeaders(data, getHeaders())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return data
    }

    override fun toString(): String {
        return this.serialize().toString()
    }

    /**
     * Set this document version
     *
     * @param version - New document version
     * @return  this
     */
    fun setVersion(version: Long): Document {
        if (version > 0) {
            this.version = version
        }

        return this
    }
}
/**
 * Kuzzle handles documents either as real-time messages or as stored documents.
 * Document is the object representation of one of these documents.
 *
 * @param kuzzleDataCollection - An instantiated Collection object
 * @param id                   - Unique document identifier
 * @param content              - The content of the document
 * @throws JSONException
 */
/**
 * Kuzzle handles documents either as real-time messages or as stored documents.
 * Document is the object representation of one of these documents.
 *
 * @param kuzzleDataCollection - An instantiated Collection object
 * @throws JSONException
 */
/**
 * Kuzzle handles documents either as real-time messages or as stored documents.
 * Document is the object representation of one of these documents.
 *
 * @param kuzzleDataCollection - An instantiated Collection object
 * @param id                   - Unique document identifier
 * @throws JSONException
 */
/**
 * [.save]
 */
