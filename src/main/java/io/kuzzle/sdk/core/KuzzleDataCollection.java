package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.util.Options;

/**
 * The type Kuzzle data collection.
 */
public class KuzzleDataCollection {

  private final Kuzzle kuzzle;
  private final String collection;
  private JSONObject headers;

  /**
   * A data collection is a set of data managed by Kuzzle. It acts like a data table for persistent documents,
   * or like a room for pub/sub messages.
   *
   * @param kuzzle     the kuzzle
   * @param collection the collection
   */
  public KuzzleDataCollection(final Kuzzle kuzzle, final String collection) {
    this.kuzzle = kuzzle;
    this.collection = collection;
    this.headers = kuzzle.getHeaders();
  }

  /**
   * Executes an advanced search on the data collection.
   * /!\ There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function.
   *
   * @param filters the filters
   * @param cb      the cb
   * @return KuzzleDataCollection object
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection advancedSearch(JSONObject filters, ResponseListener cb) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    data.put("body", filters);
    this.kuzzle.addHeaders(data, this.getHeaders());

    this.kuzzle.query(this.collection, "read", "search", data, cb);
    return this;
  }

  /**
   * Returns the number of documents matching the provided set of filters.
   * <p>
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @param cb      the cb
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection count(JSONObject filters, ResponseListener cb) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("body", filters);
    this.kuzzle.query(this.collection, "read", "count", data, cb);
    return this;
  }

  /**
   * Store a document or publish a realtime message.
   *
   * @param document the document
   * @return kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection createDocument(KuzzleDocument document) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("persist", false);
    // TODO: manage KuzzleDocument document argument

    this.kuzzle.query(this.collection, "write", "create", data);
    return this;
  }

  /**
   * Store a document or publish a realtime message.
   *
   * @param document the document
   * @param options  the options
   * @return kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection createDocument(KuzzleDocument document, Options options) throws JSONException, IOException {
    if (options != null && options.isPersist())
      document.put("persist", true);
    else
      document.put("persist", false);

    if (options != null && options.isUpdateIfExist())
      this.kuzzle.query(this.collection, "write", "createOrUpdate", document);
    else
      this.kuzzle.query(this.collection, "write", "create", document);
    return this;
  }

  /**
   * Create a new document in Kuzzle.
   *
   * @param document the document
   * @param options  the options
   * @param listener the listener
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection createDocument(JSONObject document, KuzzleOptions options, ResponseListener listener) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("persist", true);
    data.put("body", document);

    String create = "create";
    if (options != null && options.isUpdateIfExists())
      create = "createOrUpdate";
    this.kuzzle.query(this.collection, "write", create, data, options, listener);
    return this;
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param document the document
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection createDocument(JSONObject document, ResponseListener listener) throws JSONException, IOException {
    return createDocument(document, null, listener);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param document the document
   * @return the kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection createDocument(JSONObject document) throws JSONException, IOException {
    return createDocument(document, null, null);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param document the document
   * @param options  the options
   * @return the kuzzle data collection
   * @throws IOException   the io exception
   * @throws JSONException the json exception
   */
  public KuzzleDataCollection createDocument(JSONObject document, KuzzleOptions options) throws IOException, JSONException {
    return createDocument(document, options, null);
  }

  /**
   * Publish a realtime message
   *
   * @param document the document
   * @return kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection publish(JSONObject document) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    data.put("body", document);
    data.put("persist", false);
    this.kuzzle.addHeaders(data, this.getHeaders());
    this.kuzzle.query(this.collection, "write", "create", data);
    return this;
  }

  /**
   * Publish kuzzle data collection.
   *
   * @param document the document
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection publish(KuzzleDocument document) {
    JSONObject data = new JSONObject();
    // TODO KuzzleDocument
    return this;
  }

  /**
   * Delete persistent documents.
   * <p>
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param documentId the document id
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection delete(String documentId) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("_id", documentId);
    this.kuzzle.query(this.collection, "write", "delete", data);
    return this;
  }

  /**
   * Delete persistent documents.
   * <p>
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @return kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection delete(JSONObject filters) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("body", filters);
    this.kuzzle.query(this.collection, "write", "delete", data);
    return this;
  }

  /**
   * Retrieve a single stored document using its unique document ID.
   *
   * @param documentId the document id
   * @param cb         the cb
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection fetch(String documentId, ResponseListener cb) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("_id", documentId);
    this.kuzzle.query(this.collection, "read", "get", data, cb);
    return this;
  }

  /**
   * Retrieves all documents stored in this data collection
   *
   * @param cb the cb
   * @return implement kuzzle data collection
   */
  public KuzzleDataCollection fetchAll(ResponseListener cb) {
    // TODO: implement getAll method
    return this;
  }

