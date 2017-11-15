package io.kuzzle.sdk.responses

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import io.kuzzle.sdk.core.Document
import io.kuzzle.sdk.core.Collection
import io.kuzzle.sdk.core.Options
import io.kuzzle.sdk.listeners.ResponseListener

class SearchResult @JvmOverloads constructor(
        /**
         * @return Parent data collection
         */
        val collection: Collection,
        /**
         * @return Total number of fetchable documents
         */
        override val total: Long,
        /**
         * @return Fetched documents list
         */
        override val documents: List<Document>,
        /**
         * @return Search request aggregations parameters
         */
        val aggregations: JSONObject? = null,
        /**
         * @return Search request options
         */
        val options: Options = Options(),
        /**
         * @return Search request filters
         */
        val filters: JSONObject = JSONObject(), previous: SearchResult? = null) : KuzzleList<Document> {
    /**
     * @return Number of fetched documents so far
     */
    val fetchedDocument: Long

    init {
        this.fetchedDocument = if (previous != null) documents.size + previous.fetchedDocument else documents.size
    }

    /**
     * Fetches the next batch of documents
     * @param listener Response callback listener
     */
    fun fetchNext(listener: ResponseListener<SearchResult>) {
        val filters: JSONObject
        val options: Options

        try {
            options = Options(this.options)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        options.previous = this

        // retrieve next results with scroll if original search use it
        if (options.scrollId != null) {
            if (this.fetchedDocument >= this.total) {
                listener.onSuccess(null)

                return
            }

            if (options.from != null) {
                options.from = null
            }

            if (options.size != null) {
                options.size = null
            }

            this.collection.scroll(options.scrollId, options, this.filters, listener)

            return
        }

        // retrieve next results using ES's search_after
        if (options.size != null && this.filters.has("sort")) {
            if (this.fetchedDocument >= this.total) {
                listener.onSuccess(null)

                return
            }

            if (options.from != null) {
                options.from = null
            }

            try {
                val searchAfter = JSONArray()

                for (i in 0 until this.filters.getJSONArray("sort").length()) {
                    val doc = this.documents[this.documents.size - 1]
                    searchAfter.put(doc.content!!.get(this.filters.getJSONArray("sort").getJSONObject(i).keys().next()))
                }

                this.filters.put("search_after", searchAfter)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

            this.collection.search(this.filters, options, listener)

            return
        }

        // retrieve next results with  from/size if original search use it
        if (options.from != null && options.size != null) {
            try {
                filters = JSONObject(this.filters.toString())
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

            options.from = options.from!! + options.size!!

            if (options.from >= this.total) {
                listener.onSuccess(null)

                return
            }

            this.collection.search(filters, options, listener)

            return
        }

        val error: JSONObject
        try {
            error = JSONObject("Unable to retrieve next results from search: missing scrollId or from/size params")
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        listener.onError(error)
    }
}
