package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.util.Options;

/**
 * Created by kblondel on 13/10/15.
 */
public class KuzzleDataCollection {

    private final Kuzzle        kuzzle;
    private final String        collection;
    private JSONObject          headers;

    /**
     * A data collection is a set of data managed by Kuzzle. It acts like a data table for persistent documents,
     * or like a room for pub/sub messages.
     * @param kuzzle
     * @param collection
     */
    public KuzzleDataCollection(final Kuzzle kuzzle, final String collection) {
        this.kuzzle = kuzzle;
        this.collection = collection;
        this.headers = kuzzle.getHeaders();
    }

    /**
     * Executes an advanced search on the data collection.
     *
     * /!\ There is a small delay between documents creation and their existence in our advanced search layer,
     * usually a couple of seconds.
     * That means that a document that was just been created won’t be returned by this function.
     * @param filters
     * @param cb
     * @return KuzzleDataCollection object
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection   advancedSearch(JSONObject filters, ResponseListener cb) throws JSONException, IOException {
        JSONObject data = new JSONObject();
        data.put("body", filters);
        this.kuzzle.addHeaders(data, this.getHeaders());

        this.kuzzle.query(this.collection, "read", "search", data, cb);
        return this;
    }

    /**
     * Returns the number of documents matching the provided set of filters.
     *
     * There is a small delay between documents creation and their existence in our advanced search layer,
     * usually a couple of seconds.
     * That means that a document that was just been created won’t be returned by this function
     * @param filters
     * @param cb
     * @return KuzzleDataCollection
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     count(JSONObject filters, ResponseListener cb)  throws JSONException, IOException {
        JSONObject data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("body", filters);
        this.kuzzle.query(this.collection, "read", "count", data, cb);
        return this;
    }

    /**
     * Store a document or publish a realtime message.
     *
     * @param document
     * @return
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     create(KuzzleDocument document) throws JSONException, IOException {
        JSONObject data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("persist", false);
        // TODO: manage KuzzleDocument document argument

        this.kuzzle.query(this.collection, "write", "create", data);
        return this;
    }

    /**
     * Store a document or publish a realtime message.
     * @param document
     * @param options
     * @return
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     create(KuzzleDocument document, Options options) throws JSONException, IOException {
        JSONObject data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        if (options != null && options.isPersist())
            data.put("persist", true);
        else
            data.put("persist", false);
        // TODO: manage KuzzleDocument document argument

        if (options != null && options.isUpdateIfExist())
            this.kuzzle.query(this.collection, "write", "createOrUpdate", data);
        else
            this.kuzzle.query(this.collection, "write", "create", data);
        return this;
    }

    /**
     * Store a document or publish a realtime message.
     * @param document
     * @return
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     create(JSONObject document) throws JSONException, IOException {
        JSONObject data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("persist", false);
        data.put("body", document);
        this.kuzzle.query(this.collection, "write", "create", data);
        return this;
    }

    /**
     * Store a document or publish a realtime message.
     *
     * Available options:
     *   - persist (boolean - default: false): Indicates if this is a realtime message or a persistent document
     *   - updateIfExist (boolean - default: false):
     *       If the same document already exists: returns an error if sets to false.
     *       Update the existing document otherwise.
     *
     * @param document
     * @param options
     * @return KuzzleDataCollection
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     create(JSONObject document, Options options) throws JSONException, IOException {
        JSONObject data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        if (options != null && options.isPersist())
            data.put("persist", true);
        else
            data.put("persist", false);
        data.put("body", document);

        if (options != null && options.isUpdateIfExist())
            this.kuzzle.query(this.collection, "write", "createOrUpdate", data);
        else
            this.kuzzle.query(this.collection, "write", "create", data);
        return this;
    }

    /**
     * Delete persistent documents.
     *
     * There is a small delay between documents creation and their existence in our advanced search layer,
     * usually a couple of seconds.
     * That means that a document that was just been created won’t be returned by this function
     * @param documentId
     * @return KuzzleDataCollection
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     delete(String documentId) throws JSONException, IOException {
        JSONObject  data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("_id", documentId);
        this.kuzzle.query(this.collection, "write", "delete", data);
        return this;
    }

    /**
     * Delete persistent documents.
     *
     * There is a small delay between documents creation and their existence in our advanced search layer,
     * usually a couple of seconds.
     * That means that a document that was just been created won’t be returned by this function
     * @param filters
     * @return
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     delete(JSONObject filters) throws JSONException, IOException {
        JSONObject  data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("body", filters);
        this.kuzzle.query(this.collection, "write", "delete", data);
        return this;
    }

    /**
     * Retrieve a single stored document using its unique document ID.
     * @param documentId
     * @param cb
     * @return KuzzleDataCollection
     * @throws JSONException
     * @throws IOException
     */
    public KuzzleDataCollection     get(String documentId, ResponseListener cb) throws JSONException, IOException {
        JSONObject  data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("_id", documentId);
        this.kuzzle.query(this.collection, "read", "get", data, cb);
        return this;
    }