  /**
   * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
   *
   * @return KuzzleDataMapping mapping
   */
  public KuzzleDataMapping getMapping() {
    // TODO: implement the getMapping
    return new KuzzleDataMapping(this);
  }

  /**
   * Replace an existing document with a new one.
   *
   * @param documentId the document id
   * @param content    the content
   * @return kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection replace(String documentId, JSONObject content) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("_id", documentId);
    data.put("body", content);
    this.kuzzle.query(this.collection, "write", "createOrUpdate", data);
    return this;
  }

  /**
   * Replace an existing document with a new one.
   *
   * @param documentId the document id
   * @param content    the content
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection replace(String documentId, KuzzleDocument content) throws JSONException, IOException {
    JSONObject data = new JSONObject();
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
   * @param filters the filters
   * @param cb      the cb
   * @return kuzzle room
   * @throws NullPointerException the null pointer exception
   * @throws IOException          the io exception
   * @throws JSONException        the json exception
   * @throws KuzzleException      the kuzzle exception
   */
  public KuzzleRoom subscribe(JSONObject filters, ResponseListener cb) throws NullPointerException, IOException, JSONException, KuzzleException {
    this.kuzzle.isValid();
    KuzzleRoom room = new KuzzleRoom(this);
    return room.renew(filters, cb);
  }

  /**
   * Subscribe kuzzle room.
   *
   * @return the kuzzle room
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   * @throws IOException     the io exception
   */
  public KuzzleRoom subscribe() throws JSONException, KuzzleException, IOException {
    return subscribe(null, null);
  }


  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param filters the filters
   * @param cb      the cb
   * @param options the options
   * @return kuzzle room
   * @throws NullPointerException the null pointer exception
   * @throws IOException          the io exception
   * @throws JSONException        the json exception
   * @throws KuzzleException      the kuzzle exception
   */
  public KuzzleRoom subscribe(JSONObject filters, KuzzleRoomOptions options, ResponseListener cb) throws NullPointerException, IOException, JSONException, KuzzleException {
    this.kuzzle.isValid();
    KuzzleRoom room = new KuzzleRoom(this, options);
    return room.renew(filters, cb);
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @return kuzzle data collection
   * @throws JSONException the json exception
   */
  public KuzzleDataCollection updateDocument(String documentId, KuzzleDocument content) throws JSONException {
    JSONObject object = new JSONObject();
    object.put("_id", documentId);
    // TODO: handle KuzzleDocument argument
    return this;
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @param options    the options
   * @return kuzzle data collection
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataCollection updateDocument(String documentId, JSONObject content, KuzzleOptions options) throws JSONException, IOException {
    JSONObject data = new JSONObject();
    data.put("_id", documentId);
    data.put("body", content);
    this.kuzzle.addHeaders(data, this.headers);
    this.kuzzle.query(this.collection, "write", "update", data, options, null);
    return this;
  }

  /**
   * Update document kuzzle data collection.
   *
   * @param documentId the document id
   * @param content    the content
   * @return the kuzzle data collection
   * @throws IOException   the io exception
   * @throws JSONException the json exception
   */
  public KuzzleDataCollection updateDocument(String documentId, JSONObject content) throws IOException, JSONException {
    return this.updateDocument(documentId, content, null);
  }

  /**
   * Gets kuzzle.
   *
   * @return the kuzzle
   */
  public Kuzzle getKuzzle() {
    return kuzzle;
  }

  /**
   * Gets collection.
   *
   * @return the collection
   */
  public String getCollection() {
    return collection;
  }

  /**
   * Gets headers.
   *
   * @return the headers
   */
  public JSONObject getHeaders() {
    return headers;
  }

  /**
   * Sets headers.
   *
   * @param headers the headers
   */
  public void setHeaders(JSONObject headers) {
    this.headers = headers;
  }
}
