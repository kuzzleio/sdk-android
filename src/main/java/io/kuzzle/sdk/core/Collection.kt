package io.kuzzle.sdk.core

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.Arrays

import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.SubscribeListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener
import io.kuzzle.sdk.responses.SearchResult
import io.kuzzle.sdk.responses.NotificationResponse

open class Collection
/**
 * Constructor
 *
 * @param kuzzle  Kuzzle instance
 * @param collection  Data collection name
 * @param index  Parent data index name
 */
(
        /**
         * Get the attached Kuzzle object instance.
         *
         * @return attached Kuzzle object instance
         */
        val kuzzle: Kuzzle,
        /**
         * Get this data collection name
         *
         * @return data collection name
         */
        val collection: String,
        /**
         * Get the parent data index name
         *
         * @return parent data index name
         */
        val index: String) {
    private val subscribeCallback: ResponseListener<Room>? = null
    private val subscribeError: JSONObject? = null
    private val subscribeRoom: Room? = null

    protected var headers: JSONObject

    init {
        if (kuzzle == null) {
            throw IllegalArgumentException("Collection: need a Kuzzle instance to initialize")
        }

        if (index == null || collection == null) {
            throw IllegalArgumentException("Collection: index and collection required")
        }

        try {
            this.headers = JSONObject(kuzzle.getHeaders().toString())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * [.search]
     */
    fun search(filter: JSONObject, listener: ResponseListener<SearchResult>) {
        this.search(filter, Options(), listener)
    }

    /**
     * Executes a search on the data collection.
     * /!\ There is a small delay between documents creation and their existence in our search layer,
     * usually a couple of seconds.
     * That means that a document that was just been created won’t be returned by this function.
     *
     * @param filters  Search filters to apply
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun search(filters: JSONObject?, options: Options, listener: ResponseListener<SearchResult>) {
        if (listener == null) {
            throw IllegalArgumentException("listener cannot be null")
        }
        this.kuzzle.isValid()
        val data = JSONObject()
        try {
            if (filters != null) {
                data.put("body", filters)
            }

            this.kuzzle.addHeaders(data, this.getHeaders())

            this.kuzzle.query(makeQueryArgs("document", "search"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    try {
                        val response: SearchResult
                        var aggregations: JSONObject? = null
                        val hits = `object`.getJSONObject("result").getJSONArray("hits")
                        val docs = ArrayList<Document>()

                        for (i in 0 until hits.length()) {
                            val hit = hits.getJSONObject(i)
                            val doc = Document(this@Collection, hit.getString("_id"), hit.getJSONObject("_source"), hit.getJSONObject("_meta"))

                            docs.add(doc)
                        }

                        if (`object`.getJSONObject("result").has("_scroll_id")) {
                            options.scrollId = `object`.getJSONObject("result").getString("_scroll_id")
                        }

                        if (`object`.getJSONObject("result").has("aggregations")) {
                            aggregations = `object`.getJSONObject("result").getJSONObject("aggregations")
                        }

                        response = SearchResult(
                                this@Collection,
                                `object`.getJSONObject("result").getInt("total").toLong(),
                                docs,
                                aggregations,
                                options,
                                filters!!,
                                options.previous
                        )

                        listener.onSuccess(response)
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
     * [.scroll]
     */
    fun scroll(scrollId: String, listener: ResponseListener<SearchResult>) {
        this.scroll(scrollId, Options(), JSONObject(), listener)
    }

    /**
     * [.scroll]
     */
    fun scroll(scrollId: String, options: Options, listener: ResponseListener<SearchResult>) {
        this.scroll(scrollId, options, JSONObject(), listener)
    }

    /**
     * Gets the next page of results from a previous search or scroll request
     *
     * @param scrollId  Scroll unique identifier
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun scroll(scrollId: String?, options: Options, filters: JSONObject, listener: ResponseListener<SearchResult>?) {
        val request: JSONObject

        try {
            request = JSONObject().put("body", JSONObject())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        if (listener == null) {
            throw IllegalArgumentException("listener cannot be null")
        }

        if (scrollId == null) {
            throw RuntimeException("Collection.scroll: scrollId is required")
        }

        options.scrollId = scrollId

        try {
            this.kuzzle.query(this.kuzzle.buildQueryArgs("document", "scroll"), request, options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    try {
                        val response: SearchResult
                        val hits = `object`.getJSONObject("result").getJSONArray("hits")
                        val docs = ArrayList<Document>()

                        for (i in 0 until hits.length()) {
                            val hit = hits.getJSONObject(i)
                            val doc = Document(this@Collection, hit.getString("_id"), hit.getJSONObject("_source"), hit.getJSONObject("_meta"))

                            docs.add(doc)
                        }

                        if (`object`.getJSONObject("result").has("_scroll_id")) {
                            options.scrollId = `object`.getJSONObject("result").getString("_scroll_id")
                        }

                        response = SearchResult(
                                this@Collection,
                                `object`.getJSONObject("result").getInt("total").toLong(),
                                docs,
                                JSONObject(),
                                options,
                                filters,
                                options.previous
                        )

                        listener.onSuccess(response)
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
     * [.scrollSpecifications]
     */
    fun scrollSpecifications(scrollId: String, listener: ResponseListener<JSONObject>) {
        this.scrollSpecifications(scrollId, Options(), listener)
    }

    /**
     * Scrolls through specifications using the provided scrollId
     *
     * @param scrollId  Scroll unique identifier
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun scrollSpecifications(scrollId: String, options: Options, listener: ResponseListener<JSONObject>) {
        this.kuzzle.isValid()

        val data = JSONObject()

        if (scrollId == null) {
            throw RuntimeException("Collection.scrollSpecifications: scrollId is required")
        }

        if (listener == null) {
            throw IllegalArgumentException("listener cannot be null")
        }

        try {
            data.put("scrollId", scrollId)

            this.kuzzle.addHeaders(data, this.getHeaders())

            this.kuzzle.query(makeQueryArgs("collection", "scrollSpecifications"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result"))
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
     * [.searchSpecifications]
     */
    fun searchSpecifications(listener: ResponseListener<JSONObject>) {
        this.searchSpecifications(null, Options(), listener)
    }

    /**
     * [.searchSpecifications]
     */
    fun searchSpecifications(filters: JSONObject, listener: ResponseListener<JSONObject>) {
        this.searchSpecifications(filters, Options(), listener)
    }

    /**
     * [.searchSpecifications]
     */
    fun searchSpecifications(options: Options, listener: ResponseListener<JSONObject>) {
        this.searchSpecifications(null, options, listener)
    }

    /**
     * Searches specifications across indexes/collections according to the provided filters
     *
     * @param filters Optional filters in ElasticSearch Query DSL format
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun searchSpecifications(filters: JSONObject?, options: Options, listener: ResponseListener<JSONObject>) {
        this.kuzzle.isValid()

        val data = JSONObject()

        if (listener == null) {
            throw IllegalArgumentException("listener cannot be null")
        }

        try {
            if (filters != null) {
                data.put("body", JSONObject()
                        .put("query", filters)
                )
            }

            this.kuzzle.addHeaders(data, this.getHeaders())

            this.kuzzle.query(makeQueryArgs("collection", "searchSpecifications"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result"))
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
     * Make query args kuzzle . query args.
     *
     * @param controller  API Controller to invoke
     * @param action  Controller action name
     * @return Built query arguments
     */
    fun makeQueryArgs(controller: String, action: String): io.kuzzle.sdk.core.Kuzzle.QueryArgs {
        val args = io.kuzzle.sdk.core.Kuzzle.QueryArgs()
        args.action = action
        args.controller = controller
        args.index = this.index
        args.collection = this.collection
        return args
    }

    /**
     * Returns the provided array of Documents with each one being serialized
     *
     * @param documents  Array of Document objects
     * @return A list of serialized documents
     */
    @Throws(JSONException::class)
    private fun serializeDocuments(documents: Array<Document>): JSONArray {
        val serializedDocuments = JSONArray()

        for (document in documents) {
            serializedDocuments.put(document.serialize())
        }

        return serializedDocuments
    }

    /**
     * [.count]
     */
    fun count(filters: JSONObject, listener: ResponseListener<Int>) {
        this.count(filters, null, listener)
    }

    /**
     * [.count]
     */
    fun count(listener: ResponseListener<Int>) {
        this.count(null, null, listener)
    }

    /**
     * Returns the number of documents matching the provided set of filters.
     * There is a small delay between documents creation and their existence in our search layer,
     * usually a couple of seconds.
     * That means that a document that was just been created won’t be returned by this function
     *
     * @param filters  Search filters
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun count(filters: JSONObject?, options: Options?, listener: ResponseListener<Int>) {
        if (listener == null) {
            throw IllegalArgumentException("Collection.count: listener required")
        }
        val data = JSONObject()
        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            data.put("body", filters)
            this.kuzzle.query(makeQueryArgs("document", "count"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getInt("count"))
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
     * [.create]
     */
    fun create(options: Options): Collection {
        return this.create(options, null)
    }

    /**
     * [.create]
     */
    fun create(): Collection {
        return this.create(null, null)
    }

    /**
     * [.create]
     */
    fun create(listener: ResponseListener<JSONObject>): Collection {
        return this.create(null, listener)
    }

    /**
     * Create a new empty data collection, with no associated mapping.
     * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
     *
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun create(options: Options?, listener: ResponseListener<JSONObject>?): Collection {
        val data = JSONObject()
        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("collection", "create"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.createDocument]
     */
    @Throws(JSONException::class)
    fun createDocument(id: String, content: JSONObject): Collection {
        return this.createDocument(id, content, null, null)
    }

    /**
     * [.createDocument]
     */
    @Throws(JSONException::class)
    fun createDocument(id: String, content: JSONObject, options: Options): Collection {
        return this.createDocument(id, content, options, null)
    }

    /**
     * [.createDocument]
     */
    @Throws(JSONException::class)
    fun createDocument(id: String, content: JSONObject, listener: ResponseListener<Document>): Collection {
        return this.createDocument(id, content, null, listener)
    }

    /**
     * [.createDocument]
     */
    @Throws(JSONException::class)
    fun createDocument(content: JSONObject): Collection {
        return this.createDocument(null, content, null, null)
    }

    /**
     * [.createDocument]
     */
    @Throws(JSONException::class)
    fun createDocument(content: JSONObject, options: Options): Collection {
        return this.createDocument(null, content, options, null)
    }

    /**
     * [.createDocument]
     */
    @Throws(JSONException::class)
    fun createDocument(content: JSONObject, listener: ResponseListener<Document>): Collection {
        return this.createDocument(null, content, null, listener)
    }

    /**
     * [.createDocument]
     */
    @Throws(JSONException::class)
    fun createDocument(content: JSONObject, options: Options, listener: ResponseListener<Document>): Collection {
        return this.createDocument(null, content, options, listener)
    }

    /**
     * Create document kuzzle data collection.
     *
     * @param id        document ID
     * @param content   document content
     * @param options      Request options
     * @param listener  Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun createDocument(id: String?, content: JSONObject, options: Options?, listener: ResponseListener<Document>?): Collection {
        if (content == null) {
            throw IllegalArgumentException("Cannot create an empty document")
        }

        val doc = Document(this, id, content)
        return this.createDocument(doc, options, listener)
    }

    /**
     * [.createDocument]
     */
    fun createDocument(document: Document): Collection {
        return this.createDocument(document, null, null)
    }

    /**
     * [.createDocument]
     */
    fun createDocument(document: Document, options: Options): Collection {
        return this.createDocument(document, options, null)
    }

    /**
     * [.createDocument]
     */
    fun createDocument(document: Document, listener: ResponseListener<Document>): Collection {
        return this.createDocument(document, null, listener)
    }

    /**
     * Create a new document in kuzzle
     *
     * @param document the document
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun createDocument(document: Document, options: Options?, listener: ResponseListener<Document>?): Collection {
        var action = "create"
        val data = document.serialize()

        if (options != null && options.ifExist == "replace") {
            action = "createOrReplace"
        }

        this.kuzzle.addHeaders(data, this.getHeaders())

        try {
            this.kuzzle.query(makeQueryArgs("document", action), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            val result = response.getJSONObject("result")
                            val document = Document(this@Collection, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta"))
                            document.version = result.getLong("_version")
                            listener.onSuccess(document)
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.collectionMapping]
     */
    fun collectionMapping(): CollectionMapping {
        return CollectionMapping(this)
    }

    /**
     * CollectionMapping constructor, attaching it to this
     * Collection object
     *
     * @param mapping  Raw mapping to declare
     * @return a newly instantiated CollectionMapping object
     */
    fun collectionMapping(mapping: JSONObject): CollectionMapping {
        return CollectionMapping(this, mapping)
    }

    /**
     * [.deleteDocument]
     */
    fun deleteDocument(documentId: String): Collection {
        return this.deleteDocument(documentId, null, null)
    }

    /**
     * [.deleteDocument]
     */
    fun deleteDocument(documentId: String, options: Options): Collection {
        return this.deleteDocument(documentId, options, null)
    }

    /**
     * [.deleteDocument]
     */
    fun deleteDocument(documentId: String, listener: ResponseListener<String>): Collection {
        return this.deleteDocument(documentId, null, listener)
    }

    /**
     * Delete a single document
     *
     * @param documentId  Document unique identifier
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun deleteDocument(documentId: String, options: Options?, listener: ResponseListener<String>?): Collection {
        if (documentId == null) {
            throw IllegalArgumentException("Collection.deleteDocument: documentId required")
        }
        return this.deleteDocument(documentId, null, options, listener, null)
    }

    /**
     * [.deleteDocument]
     */
    fun deleteDocument(filters: JSONObject): Collection {
        return this.deleteDocument(filters, null, null)
    }

    /**
     * [.deleteDocument]
     */
    fun deleteDocument(filters: JSONObject, options: Options): Collection {
        return this.deleteDocument(filters, options, null)
    }

    /**
     * [.deleteDocument]
     */
    fun deleteDocument(filters: JSONObject, listener: ResponseListener<Array<String>>): Collection {
        return this.deleteDocument(filters, null, listener)
    }

    /**
     * Delete documents using search filters
     *
     * @param filters  Search filters
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun deleteDocument(filters: JSONObject, options: Options?, listener: ResponseListener<Array<String>>?): Collection {
        if (filters == null) {
            throw IllegalArgumentException("Collection.deleteDocument: filters required")
        }
        return this.deleteDocument(null, filters, options, null, listener)
    }

    /**
     * Delete either a single document or multiple ones using search filters
     *
     * @param documentId  Document unique identifier
     * @param filter  Search fitlers
     * @param options  Request options
     * @param listener  Response callback listener (single document delete)
     * @param listener2  Response callback listener (document search delete)
     * @return this
     */
    protected open fun deleteDocument(documentId: String?, filter: JSONObject?, options: Options?, listener: ResponseListener<String>?, listener2: ResponseListener<Array<String>>?): Collection {
        val data = JSONObject()
        val action: String
        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            if (documentId != null) {
                data.put("_id", documentId)
                action = "delete"
            } else {
                data.put("body", JSONObject().put("query", filter))
                action = "deleteByQuery"
            }
            this.kuzzle.query(makeQueryArgs("document", action), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        if (listener != null) {
                            listener.onSuccess(response.getJSONObject("result").getString("_id"))
                        } else if (listener2 != null) {
                            val array = response.getJSONObject("result").getJSONArray("hits")
                            val length = array.length()
                            val ids = arrayOfNulls<String>(length)
                            for (i in 0 until length) {
                                ids[i] = array.getString(i)
                            }
                            listener2.onSuccess(ids)
                        }
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error) ?: listener2?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.deleteSpecifications]
     */
    @Throws(JSONException::class)
    fun deleteSpecifications(): Collection {
        return this.deleteSpecifications(Options(), null)
    }

    /**
     * [.deleteSpecifications]
     */
    @Throws(JSONException::class)
    fun deleteSpecifications(options: Options): Collection {
        return this.deleteSpecifications(options, null)
    }

    /**
     * [.deleteSpecifications]
     */
    @Throws(JSONException::class)
    fun deleteSpecifications(listener: ResponseListener<JSONObject>): Collection {
        return this.deleteSpecifications(Options(), listener)
    }

    /**
     * Deletes the current specifications for this collection
     *
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun deleteSpecifications(options: Options, listener: ResponseListener<JSONObject>?): Collection {
        val data = JSONObject()

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("collection", "deleteSpecifications"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.document]
     */
    @Throws(JSONException::class)
    fun document(): Document {
        return Document(this)
    }

    /**
     * [.document]
     */
    @Throws(JSONException::class)
    fun document(id: String): Document {
        return Document(this, id)
    }

    /**
     * [.document]
     */
    @Throws(JSONException::class)
    fun document(content: JSONObject): Document {
        return Document(this, content)
    }

    /**
     * Instantiates a Document object with a preset unique
     * identifier and content.
     * This document is attached to this data collection
     *
     * @param id  Document unique identifier
     * @param content  Document content
     * @return newly instantiated Document object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun document(id: String, content: JSONObject): Document {
        return Document(this, id, content)
    }

    /**
     * [.documentExists]
     */
    fun documentExists(documentId: String, listener: ResponseListener<JSONObject>) {
        this.documentExists(documentId, null, listener)
    }

    /**
     * Asks Kuzzle API if the provided document exists
     *
     * @param documentId  Document unique identifier
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun documentExists(documentId: String, options: Options?, listener: ResponseListener<JSONObject>?) {
        if (documentId == null) {
            throw IllegalArgumentException("Collection.documentExists: documentId required")
        }
        if (listener == null) {
            throw IllegalArgumentException("Collection.documentExists: listener required")
        }

        try {
            val data = JSONObject().put("_id", documentId)
            this.kuzzle.addHeaders(data, this.getHeaders())

            this.kuzzle.query(makeQueryArgs("document", "exists"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener.onSuccess(response)
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
     * [.fetchDocument]
     */
    fun fetchDocument(documentId: String, listener: ResponseListener<Document>) {
        this.fetchDocument(documentId, null, listener)
    }

    /**
     * Fetch a document from Kuzzle
     *
     * @param documentId  Document unique identifier
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun fetchDocument(documentId: String, options: Options?, listener: ResponseListener<Document>?) {
        if (documentId == null) {
            throw IllegalArgumentException("Collection.fetchDocument: documentId required")
        }
        if (listener == null) {
            throw IllegalArgumentException("Collection.fetchDocument: listener required")
        }

        try {
            val data = JSONObject().put("_id", documentId)
            this.kuzzle.addHeaders(data, this.getHeaders())

            this.kuzzle.query(makeQueryArgs("document", "get"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        val document = Document(this@Collection, result.getString("_id"), result.getJSONObject("_source"))

                        document.version = result.getLong("_version")
                        listener.onSuccess(document)
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
     * [.getMapping]
     */
    fun getMapping(listener: ResponseListener<CollectionMapping>) {
        this.getMapping(null, listener)
    }

    /**
     * Get the mapping for this data collection
     *
     * @param options  Request options
     * @param listener  Response callback listener
     */
    fun getMapping(options: Options?, listener: ResponseListener<CollectionMapping>) {
        if (listener == null) {
            throw IllegalArgumentException("Collection.getMapping: listener required")
        }
        CollectionMapping(this).refresh(options, listener)
    }

    /**
     * [.getSpecifications]
     */
    @Throws(JSONException::class)
    fun getSpecifications(listener: ResponseListener<JSONObject>) {
        this.getSpecifications(Options(), listener)
    }

    /**
     * Get the specifications for this collection
     *
     * @param options  Request options
     * @param listener  Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun getSpecifications(options: Options, listener: ResponseListener<JSONObject>) {
        val data = JSONObject()
                .put("body", JSONObject())

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("collection", "getSpecifications"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result"))
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
     * Create multiple documents
     *
     * @param documents  Array of Document objects to create
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun mCreateDocument(documents: Array<Document>, options: Options, listener: ResponseListener<JSONObject>?): Collection {
        if (documents.size == 0) {
            throw IllegalArgumentException("Collection.mCreateDocument: The document array should not be empty")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put("documents", this.serializeDocuments(documents))
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("document", "mCreate"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.mCreateDocument]
     */
    @Throws(JSONException::class)
    fun mCreateDocument(documents: Array<Document>, listener: ResponseListener<JSONObject>): Collection {
        return this.mCreateDocument(documents, Options(), listener)
    }

    /**
     * [.mCreateDocument]
     */
    @Throws(JSONException::class)
    fun mCreateDocument(documents: Array<Document>, options: Options): Collection {
        return this.mCreateDocument(documents, options, null)
    }

    /**
     * [.mCreateDocument]
     */
    @Throws(JSONException::class)
    fun mCreateDocument(documents: Array<Document>): Collection {
        return this.mCreateDocument(documents, Options(), null)
    }

    /**
     * Create or replace multiple documents
     *
     * @param documents  Array of Document objects to create or replace
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun mCreateOrReplaceDocument(documents: Array<Document>, options: Options, listener: ResponseListener<JSONObject>?): Collection {
        if (documents.size == 0) {
            throw IllegalArgumentException("Collection.mCreateOrReplaceDocument: The document array should not be empty")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put("documents", this.serializeDocuments(documents))
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("document", "mCreateOrReplace"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.mCreateOrReplaceDocument]
     */
    @Throws(JSONException::class)
    fun mCreateOrReplaceDocument(documents: Array<Document>, listener: ResponseListener<JSONObject>): Collection {
        return this.mCreateOrReplaceDocument(documents, Options(), listener)
    }

    /**
     * [.mCreateOrReplaceDocument]
     */
    @Throws(JSONException::class)
    fun mCreateOrReplaceDocument(documents: Array<Document>, options: Options): Collection {
        return this.mCreateOrReplaceDocument(documents, options, null)
    }

    /**
     * [.mCreateOrReplaceDocument]
     */
    @Throws(JSONException::class)
    fun mCreateOrReplaceDocument(documents: Array<Document>): Collection {
        return this.mCreateOrReplaceDocument(documents, Options(), null)
    }

    /**
     * Delete multiple documents using their unique IDs
     *
     * @param documentIds  Array of document IDs to delete
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun mDeleteDocument(documentIds: Array<String>, options: Options, listener: ResponseListener<JSONArray>?): Collection {
        if (documentIds.size == 0) {
            throw IllegalArgumentException("Collection.mDeleteDocument: The document IDs array should not be empty")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put("ids", JSONArray(Arrays.asList(*documentIds)))
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("document", "mDelete"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONArray("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.mDeleteDocument]
     */
    @Throws(JSONException::class)
    fun mDeleteDocument(documentIds: Array<String>, listener: ResponseListener<JSONArray>): Collection {
        return this.mDeleteDocument(documentIds, Options(), listener)
    }

    /**
     * [.mDeleteDocument]
     */
    @Throws(JSONException::class)
    fun mDeleteDocument(documentIds: Array<String>, options: Options): Collection {
        return this.mDeleteDocument(documentIds, options, null)
    }

    /**
     * [.mDeleteDocument]
     */
    @Throws(JSONException::class)
    fun mDeleteDocument(documentIds: Array<String>): Collection {
        return this.mDeleteDocument(documentIds, Options(), null)
    }

    /**
     * Fetch multiple documents
     *
     * @param documentIds  Array of document IDs to retrieve
     * @param options  Request options
     * @param listener  Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun mGetDocument(documentIds: Array<String>, options: Options, listener: ResponseListener<JSONObject>) {
        if (documentIds.size == 0) {
            throw IllegalArgumentException("Collection.mGetDocument: The document IDs array should not be empty")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put("ids", JSONArray(Arrays.asList(*documentIds)))
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("document", "mGet"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result"))
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
     * [.mGetDocument]
     */
    @Throws(JSONException::class)
    fun mGetDocument(documentIds: Array<String>, listener: ResponseListener<JSONObject>) {
        this.mGetDocument(documentIds, Options(), listener)
    }

    /**
     * Replace multiple documents
     *
     * @param documents  Array of Document objects to replace
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun mReplaceDocument(documents: Array<Document>, options: Options, listener: ResponseListener<JSONObject>?): Collection {
        if (documents.size == 0) {
            throw IllegalArgumentException("Collection.mReplaceDocument: The document array should not be empty")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put("documents", this.serializeDocuments(documents))
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("document", "mReplace"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.mReplaceDocument]
     */
    @Throws(JSONException::class)
    fun mReplaceDocument(documents: Array<Document>, listener: ResponseListener<JSONObject>): Collection {
        return this.mReplaceDocument(documents, Options(), listener)
    }

    /**
     * [.mReplaceDocument]
     */
    @Throws(JSONException::class)
    fun mReplaceDocument(documents: Array<Document>, options: Options): Collection {
        return this.mReplaceDocument(documents, options, null)
    }

    /**
     * [.mReplaceDocument]
     */
    @Throws(JSONException::class)
    fun mReplaceDocument(documents: Array<Document>): Collection {
        return this.mReplaceDocument(documents, Options(), null)
    }

    /**
     * Update multiple documents
     *
     * @param documents  Array of Document objects to update
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun mUpdateDocument(documents: Array<Document>, options: Options, listener: ResponseListener<JSONObject>?): Collection {
        if (documents.size == 0) {
            throw IllegalArgumentException("Collection.mUpdateDocument: The document array should not be empty")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put("documents", this.serializeDocuments(documents))
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("document", "mUpdate"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.mUpdateDocument]
     */
    @Throws(JSONException::class)
    fun mUpdateDocument(documents: Array<Document>, listener: ResponseListener<JSONObject>): Collection {
        return this.mUpdateDocument(documents, Options(), listener)
    }

    /**
     * [.mUpdateDocument]
     */
    @Throws(JSONException::class)
    fun mUpdateDocument(documents: Array<Document>, options: Options): Collection {
        return this.mUpdateDocument(documents, options, null)
    }

    /**
     * [.mUpdateDocument]
     */
    @Throws(JSONException::class)
    fun mUpdateDocument(documents: Array<Document>): Collection {
        return this.mUpdateDocument(documents, Options(), null)
    }

    /**
     * [.publishMessage]
     */
    fun publishMessage(document: Document): Collection {
        return this.publishMessage(document, null, null)
    }

    /**
     * [.publishMessage]
     */
    fun publishMessage(document: Document, listener: ResponseListener<JSONObject>): Collection {
        return this.publishMessage(document, null, listener)
    }

    /**
     * [.publishMessage]
     */
    fun publishMessage(document: Document, options: Options): Collection {
        return this.publishMessage(document, options, null)
    }

    /**
     * Publish a real-time message
     *
     * @param document  Document to publish
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun publishMessage(document: Document, options: Options?, listener: ResponseListener<JSONObject>?): Collection {
        if (document == null) {
            throw IllegalArgumentException("Cannot publish a null document")
        }

        return this.publishMessage(document.content, options, listener)
    }

    /**
     * [.publishMessage]
     */
    fun publishMessage(content: JSONObject): Collection {
        return this.publishMessage(content, null, null)
    }

    /**
     * [.publishMessage]
     */
    fun publishMessage(content: JSONObject, listener: ResponseListener<JSONObject>): Collection {
        return this.publishMessage(content, null, listener)
    }

    /**
     * [.publishMessage]
     */
    fun publishMessage(content: JSONObject, options: Options): Collection {
        return this.publishMessage(content, options, null)
    }

    /**
     * Publish a real-time message
     *
     * @param content  Message content
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun publishMessage(content: JSONObject, options: Options?, listener: ResponseListener<JSONObject>?): Collection {
        if (content == null) {
            throw IllegalArgumentException("Cannot publish null content")
        }

        try {
            val data = JSONObject().put("body", content)
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("realtime", "publish"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener?.onSuccess(response)
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
     * [.replaceDocument]
     */
    fun replaceDocument(documentId: String, content: JSONObject): Collection {
        return this.replaceDocument(documentId, content, null, null)
    }

    /**
     * [.replaceDocument]
     */
    fun replaceDocument(documentId: String, content: JSONObject, listener: ResponseListener<Document>): Collection {
        return this.replaceDocument(documentId, content, null, listener)
    }

    /**
     * [.replaceDocument]
     */
    fun replaceDocument(documentId: String, content: JSONObject, options: Options): Collection {
        return this.replaceDocument(documentId, content, options, null)
    }

    /**
     * Replace an existing document with a new one.
     *
     * @param documentId  Document unique identifier
     * @param content  New document content
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun replaceDocument(documentId: String, content: JSONObject, options: Options?, listener: ResponseListener<Document>?): Collection {
        if (documentId == null) {
            throw IllegalArgumentException("Collection.replaceDocument: documentId required")
        }

        try {
            val data = JSONObject().put("_id", documentId).put("body", content)
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("document", "createOrReplace"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            val result = response.getJSONObject("result")
                            val document = Document(this@Collection, result.getString("_id"), result.getJSONObject("_source"))
                            document.version = result.getLong("_version")
                            listener.onSuccess(document)
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.validateSpecifications]
     */
    @Throws(JSONException::class)
    fun validateSpecifications(specifications: JSONObject, listener: ResponseListener<Boolean>) {
        this.validateSpecifications(specifications, Options(), listener)
    }

    /**
     * Validates the provided specifications
     *
     * @param specifications  Specifications content
     * @param options  Request options
     * @param listener  Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun validateSpecifications(specifications: JSONObject, options: Options, listener: ResponseListener<Boolean>) {
        if (specifications == null) {
            throw IllegalArgumentException("Collection.validateSpecifications: specifications cannot be null")
        }

        if (listener == null) {
            throw IllegalArgumentException("listener cannot be null")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put(this.index, JSONObject()
                                .put(this.collection, specifications)
                        )
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("collection", "validateSpecifications"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getBoolean("valid"))
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
     * [.room]
     */
    fun room(): Room {
        return this.room(null)
    }

    /**
     * Room object constructor, attaching it to this
     * data collection
     *
     * @param options  Request options
     * @return a newly instantiated Room object
     */
    fun room(options: RoomOptions?): Room {
        return Room(this, options)
    }

    /**
     * [.setHeaders]
     */
    fun setHeaders(content: JSONObject): Collection {
        return this.setHeaders(content, false)
    }

    /**
     * Sets headers global to this data collection
     *
     * @param content  Headers content
     * @param replace  true: replace existing headers, false: append
     * @return this
     */
    fun setHeaders(content: JSONObject?, replace: Boolean): Collection {
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
                    this.headers.put(key, content.get(key))
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.subscribe]
     */
    fun subscribe(filters: JSONObject, listener: ResponseListener<NotificationResponse>): SubscribeListener {
        return this.subscribe(filters, null, listener)
    }

    /**
     * [.subscribe]
     */
    fun subscribe(options: RoomOptions, listener: ResponseListener<NotificationResponse>): SubscribeListener {
        return this.subscribe(null, options, listener)
    }

    /**
     * Subscribes to this data collection with a set of Kuzzle DSL filters.
     *
     * @param filters  Subscription filters
     * @param options  Request options
     * @param listener  Response callback listener
     * @return an object with a onDone() callback triggered when the subscription is active
     */
    fun subscribe(filters: JSONObject?, options: RoomOptions?, listener: ResponseListener<NotificationResponse>): SubscribeListener {
        if (listener == null) {
            throw IllegalArgumentException("Collection.subscribe: listener required")
        }
        this.kuzzle.isValid()
        val room = Room(this, options)
        val subscribeResponseListener = SubscribeListener()

        room.renew(filters, listener, subscribeResponseListener)

        return subscribeResponseListener
    }

    /**
     * [.truncate]
     */
    fun truncate(): Collection {
        return this.truncate(null, null)
    }

    /**
     * [.truncate]
     */
    fun truncate(options: Options): Collection {
        return this.truncate(options, null)
    }

    /**
     * [.truncate]
     */
    fun truncate(listener: ResponseListener<JSONObject>): Collection {
        return this.truncate(null, listener)
    }

    /**
     * Truncate the data collection, removing all stored documents but keeping all associated mappings.
     *
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun truncate(options: Options?, listener: ResponseListener<JSONObject>?): Collection {
        val data = JSONObject()
        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("collection", "truncate"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.updateDocument]
     */
    fun updateDocument(documentId: String, content: JSONObject): Collection {
        return this.updateDocument(documentId, content, null, null)
    }

    /**
     * [.updateDocument]
     */
    fun updateDocument(documentId: String, content: JSONObject, options: Options): Collection {
        return this.updateDocument(documentId, content, options, null)
    }

    /**
     * [.updateDocument]
     */
    fun updateDocument(documentId: String, content: JSONObject, listener: ResponseListener<Document>): Collection {
        return this.updateDocument(documentId, content, null, listener)
    }

    /**
     * Update parts of a document
     *
     * @param documentId  Document unique identifier
     * @param content  Document content to update
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    fun updateDocument(documentId: String, content: JSONObject, options: Options?, listener: ResponseListener<Document>?): Collection {
        if (documentId == null) {
            throw IllegalArgumentException("Collection.updateDocument: documentId required")
        }
        if (content == null) {
            throw IllegalArgumentException("Collection.updateDocument: content required")
        }

        try {
            val data = JSONObject().put("_id", documentId).put("body", content)
            this.kuzzle.addHeaders(data, this.getHeaders())

            if (options != null && options.retryOnConflict > 0) {
                data.put("retryOnConflict", options.retryOnConflict)
            }

            this.kuzzle.query(makeQueryArgs("document", "update"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            val result = response.getJSONObject("result")
                            val document = Document(this@Collection, result.getString("_id"), result.getJSONObject("_source"))
                            document.version = result.getLong("_version")
                            document.refresh(listener)
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * [.updateSpecifications]
     */
    @Throws(JSONException::class)
    fun updateSpecifications(specifications: JSONObject): Collection {
        return this.updateSpecifications(specifications, Options(), null)
    }

    /**
     * [.updateSpecifications]
     */
    @Throws(JSONException::class)
    fun updateSpecifications(specifications: JSONObject, options: Options): Collection {
        return this.updateSpecifications(specifications, options, null)
    }

    /**
     * [.updateSpecifications]
     */
    @Throws(JSONException::class)
    fun updateSpecifications(specifications: JSONObject, listener: ResponseListener<JSONObject>): Collection {
        return this.updateSpecifications(specifications, Options(), listener)
    }

    /**
     * Updates the current specifications of this collection
     *
     * @param specifications  Updated specifications content
     * @param options  Request options
     * @param listener  Response callback listener
     * @return this
     */
    @Throws(JSONException::class)
    fun updateSpecifications(specifications: JSONObject, options: Options, listener: ResponseListener<JSONObject>?): Collection {
        if (specifications == null) {
            throw IllegalArgumentException("Collection.updateSpecifications: specifications cannot be null")
        }

        val data = JSONObject()
                .put("body", JSONObject()
                        .put(this.index, JSONObject()
                                .put(this.collection, specifications)
                        )
                )

        try {
            this.kuzzle.addHeaders(data, this.getHeaders())
            this.kuzzle.query(makeQueryArgs("collection", "updateSpecifications"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener != null) {
                        try {
                            listener.onSuccess(response.getJSONObject("result"))
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
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

        return this
    }

    /**
     * Get the data collection global headers
     *
     * @return data collection global headers
     */
    fun getHeaders(): JSONObject {
        return this.headers
    }
}