    // Alias to get
    public KuzzleDataCollection     fetch(String documentId, ResponseListener cb) throws JSONException, IOException {
        return this.get(documentId, cb);
    }

    /**
     * Retrieves all documents stored in this data collection
     * @param cb
     * @return implement
     */
    public  KuzzleDataCollection    getAll(ResponseListener cb) {
        // TODO: implement getAll method
        return this;
    }

    // Alias to getAll
    public  KuzzleDataCollection    fetchAll(ResponseListener cb) {
        return this.getAll(cb);
    }

    /**
     * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
     * @return KuzzleDataMapping
     */
    public  KuzzleDataMapping    getMapping() {
        // TODO: implement the getMapping
        return new KuzzleDataMapping(this);
    }

    /**
     * Replace an existing document with a new one.
     * @param documentId
     * @param content
     * @return
     */
    public  KuzzleDataCollection    replace(String documentId, JSONObject content) throws JSONException, IOException {
        JSONObject  data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("_id", documentId);
        data.put("body", content);
        this.kuzzle.query(this.collection, "write", "createOrUpdate", data);
        return this;
    }

    /**
     * Replace an existing document with a new one.
     * @param documentId
     * @param content
     * @return KuzzleDataCollection
     * @throws JSONException
     * @throws IOException
     */
    public  KuzzleDataCollection    replace(String documentId, KuzzleDocument content) throws JSONException, IOException {
        JSONObject  data = new JSONObject();
        this.kuzzle.addHeaders(data, this.getHeaders());
        data.put("_id", documentId);
        // TODO: handle kuzzleDocument content
        this.kuzzle.query(this.collection, "write", "createOrUpdate", data);
        return this;
    }

    /**
     * Subscribes to this data collection with a set of filters.
     * To subscribe to the entire data collection, simply provide an empty filter.
     *
     * @param filters
     * @param cb
     * @return
     * @throws NullPointerException
     * @throws IOException
     * @throws JSONException
     */
    public KuzzleRoom     subscribe(JSONObject filters, ResponseListener cb, ResponseListener ready) throws NullPointerException, IOException, JSONException, KuzzleException {
        this.kuzzle.isValid();
        KuzzleRoom room = new KuzzleRoom(this);
        return room.renew(filters, cb, ready);
    }

    /**
     * Subscribes to this data collection with a set of filters.
     * To subscribe to the entire data collection, simply provide an empty filter.
     *
     * @param filters
     * @param cb
     * @return
     * @throws NullPointerException
     * @throws IOException
     * @throws JSONException
     */
    public KuzzleRoom     subscribe(JSONObject filters, ResponseListener cb) throws NullPointerException, IOException, JSONException, KuzzleException {
        this.kuzzle.isValid();
        KuzzleRoom room = new KuzzleRoom(this);
        return room.renew(filters, cb, null);
    }

    /**
     * Subscribes to this data collection with a set of filters.
     * To subscribe to the entire data collection, simply provide an empty filter.
     *
     * @param filters
     * @param options
     * @param cb
     * @return
     */
    public KuzzleRoom     subscribe(JSONObject filters, ResponseListener cb, ResponseListener ready, KuzzleRoomOption options) throws NullPointerException, IOException, JSONException, KuzzleException {
        this.kuzzle.isValid();
        KuzzleRoom room = new KuzzleRoom(this, options);
        return room.renew(filters, cb, ready);
    }

    /**
     * Update parts of a document
     *
     * @param documentId
     * @param content
     * @return
     * @throws JSONException
     */
    public KuzzleDataCollection     update(String documentId, KuzzleDocument content) throws JSONException {
        JSONObject  object = new JSONObject();
        object.put("_id", documentId);
        // TODO: handle KuzzleDocument argument
        return this;
    }

    /**
     * Update parts of a document
     *
     * @param documentId
     * @param content
     * @return
     * @throws JSONException
     */
    public KuzzleDataCollection     update(String documentId, JSONObject content) throws JSONException, IOException {
        JSONObject  data = new JSONObject();
        data.put("_id", documentId);
        data.put("body", content);
        this.kuzzle.addHeaders(data, this.headers);
        this.kuzzle.query(this.collection, "write", "update", data);
        return this;
    }

    public Kuzzle getKuzzle() {
        return kuzzle;
    }

    public String getCollection() {
        return collection;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public void setHeaders(JSONObject headers) {
        this.headers = headers;
    }
}
