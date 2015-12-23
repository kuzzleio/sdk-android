package io.kuzzle.sdk.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.ResponseListener;

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
   * @param filter the filter
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection advancedSearch(JSONObject filter) throws KuzzleException, JSONException {
    return this.advancedSearch(filter, null, null);
  }

  /**
   * Executes an advanced search on the data collection.
   * /!\ There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function.
   *
   * @param filter  the filter
   * @param options the options
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection advancedSearch(JSONObject filter, KuzzleOptions options) throws KuzzleException, JSONException {
    return this.advancedSearch(filter, options, null);
  }

  /**
   * Executes an advanced search on the data collection.
   * /!\ There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function.
   *
   * @param filter   the filter
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection advancedSearch(JSONObject filter, ResponseListener listener) throws KuzzleException, JSONException {
    return this.advancedSearch(filter, null, listener);
  }

  /**
   * Executes an advanced search on the data collection.
   * /!\ There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function.
   *
   * @param filters  the filters
   * @param options  the options
   * @param listener the listener
   * @return KuzzleDataCollection object
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection advancedSearch(JSONObject filters, final KuzzleOptions options, final ResponseListener listener) throws KuzzleException, JSONException {
    this.kuzzle.isValid();
    JSONObject data = new JSONObject();
    if (filters != null) {
      data.put("body", filters);
    }
    this.kuzzle.addHeaders(data, this.getHeaders());

    this.kuzzle.query(this.collection, "read", "search", data, options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          JSONArray docs = new JSONArray();
          JSONArray hits = object.getJSONObject("hits").getJSONArray("hits");
          JSONObject result = new JSONObject();
          for (int i = 0; i < hits.length(); i++) {
            KuzzleDocument doc = new KuzzleDocument(KuzzleDataCollection.this);
            doc.setId(hits.getJSONObject(i).getString("_id"));
            doc.setContent(hits.getJSONObject(i).getJSONObject("_source"));
            docs.put(doc);
          }
          result.put("documents", docs);
          result.put("total", hits.length());
          if (listener != null) {
            listener.onSuccess(result);
          }
        } catch(JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {
        if (listener != null) {
          listener.onError(error);
        }
      }
    });
    return this;
  }

  /**
   * Returns the number of documents matching the provided set of filters.
   
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection count(JSONObject filters) throws KuzzleException, JSONException {
    return this.count(filters, null, null);
  }

  /**
   * Returns the number of documents matching the provided set of filters.
   
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @param options the options
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection count(JSONObject filters, KuzzleOptions options) throws KuzzleException, JSONException {
    return this.count(filters, options, null);
  }

  /**
   * Returns the number of documents matching the provided set of filters.
   
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters  the filters
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection count(JSONObject filters, ResponseListener listener) throws KuzzleException, JSONException {
    return this.count(filters, null, listener);
  }

  /**
   * Returns the number of documents matching the provided set of filters.
   
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @param options the options
   * @param cb      the cb
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection count(JSONObject filters, KuzzleOptions options, ResponseListener cb) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("body", filters);
    this.kuzzle.query(this.collection, "read", "count", data, options, cb);
    return this;
  }


  /**
   * Create a new empty data collection, with no associated mapping.
   
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection create(KuzzleOptions options, ResponseListener listener) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    this.kuzzle.query(this.collection, "write", "createCollection", data, options, listener);
    return this;
  }

  /**
   * Create a new document in Kuzzle
   *
   * @param document the document
   * @return kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection createDocument(KuzzleDocument document) throws JSONException, KuzzleException {
    return this.createDocument(document, null, null);
  }

  /**
   * Create a new document in kuzzle
   *
   * @param document the document
   * @param options  the options
   * @return kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection createDocument(KuzzleDocument document, KuzzleOptions options) throws JSONException, KuzzleException {
    return this.createDocument(document, options, null);
  }

  public KuzzleDataCollection createDocument(KuzzleDocument document, ResponseListener listener) throws KuzzleException, JSONException {
    return this.createDocument(document, null ,listener);
  }

  /**
   * Create a new document in kuzzle
   *
   * @param document the document
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection createDocument(KuzzleDocument document, KuzzleOptions options, ResponseListener listener) throws JSONException, KuzzleException {
    document.put("persist", true);
    String create = "create";
    if (options != null && options.isUpdateIfExists())
      create = "createOrUpdate";

    this.kuzzle.query(this.collection, "write", create, document, options, listener);
    return this;
  }

  /**
   * Delete kuzzle data collection.
   *
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection delete() throws JSONException, KuzzleException {
    return this.delete(null, null);
  }

  /**
   * Delete kuzzle data collection.
   *
   * @param options the options
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection delete(KuzzleOptions options) throws JSONException, KuzzleException {
    return this.delete(options, null);
  }

  /**
   * Delete kuzzle data collection.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection delete(ResponseListener listener) throws JSONException, KuzzleException {
    return this.delete(null, listener);
  }

  /**
   * Delete kuzzle data collection.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection delete(KuzzleOptions options, ResponseListener listener) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();

    this.kuzzle.addHeaders(data, this.getHeaders());
    this.kuzzle.query(this.collection, "admin", "deleteCollection", data, options, listener);
    return this;
  }

  /**
   * Delete a persistent document.
   
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param documentId the document id
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection deleteDocument(String documentId) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("_id", documentId);
    this.kuzzle.query(this.collection, "write", "delete", data);
    return this;
  }

  /**
   * Delete a persistent document.
   
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @return kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection deleteDocument(JSONObject filters) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("body", filters);
    this.kuzzle.query(this.collection, "write", "delete", data);
    return this;
  }

  public KuzzleDataCollection fetchDocument(String documentId, ResponseListener listener) throws KuzzleException, JSONException {
    return this.fetchDocument(documentId, null, listener);
  }

  /**
   * Retrieve a single stored document using its unique document ID.
   *
   * @param documentId the document id
   * @param options    the options
   * @param cb         the cb
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection fetchDocument(String documentId, KuzzleOptions options, ResponseListener cb) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.getHeaders());
    data.put("_id", documentId);
    this.kuzzle.query(this.collection, "read", "get", data, options, cb);
    return this;
  }

  /**
   * Retrieves all documents stored in this data collection.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection fetchAllDocuments(ResponseListener listener) throws KuzzleException, JSONException {
    return this.fetchAllDocuments(null, listener);
  }

  /**
   * Retrieves all documents stored in this data collection.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection fetchAllDocuments(KuzzleOptions options, ResponseListener listener) throws JSONException, KuzzleException {
    return this.advancedSearch(null, options, listener);
  }

  /**
   * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
   *
   * @return the mapping
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection getMapping() throws JSONException, KuzzleException {
    return this.getMapping(null, null);
  }

  /**
   * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
   *
   * @param options the options
   * @return the mapping
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection getMapping(KuzzleOptions options) throws JSONException, KuzzleException {
    return this.getMapping(options, null);
  }

  /**
   * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
   *
   * @param listener the listener
   * @return the mapping
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection getMapping(ResponseListener listener) throws JSONException, KuzzleException {
    return this.getMapping(null, listener);
  }

  /**
   * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
   *
   * @param options  the options
   * @param listener the listener
   * @return the mapping
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection getMapping(KuzzleOptions options, ResponseListener listener) throws KuzzleException, JSONException {
    new KuzzleDataMapping(this).refresh(options, listener);
    return this;
  }

  /**
   * Publish a realtime message
   *
   * @param document the document
   * @param options  the options
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection publishMessage(KuzzleDocument document, KuzzleOptions options) throws JSONException, KuzzleException {
    this.kuzzle.addHeaders(document, this.getHeaders());
    this.kuzzle.query(this.collection, "write", "publish", document, options, null);
    return this;
  }

  /**
   * Publish a realtime message
   *
   * @param document the document
   * @return kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection publishMessage(KuzzleDocument document) throws JSONException, KuzzleException {
    return this.publishMessage(document, null);
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping the mapping
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection putMapping(KuzzleDataMapping mapping) throws JSONException, KuzzleException {
    return this.putMapping(mapping, null, null);
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping the mapping
   * @param options the options
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection putMapping(KuzzleDataMapping mapping, KuzzleOptions options) throws JSONException, KuzzleException {
    return this.putMapping(mapping, options, null);
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping  the mapping
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection putMapping(KuzzleDataMapping mapping, ResponseListener listener) throws JSONException, KuzzleException {
    return this.putMapping(mapping, null, listener);
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping  the mapping
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection putMapping(KuzzleDataMapping mapping, KuzzleOptions options, ResponseListener listener) throws KuzzleException, JSONException {
    mapping.apply(options, listener);
    return this;
  }

  /**
   * Replace an existing document with a new one.
   *
   * @param documentId the document id
   * @param content    the content
   * @return KuzzleDataCollection kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection replaceDocument(String documentId, KuzzleDocument content) throws JSONException, KuzzleException {
    content.setId(documentId);
    this.kuzzle.addHeaders(content, this.getHeaders());
    this.kuzzle.query(this.collection, "write", "createOrUpdate", content);
    return this;
  }

  /**
   * Sets headers.
   *
   * @param content the content
   * @return the headers
   * @throws JSONException the json exception
   */
  public KuzzleDataCollection setHeaders(JSONObject content) throws JSONException {
    return this.setHeaders(content, false);
  }

  /**
   * Sets headers.
   *
   * @param content the content
   * @param replace the replace
   * @return the headers
   * @throws JSONException the json exception
   */
  public KuzzleDataCollection setHeaders(JSONObject content, boolean replace) throws JSONException {
    this.kuzzle.setHeaders(content, replace);
    return this;
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param filter  the filter
   * @param options KuzzleRoomOptions
   * @return the kuzzle room
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleRoom subscribe(JSONObject filter, KuzzleRoomOptions options) throws JSONException, KuzzleException {
    return this.subscribe(filter, options, null);
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param options KuzzleRoomOptions
   * @return the kuzzle room
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleRoom subscribe(KuzzleRoomOptions options) throws JSONException, KuzzleException {
    return this.subscribe(null, options, null);
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param filter   the filter
   * @param callback the callback
   * @return the kuzzle room
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleRoom subscribe(JSONObject filter, ResponseListener callback) throws JSONException, KuzzleException {
    return this.subscribe(filter, null, callback);
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param filter the filter
   * @return the kuzzle room
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleRoom subscribe(JSONObject filter) throws JSONException, KuzzleException {
    return this.subscribe(filter, null, null);
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @return the kuzzle room
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleRoom subscribe() throws JSONException, KuzzleException {
    return this.subscribe(null, null, null);
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param filters the filters
   * @param options the options
   * @param cb      the cb
   * @return kuzzle room
   * @throws NullPointerException the null pointer exception
   * @throws JSONException        the json exception
   * @throws KuzzleException      the kuzzle exception
   */
  public KuzzleRoom subscribe(JSONObject filters, KuzzleRoomOptions options, ResponseListener cb) throws NullPointerException, JSONException, KuzzleException {
    this.kuzzle.isValid();
    KuzzleRoom room = new KuzzleRoom(this, options);
    return room.renew(filters, cb);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   
   * This method is a lot faster than removing all documents using a query.
   *
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection truncate() throws KuzzleException, JSONException {
    return this.truncate(null, null);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   
   * This method is a lot faster than removing all documents using a query.
   *
   * @param options the options
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection truncate(KuzzleOptions options) throws KuzzleException, JSONException {
    return this.truncate(options, null);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   
   * This method is a lot faster than removing all documents using a query.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection truncate(ResponseListener listener) throws KuzzleException, JSONException {
    return this.truncate(null, listener);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   
   * This method is a lot faster than removing all documents using a query.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection truncate(KuzzleOptions options, ResponseListener listener) throws JSONException, KuzzleException {
    JSONObject  data = new JSONObject();
    this.kuzzle.addHeaders(data, this.kuzzle.getHeaders());
    this.kuzzle.query(this.collection, "admin", "truncateCollection", data, options, listener);
    return this;
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection updateDocument(String documentId, KuzzleDocument content) throws KuzzleException, JSONException {
    return this.updateDocument(documentId, content, null, null);
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @param options    the options
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection updateDocument(String documentId, KuzzleDocument content, KuzzleOptions options) throws KuzzleException, JSONException {
    return this.updateDocument(documentId, content, options, null);
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @param listener   the listener
   * @return the kuzzle data collection
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataCollection updateDocument(String documentId, KuzzleDocument content, ResponseListener listener) throws KuzzleException, JSONException {
    return this.updateDocument(documentId, content, null, listener);
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @param options    the options
   * @param listener   the listener
   * @return kuzzle data collection
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataCollection updateDocument(String documentId, KuzzleDocument content, KuzzleOptions options, final ResponseListener listener) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.kuzzle.getHeaders());
    data.put("_id", documentId);
    data.put("body", content.getContent());
    if (listener == null) {
      this.kuzzle.query(this.collection, "write", "update", data, options, null);
    } else {
      this.kuzzle.query(this.collection, "write", "update", data, options, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) {
            listener.onSuccess(object);
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    }
    return this;
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
    return kuzzle.getHeaders();
  }

}
