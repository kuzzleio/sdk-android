package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.KuzzleSubscribeListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleDocumentList;
import io.kuzzle.sdk.responses.KuzzleNotificationResponse;

/**
 * The type Kuzzle data collection.
 */
public class Collection {

  private final Kuzzle kuzzle;
  private final String collection;
  private final String index;
  private KuzzleResponseListener<Room> subscribeCallback;
  private JSONObject subscribeError = null;
  private Room subscribeRoom = null;

  /**
   * The Headers.
   */
  protected JSONObject headers;

  /**
   * A data collection is a set of data managed by Kuzzle. It acts like a data table for persistent documents,
   * or like a room for pub/sub messages.
   *
   * @param kuzzle     the kuzzle
   * @param collection the collection
   * @param index      the index
   */
  public Collection(@NonNull final Kuzzle kuzzle, @NonNull final String collection, @NonNull final String index) {
    if (kuzzle == null) {
      throw new IllegalArgumentException("Collection: need a Kuzzle instance to initialize");
    }

    if (index == null || collection == null) {
      throw new IllegalArgumentException("Collection: index and collection required");
    }
    this.kuzzle = kuzzle;
    this.collection = collection;
    this.index = index;

    try {
      this.headers = new JSONObject(kuzzle.getHeaders().toString());
    }
    catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes a search on the data collection.
   * /!\ There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function.
   *
   * @param filter   the filter
   * @param listener the listener
   */
  public void search(final JSONObject filter, final KuzzleResponseListener<KuzzleDocumentList> listener) {
    this.search(filter, null, listener);
  }

  /**
   * Executes a search on the data collection.
   * /!\ There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function.
   *
   * @param filters  the filters
   * @param options  the options
   * @param listener the listener
   */
  public void search(final JSONObject filters, final Options options, @NonNull final KuzzleResponseListener<KuzzleDocumentList> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("listener cannot be null");
    }
    this.kuzzle.isValid();
    JSONObject data = new JSONObject();
    try {
      if (filters != null) {
        data.put("body", filters);
      }

      this.kuzzle.addHeaders(data, this.getHeaders());

      this.kuzzle.query(makeQueryArgs("document", "search"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            KuzzleDocumentList response;
            JSONArray hits = object.getJSONObject("result").getJSONArray("hits");
            List<Document> docs = new ArrayList<Document>();
            for (int i = 0; i < hits.length(); i++) {
              JSONObject hit = hits.getJSONObject(i);
              Document doc = new Document(Collection.this, hit.getString("_id"), hit.getJSONObject("_source"));
              docs.add(doc);
            }
            if (object.getJSONObject("result").has("aggregations")) {
              response = new KuzzleDocumentList(docs, object.getJSONObject("result").getInt("total"), object.getJSONObject("result").getJSONObject("aggregations"));
            }
            else {
              response = new KuzzleDocumentList(docs, object.getJSONObject("result").getInt("total"));
            }
            listener.onSuccess(response);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Make query args kuzzle . query args.
   *
   * @param controller the controller
   * @param action     the action
   * @return the kuzzle . query args
   */
  public io.kuzzle.sdk.core.Kuzzle.QueryArgs makeQueryArgs(final String controller, final String action) {
    io.kuzzle.sdk.core.Kuzzle.QueryArgs args = new io.kuzzle.sdk.core.Kuzzle.QueryArgs();
    args.action = action;
    args.controller = controller;
    args.index = this.index;
    args.collection = this.collection;
    return args;
  }

  /**
   * Count kuzzle data collection.
   *
   * @param filters  the filters
   * @param listener the listener
   */
  public void count(final JSONObject filters, @NonNull final KuzzleResponseListener<Integer> listener) {
    this.count(filters, null, listener);
  }

  /**
   * Count kuzzle data collection.
   *
   * @param listener the listener
   */
  public void count(@NonNull final KuzzleResponseListener<Integer> listener) {
    this.count(null, null, listener);
  }

  /**
   * Returns the number of documents matching the provided set of filters.
   * There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters  the filters
   * @param options  the options
   * @param listener the cb
   */
  public void count(final JSONObject filters, final Options options, @NonNull final KuzzleResponseListener<Integer> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Collection.count: listener required");
    }
    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      data.put("body", filters);
      this.kuzzle.query(makeQueryArgs("document", "count"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getInt("count"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create a new empty data collection, with no associated mapping.
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @param options the options
   * @return the kuzzle data collection
   */
  public Collection create(final Options options) {
    return this.create(options, null);
  }

  /**
   * Create a new empty data collection, with no associated mapping.
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public Collection create(final KuzzleResponseListener<JSONObject> listener) {
    return this.create(null, listener);
  }

  /**
   * Create a new empty data collection, with no associated mapping.
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @return the kuzzle data collection
   */
  public Collection create() {
    return this.create(null, null);
  }

  /**
   * Create a new empty data collection, with no associated mapping.
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public Collection create(final Options options, final KuzzleResponseListener<JSONObject> listener) {
    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("collection", "create"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              listener.onSuccess(response.getJSONObject("result"));
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param id      - document ID
   * @param content - document content
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(final String id, @NonNull final JSONObject content) throws JSONException {
    return this.createDocument(id, content, null, null);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param id      - document ID
   * @param content - document content
   * @param opts    - optional arguments
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(final String id, @NonNull final JSONObject content, Options opts) throws JSONException {
    return this.createDocument(id, content, opts, null);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param id       - document ID
   * @param content  - document content
   * @param listener - result listener
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(final String id, @NonNull final JSONObject content, final KuzzleResponseListener<Document> listener) throws JSONException {
    return this.createDocument(id, content, null, listener);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param content - document content
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(@NonNull final JSONObject content) throws JSONException {
    return this.createDocument(null, content, null, null);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param content - document content
   * @param opts    - optional arguments
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(@NonNull final JSONObject content, Options opts) throws JSONException {
    return this.createDocument(null, content, opts, null);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param content  - document content
   * @param listener - result listener
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(@NonNull final JSONObject content, final KuzzleResponseListener<Document> listener) throws JSONException {
    return this.createDocument(null, content, null, listener);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param content  - document content
   * @param opts     - optional arguments
   * @param listener - result listener
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(@NonNull final JSONObject content, Options opts, final KuzzleResponseListener<Document> listener) throws JSONException {
    return this.createDocument(null, content, opts, listener);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param id       - document ID
   * @param content  - document content
   * @param opts     - optional arguments
   * @param listener - result listener
   * @return this object
   * @throws JSONException the json exception
   */
  public Collection createDocument(final String id, @NonNull final JSONObject content, Options opts, final KuzzleResponseListener<Document> listener) throws JSONException {
    if (content == null) {
      throw new IllegalArgumentException("Cannot create an empty document");
    }

    Document doc = new Document(this, id, content);
    return this.createDocument(doc, opts, listener);
  }

  /**
   * Create a new document in Kuzzle
   *
   * @param document the document
   * @return kuzzle data collection
   */
  public Collection createDocument(final Document document) {
    return this.createDocument(document, null, null);
  }

  /**
   * Create a new document in kuzzle
   *
   * @param document the document
   * @param options  the options
   * @return the kuzzle data collection
   */
  public Collection createDocument(final Document document, final Options options) {
    return this.createDocument(document, options, null);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param document the document
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public Collection createDocument(final Document document, final KuzzleResponseListener<Document> listener) {
    return this.createDocument(document, null, listener);
  }

  /**
   * Create a new document in kuzzle
   *
   * @param document the document
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public Collection createDocument(final Document document, final Options options, final KuzzleResponseListener<Document> listener) {
    String action = (options != null && options.isUpdateIfExists()) ? "createOrReplace" : "create";
    JSONObject data = document.serialize();

    this.kuzzle.addHeaders(data, this.getHeaders());

    try {
      this.kuzzle.query(makeQueryArgs("document", action), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              JSONObject result = response.getJSONObject("result");
              Document document = new Document(Collection.this, result.getString("_id"), result.getJSONObject("_source"));
              document.setVersion(result.getLong("_version"));
              listener.onSuccess(document);
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Data mapping factory kuzzle data mapping.
   *
   * @return the kuzzle data mapping
   */
  public CollectionMapping collectionMapping() {
    return new CollectionMapping(this);
  }

  /**
   * Data mapping factory kuzzle data mapping.
   *
   * @param mapping the mapping
   * @return the kuzzle data mapping
   */
  public CollectionMapping collectionMapping(JSONObject mapping) {
    return new CollectionMapping(this, mapping);
  }


  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param documentId the document id
   * @return Collection kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final String documentId) {
    return this.deleteDocument(documentId, null, null);
  }

  /**
   * Delete document kuzzle data collection.
   *
   * @param documentId the document id
   * @param options    the options
   * @return the kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final String documentId, Options options) {
    return this.deleteDocument(documentId, options, null);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param documentId the document id
   * @param listener   the listener
   * @return Collection kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final String documentId, final KuzzleResponseListener<String> listener) {
    return this.deleteDocument(documentId, null, listener);
  }

  /**
   * Delete document kuzzle data collection.
   *
   * @param documentId the document id
   * @param options    the options
   * @param listener   the listener
   * @return the kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final String documentId, final Options options, final KuzzleResponseListener<String> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("Collection.deleteDocument: documentId required");
    }
    return this.deleteDocument(documentId, null, options, listener, null);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @return Collection kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final JSONObject filters) {
    return this.deleteDocument(filters, null, null);
  }

  /**
   * Delete document kuzzle data collection.
   *
   * @param filters the filters
   * @param options the options
   * @return the kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final JSONObject filters, final Options options) {
    return this.deleteDocument(filters, options, null);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters  the filters
   * @param listener the listener
   * @return Collection kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final JSONObject filters, final KuzzleResponseListener<String[]> listener) {
    return this.deleteDocument(filters, null, listener);
  }

  /**
   * Delete document kuzzle data collection.
   *
   * @param filters  the filters
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public Collection deleteDocument(@NonNull final JSONObject filters, final Options options, final KuzzleResponseListener<String[]> listener) {
    if (filters == null) {
      throw new IllegalArgumentException("Collection.deleteDocument: filters required");
    }
    return this.deleteDocument(null, filters, options, null, listener);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param documentId the document id
   * @param filter     the filter
   * @param options    the options
   * @param listener   the listener
   * @param listener2  the listener 2
   * @return Collection kuzzle data collection
   */
  protected Collection deleteDocument(final String documentId, final JSONObject filter, final Options options, final KuzzleResponseListener<String> listener, final KuzzleResponseListener<String[]> listener2) {
    JSONObject data = new JSONObject();
    String action;
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      if (documentId != null) {
        data.put("_id", documentId);
        action = "delete";
      } else {
        data.put("body", filter);
        action = "deleteByQuery";
      }
      this.kuzzle.query(makeQueryArgs("document", action), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            if (listener != null) {
              listener.onSuccess(response.getJSONObject("result").getString("_id"));
            } else if (listener2 != null) {
              JSONArray array = response.getJSONObject("result").getJSONArray("hits");
              int length = array.length();
              String[] ids = new String[length];
              for (int i = 0; i < length; i++) {
                ids[i] = array.getString(i);
              }
              listener2.onSuccess(ids);
            }
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          } else if (listener2 != null) {
            listener2.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Document factory kuzzle document.
   *
   * @return the kuzzle document
   * @throws JSONException the json exception
   */
  public Document document() throws JSONException {
    return new Document(this);
  }

  /**
   * Document factory kuzzle document.
   *
   * @param id the id
   * @return the kuzzle document
   * @throws JSONException the json exception
   */
  public Document document(final String id) throws JSONException {
    return new Document(this, id);
  }

  /**
   * Document factory kuzzle document.
   *
   * @param content the content
   * @return the kuzzle document
   * @throws JSONException the json exception
   */
  public Document document(final JSONObject content) throws JSONException {
    return new Document(this, content);
  }

  /**
   * Document factory kuzzle document.
   *
   * @param id      the id
   * @param content the content
   * @return the kuzzle document
   * @throws JSONException the json exception
   */
  public Document document(final String id, final JSONObject content) throws JSONException {
    return new Document(this, id, content);
  }

  /**
   * Fetch document kuzzle data collection.
   *
   * @param documentId the document id
   * @param listener   the listener
   */
  public void fetchDocument(@NonNull final String documentId, @NonNull final KuzzleResponseListener<Document> listener) {
    this.fetchDocument(documentId, null, listener);
  }

  /**
   * Retrieve a single stored document using its unique document ID.
   *
   * @param documentId the document id
   * @param options    the options
   * @param listener   the listener
   */
  public void fetchDocument(@NonNull final String documentId, final Options options, final KuzzleResponseListener<Document> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("Collection.fetchDocument: documentId required");
    }
    if (listener == null) {
      throw new IllegalArgumentException("Collection.fetchDocument: listener required");
    }

    try {
      JSONObject data = new JSONObject().put("_id", documentId);
      this.kuzzle.addHeaders(data, this.getHeaders());

      this.kuzzle.query(makeQueryArgs("document", "get"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            Document document = new Document(Collection.this, result.getString("_id"), result.getJSONObject("_source"));

            document.setVersion(result.getLong("_version"));
            listener.onSuccess(document);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves all documents stored in this data collection.
   *
   * @param listener the listener
   */
  public void fetchAllDocuments(@NonNull final KuzzleResponseListener<KuzzleDocumentList> listener) {
    this.fetchAllDocuments(null, listener);
  }

  /**
   * Retrieves all documents stored in this data collection.
   *
   * @param options  the options
   * @param listener the listener
   */
  public void fetchAllDocuments(final Options options, @NonNull final KuzzleResponseListener<KuzzleDocumentList> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Collection.fetchAllDocuments: listener required");
    }

    this.search(new JSONObject(), options, listener);
  }

  /**
   * Instantiates a CollectionMapping object containing the current mapping of this collection.
   *
   * @param listener the listener
   */
  public void getMapping(@NonNull final KuzzleResponseListener<CollectionMapping> listener) {
    this.getMapping(null, listener);
  }

  /**
   * Instantiates a CollectionMapping object containing the current mapping of this collection.
   *
   * @param options  the options
   * @param listener the listener
   */
  public void getMapping(final Options options, @NonNull final KuzzleResponseListener<CollectionMapping> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Collection.getMapping: listener required");
    }
    new CollectionMapping(this).refresh(options, listener);
  }

  /**
   * Publish a realtime message
   *
   * @param document the document
   * @return kuzzle data collection
   */
  public Collection publishMessage(final Document document) {
    return this.publishMessage(document, null, null);
  }

  /**
   * Publish a realtime message
   *
   * @param document the content
   * @param listener response callback
   * @return the kuzzle data collection
   */
  public Collection publishMessage(@NonNull final Document document, final KuzzleResponseListener<JSONObject> listener) {
    return this.publishMessage(document, null, listener);
  }


  /**
   * Publish a realtime message
   *
   * @param document the content
   * @param options the options
   * @param listener response callback
   * @return the kuzzle data collection
   */
  public Collection publishMessage(@NonNull final Document document, final Options options, final KuzzleResponseListener<JSONObject> listener) {
    if (document == null) {
      throw new IllegalArgumentException("Cannot publish a null document");
    }

    return this.publishMessage(document.getContent(), options, listener);
  }

  /**
   * Publish a realtime message
   *
   * @param document the document
   * @param options  the options
   * @return the kuzzle data collection
   */
  public Collection publishMessage(@NonNull final Document document, final Options options) {
    return this.publishMessage(document, options, null);
  }

  /**
   * Publish a realtime message
   *
   * @param content the content
   * @return the kuzzle data collection
   */
  public Collection publishMessage(@NonNull final JSONObject content) {
    return this.publishMessage(content, null, null);
  }

  /**
   * Publish a realtime message
   *
   * @param content the content
   * @param listener response callback
   * @return the kuzzle data collection
   */
  public Collection publishMessage(@NonNull final JSONObject content, final KuzzleResponseListener<JSONObject> listener) {
    return this.publishMessage(content, null, listener);
  }


  /**
   * Publish a realtime message
   *
   * @param content the content
   * @param options the options
   * @return the kuzzle data collection
   */
  public Collection publishMessage(@NonNull final JSONObject content, final Options options) {
    return this.publishMessage(content, options, null);
  }

  /**
   * Publish a realtime message
   *
   * @param content the content
   * @param options the options
   * @param listener response callback
   * @return the kuzzle data collection
   */
  public Collection publishMessage(@NonNull final JSONObject content, final Options options, final KuzzleResponseListener<JSONObject> listener) {
    if (content == null) {
      throw new IllegalArgumentException("Cannot publish null content");
    }

    try {
      JSONObject data = new JSONObject().put("body", content);
      this.kuzzle.addHeaders(data, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("realtime", "publish"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            listener.onSuccess(response);
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Replace an existing document with a new one.
   *
   * @param documentId the document id
   * @param content    the content
   * @return the kuzzle data collection
   */
  public Collection replaceDocument(@NonNull final String documentId, final JSONObject content) {
    return this.replaceDocument(documentId, content, null, null);
  }

  /**
   * Replace document kuzzle data collection.
   *
   * @param documentId the document id
   * @param content    the content
   * @param listener   the listener
   * @return the kuzzle data collection
   */
  public Collection replaceDocument(@NonNull final String documentId, final JSONObject content, final KuzzleResponseListener<Document> listener) {
    return this.replaceDocument(documentId, content, null, listener);
  }

  /**
   * Replace document kuzzle data collection.
   *
   * @param documentId the document id
   * @param content    the content
   * @param options    the options
   * @return the kuzzle data collection
   */
  public Collection replaceDocument(@NonNull final String documentId, final JSONObject content, final Options options) {
    return this.replaceDocument(documentId, content, options, null);
  }

  /**
   * Replace an existing document with a new one.
   *
   * @param documentId the document id
   * @param content    the content
   * @param options    the options
   * @param listener   the listener
   * @return Collection kuzzle data collection
   */
  public Collection replaceDocument(@NonNull final String documentId, final JSONObject content, final Options options, final KuzzleResponseListener<Document> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("Collection.replaceDocument: documentId required");
    }

    try {
      JSONObject data = new JSONObject().put("_id", documentId).put("body", content);
      this.kuzzle.addHeaders(data, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("document", "createOrReplace"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              JSONObject result = response.getJSONObject("result");
              Document document = new Document(Collection.this, result.getString("_id"), result.getJSONObject("_source"));
              document.setVersion(result.getLong("_version"));
              listener.onSuccess(document);
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Room factory kuzzle room.
   *
   * @return the kuzzle room
   */
  public Room room() {
    return this.room(null);
  }

  /**
   * Room factory kuzzle room.
   *
   * @param options the options
   * @return the kuzzle room
   */
  public Room room(RoomOptions options) {
    return new Room(this, options);
  }

  /**
   * Sets headers.
   *
   * @param content the content
   * @return the headers
   */
  public Collection setHeaders(final JSONObject content) {
    return this.setHeaders(content, false);
  }

  /**
   * Sets headers.
   *
   * @param content the content
   * @param replace the replace
   * @return the headers
   */
  public Collection setHeaders(final JSONObject content, final boolean replace) {
    try {
      if (content == null) {
        if (replace) {
          this.headers = new JSONObject();
        }

        return this;
      }

      if (replace) {
        this.headers = new JSONObject(content.toString());
      } else {
        for (Iterator ite = content.keys(); ite.hasNext(); ) {
          String key = (String) ite.next();
          this.headers.put(key, content.get(key));
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param filters  the filters
   * @param listener the listener
   * @return the kuzzle room
   */
  public KuzzleSubscribeListener subscribe(final JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
    return this.subscribe(filters, null, listener);
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle room
   */
  public KuzzleSubscribeListener subscribe(final RoomOptions options, @NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
    return this.subscribe(null, options, listener);
  }

  /**
   * Subscribes to this data collection with a set of filters.
   * To subscribe to the entire data collection, simply provide an empty filter.
   *
   * @param filters  the filters
   * @param options  the options
   * @param listener the listener
   * @return kuzzle room
   */
  public KuzzleSubscribeListener subscribe(final JSONObject filters, final RoomOptions options, @NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Collection.subscribe: listener required");
    }
    this.kuzzle.isValid();
    final Room room = new Room(this, options);
    final KuzzleSubscribeListener subscribeResponseListener = new KuzzleSubscribeListener();

    room.renew(filters, listener, subscribeResponseListener);

    return subscribeResponseListener;
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   * This method is a lot faster than removing all documents using a query.
   *
   * @return the kuzzle data collection
   */
  public Collection truncate() {
    return this.truncate(null, null);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   * This method is a lot faster than removing all documents using a query.
   *
   * @param options the options
   * @return the kuzzle data collection
   */
  public Collection truncate(final Options options) {
    return this.truncate(options, null);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   * This method is a lot faster than removing all documents using a query.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public Collection truncate(final KuzzleResponseListener<JSONObject> listener) {
    return this.truncate(null, listener);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   * This method is a lot faster than removing all documents using a query.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public Collection truncate(final Options options, final KuzzleResponseListener<JSONObject> listener) {
    JSONObject  data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("collection", "truncate"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              listener.onSuccess(response.getJSONObject("result"));
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @return the kuzzle data collection
   */
  public Collection updateDocument(@NonNull final String documentId, @NonNull final JSONObject content) {
    return this.updateDocument(documentId, content, null, null);
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @param options    the options
   * @return the kuzzle data collection
   */
  public Collection updateDocument(@NonNull final String documentId, @NonNull final JSONObject content, final Options options) {
    return this.updateDocument(documentId, content, options, null);
  }

  /**
   * Update parts of a document
   *
   * @param documentId the document id
   * @param content    the content
   * @param listener   the listener
   * @return the kuzzle data collection
   */
  public Collection updateDocument(@NonNull final String documentId, @NonNull final JSONObject content, final KuzzleResponseListener<Document> listener) {
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
   */
  public Collection updateDocument(@NonNull final String documentId, @NonNull final JSONObject content, final Options options, final KuzzleResponseListener<Document> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("Collection.updateDocument: documentId required");
    }
    if (content == null) {
      throw new IllegalArgumentException("Collection.updateDocument: content required");
    }

    try {
      JSONObject data = new JSONObject().put("_id", documentId).put("body", content);
      this.kuzzle.addHeaders(data, this.getHeaders());

      this.kuzzle.query(makeQueryArgs("document", "update"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              JSONObject result = response.getJSONObject("result");
              Document document = new Document(Collection.this, result.getString("_id"), result.getJSONObject("_source"));
              document.setVersion(result.getLong("_version"));
              document.refresh(listener);
            } catch (JSONException e) {
              throw new RuntimeException(e);
            }
          }
        }

        @Override
        public void onError(JSONObject error) {
          if (listener != null) {
            listener.onError(error);
          }
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
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
   * Getter for the "index" property
   *
   * @return index
   */
  public String getIndex() {
    return this.index;
  }

  /**
   * Gets headers.
   *
   * @return the headers
   */
  public JSONObject getHeaders() {
    return this.headers;
  }
}
