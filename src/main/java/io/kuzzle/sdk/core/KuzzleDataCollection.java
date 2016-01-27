package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleDocumentList;
import io.kuzzle.sdk.responses.KuzzleNotificationResponse;

/**
 * The type Kuzzle data collection.
 */
public class KuzzleDataCollection {

  private final Kuzzle kuzzle;
  private final String collection;
  private final String index;

  /**
   * A data collection is a set of data managed by Kuzzle. It acts like a data table for persistent documents,
   * or like a room for pub/sub messages.
   *
   * @param kuzzle     the kuzzle
   * @param index      the index
   * @param collection the collection
   */
  public KuzzleDataCollection(final Kuzzle kuzzle, @NonNull final String index, @NonNull final String collection) {
    if (index == null || collection == null) {
      throw new IllegalArgumentException("KuzzleDataCollection: index and collection required");
    }
    this.kuzzle = kuzzle;
    this.collection = collection;
    this.index = index;
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
   */
  public KuzzleDataCollection advancedSearch(final JSONObject filter, final KuzzleResponseListener<KuzzleDocumentList> listener) {
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
   */
  public KuzzleDataCollection advancedSearch(final JSONObject filters, final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleDocumentList> listener) {
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

      this.kuzzle.query(makeQueryArgs("read", "search"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            JSONArray hits = object.getJSONObject("result").getJSONArray("hits");
            List<KuzzleDocument> docs = new ArrayList<KuzzleDocument>();
            for (int i = 0; i < hits.length(); i++) {
              KuzzleDocument doc = new KuzzleDocument(KuzzleDataCollection.this, hits.getJSONObject(i));
              docs.add(doc);
            }
            KuzzleDocumentList response = new KuzzleDocumentList(docs, object.getJSONObject("result").getInt("total"));
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
    return this;
  }

  /**
   * Make query args kuzzle . query args.
   *
   * @param action     the action
   * @param controller the controller
   * @param collection the collection
   * @param index      the index
   * @return the kuzzle . query args
   */
  public Kuzzle.QueryArgs makeQueryArgs(final String action, final String controller, final String collection, final String index) {
    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.action = action;
    args.controller = controller;
    args.collection = collection;
    args.index = index;
    return args;
  }

  /**
   * Make query args kuzzle . query args.
   *
   * @param controller the controller
   * @param action     the action
   * @return the kuzzle . query args
   */
  public Kuzzle.QueryArgs makeQueryArgs(final String controller, final String action) {
    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
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
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection count(final JSONObject filters, @NonNull final KuzzleResponseListener<Integer> listener) {
    return this.count(filters, null, listener);
  }

  /**
   * Count kuzzle data collection.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection count(@NonNull final KuzzleResponseListener<Integer> listener) {
    return this.count(null, null, listener);
  }

  /**
   * Returns the number of documents matching the provided set of filters.
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters  the filters
   * @param options  the options
   * @param listener the cb
   * @return KuzzleDataCollection kuzzle data collection
   */
  public KuzzleDataCollection count(final JSONObject filters, final KuzzleOptions options, @NonNull final KuzzleResponseListener<Integer> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.count: listener required");
    }
    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      data.put("body", filters);
      this.kuzzle.query(makeQueryArgs("read", "count"), data, options, new OnQueryDoneListener() {
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
    return this;
  }

  /**
   * Create a new empty data collection, with no associated mapping.
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @param options the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection create(final KuzzleOptions options) {
    return this.create(options, null);
  }

  /**
   * Create a new empty data collection, with no associated mapping.
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection create(final KuzzleResponseListener<JSONObject> listener) {
    return this.create(null, listener);
  }

  /**
   * Create a new empty data collection, with no associated mapping.
   * Kuzzle automatically creates data collections when storing documents, but there are cases where we want to create and prepare data collections before storing documents in it.
   *
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection create() {
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
  public KuzzleDataCollection create(final KuzzleOptions options, final KuzzleResponseListener<JSONObject> listener) {
    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("write", "createCollection"), data, options, new OnQueryDoneListener() {
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
   * Create a new document in Kuzzle
   *
   * @param document the document
   * @return kuzzle data collection
   */
  public KuzzleDataCollection createDocument(final KuzzleDocument document) {
    return this.createDocument(document, null, null);
  }

  /**
   * Create a new document in kuzzle
   *
   * @param document the document
   * @param options  the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection createDocument(final KuzzleDocument document, final KuzzleOptions options) {
    return this.createDocument(document, options, null);
  }

  /**
   * Create document kuzzle data collection.
   *
   * @param document the document
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection createDocument(final KuzzleDocument document, final KuzzleResponseListener<KuzzleDocument> listener) {
    return this.createDocument(document, null ,listener);
  }

  /**
   * Create a new document in kuzzle
   *
   * @param document the document
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection createDocument(final KuzzleDocument document, final KuzzleOptions options, final KuzzleResponseListener<KuzzleDocument> listener) {
    String create = "create";
    if (options != null && options.isUpdateIfExists()) {
      create = "createOrReplace";
    }
    try {
      document.put("persist", true);
      this.kuzzle.query(makeQueryArgs("write", create), document, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              listener.onSuccess(new KuzzleDocument(KuzzleDataCollection.this, response.getJSONObject("result")));
            } catch (JSONException e) {
              throw new RuntimeException();
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
   * @param mapping the mapping
   * @return the kuzzle data mapping
   */
  public KuzzleDataMapping dataMappingFactory(JSONObject mapping) {
    return new KuzzleDataMapping(this, mapping);
  }

  /**
   * Delete kuzzle data collection.
   *
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection delete() {
    return this.delete(null, null);
  }

  /**
   * Delete kuzzle data collection.
   *
   * @param options the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection delete(final KuzzleOptions options) {
    return this.delete(options, null);
  }

  /**
   * Delete kuzzle data collection.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection delete(final KuzzleResponseListener<String> listener) {
    return this.delete(null, listener);
  }

  /**
   * Delete kuzzle data collection.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection delete(final KuzzleOptions options, final KuzzleResponseListener<String> listener) {
    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("admin", "deleteCollection"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              listener.onSuccess(response.getJSONObject("result").getString("_id"));
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
    }  catch (JSONException e) {
      throw new RuntimeException(e);
    }
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
   */
  public KuzzleDataCollection deleteDocument(@NonNull final String documentId) {
    return this.deleteDocument(documentId, null, null);
  }

  /**
   * Delete document kuzzle data collection.
   *
   * @param documentId the document id
   * @param options    the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection deleteDocument(@NonNull final String documentId, KuzzleOptions options) {
    return this.deleteDocument(documentId, options, null);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param documentId the document id
   * @param listener   the listener
   * @return KuzzleDataCollection kuzzle data collection
   */
  public KuzzleDataCollection deleteDocument(@NonNull final String documentId, final KuzzleResponseListener<String> listener) {
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
  public KuzzleDataCollection deleteDocument(@NonNull final String documentId, final KuzzleOptions options, final KuzzleResponseListener<String> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.deleteDocument: documentId required");
    }
    return this.deleteDocument(documentId, null, options, listener, null);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters the filters
   * @return KuzzleDataCollection kuzzle data collection
   */
  public KuzzleDataCollection deleteDocument(@NonNull final JSONObject filters) {
    return this.deleteDocument(filters, null, null);
  }

  /**
   * Delete document kuzzle data collection.
   *
   * @param filters the filters
   * @param options the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection deleteDocument(@NonNull final JSONObject filters, final KuzzleOptions options) {
    return this.deleteDocument(filters, options, null);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param filters  the filters
   * @param listener the listener
   * @return KuzzleDataCollection kuzzle data collection
   */
  public KuzzleDataCollection deleteDocument(@NonNull final JSONObject filters, final KuzzleResponseListener<String[]> listener) {
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
  public KuzzleDataCollection deleteDocument(@NonNull final JSONObject filters, final KuzzleOptions options, final KuzzleResponseListener<String[]> listener) {
    if (filters == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.deleteDocument: filters required");
    }
    return this.deleteDocument(null, filters, options, null, listener);
  }

  /**
   * Delete a persistent document.
   * There is a small delay between documents creation and their existence in our advanced search layer,
   * usually a couple of seconds.
   * That means that a document that was just been created won’t be returned by this function
   *
   * @param documentId the document id
   * @param filter     the filter
   * @return KuzzleDataCollection kuzzle data collection
   */
  private KuzzleDataCollection deleteDocument(final String documentId, final JSONObject filter, final KuzzleOptions options, final KuzzleResponseListener<String> listener, final KuzzleResponseListener<String[]> listener2) {
    JSONObject data = new JSONObject();
    String action = "delete";
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      if (documentId != null) {
        data.put("_id", documentId);
      }
      if (filter != null) {
        data.put("body", filter);
        action = "deleteByQuery";
      }
      this.kuzzle.query(makeQueryArgs("write", action), data, options, new OnQueryDoneListener() {
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

  public KuzzleDocument documentFactory() {
    return new KuzzleDocument(this);
  }

  public KuzzleDocument documentFactory(final String id) {
    return new KuzzleDocument(this, id);
  }

  public KuzzleDocument documentFactory(final JSONObject content) {
    return new KuzzleDocument(this, content);
  }

  public KuzzleDocument documentFactory(final String id, final JSONObject content) {
    return new KuzzleDocument(this, id, content);
  }

  /**
   * Fetch document kuzzle data collection.
   *
   * @param documentId the document id
   * @param listener   the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection fetchDocument(@NonNull final String documentId, @NonNull final KuzzleResponseListener<KuzzleDocument> listener) {
    return this.fetchDocument(documentId, null, listener);
  }

  /**
   * Retrieve a single stored document using its unique document ID.
   *
   * @param documentId the document id
   * @param options    the options
   * @param listener   the listener
   * @return KuzzleDataCollection kuzzle data collection
   */
  public KuzzleDataCollection fetchDocument(@NonNull final String documentId, final KuzzleOptions options, final KuzzleResponseListener<KuzzleDocument> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.fetchDocument: documentId required");
    }
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.fetchDocument: listener required");
    }
    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.getHeaders());
      data.put("_id", documentId);
      this.kuzzle.query(makeQueryArgs("read", "get"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(new KuzzleDocument(KuzzleDataCollection.this, response.getJSONObject("result")));
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
    return this;
  }

  /**
   * Retrieves all documents stored in this data collection.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection fetchAllDocuments(@NonNull final KuzzleResponseListener<KuzzleDocumentList> listener) {
    return this.fetchAllDocuments(null, listener);
  }

  /**
   * Retrieves all documents stored in this data collection.
   *
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection fetchAllDocuments(final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleDocumentList> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.fetchAllDocuments: listener required");
    }
    return this.advancedSearch(null, options, new KuzzleResponseListener<KuzzleDocumentList>() {
      @Override
      public void onSuccess(KuzzleDocumentList response) {
        listener.onSuccess(response);
      }

      @Override
      public void onError(JSONObject error) {
        listener.onError(error);
      }
    });
  }

  /**
   * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
   *
   * @param listener the listener
   * @return the mapping
   */
  public KuzzleDataCollection getMapping(@NonNull final KuzzleResponseListener<KuzzleDataMapping> listener) {
    return this.getMapping(null, listener);
  }

  /**
   * Instantiates a KuzzleDataMapping object containing the current mapping of this collection.
   *
   * @param options  the options
   * @param listener the listener
   * @return the mapping
   */
  public KuzzleDataCollection getMapping(final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleDataMapping> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.getMapping: listener required");
    }
    new KuzzleDataMapping(this).refresh(options, listener);
    return this;
  }

  /**
   * Publish a realtime message
   *
   * @param document the document
   * @return kuzzle data collection
   */
  public KuzzleDataCollection publishMessage(final KuzzleDocument document) {
    return this.publishMessage(document, null);
  }

  /**
   * Publish a realtime message
   *
   * @param document the document
   * @param options  the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection publishMessage(@NonNull final KuzzleDocument document, final KuzzleOptions options) {
    try {
      this.kuzzle.addHeaders(document, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("write", "publish"), document, options, null);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping the mapping
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection updateMapping(final KuzzleDataMapping mapping) {
    return this.updateMapping(mapping, null, null);
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping the mapping
   * @param options the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection updateMapping(final KuzzleDataMapping mapping, KuzzleOptions options) {
    return this.updateMapping(mapping, options, null);
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping  the mapping
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection updateMapping(final KuzzleDataMapping mapping, final KuzzleResponseListener<KuzzleDataMapping> listener) {
    return this.updateMapping(mapping, null, listener);
  }

  /**
   * Put mapping kuzzle data collection.
   *
   * @param mapping  the mapping
   * @param options  the options
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection updateMapping(final KuzzleDataMapping mapping, final KuzzleOptions options, final KuzzleResponseListener<KuzzleDataMapping> listener) {
    mapping.apply(options, listener);
    return this;
  }

  /**
   * Replace an existing document with a new one.
   *
   * @param documentId the document id
   * @param content    the content
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection replaceDocument(@NonNull final String documentId, final KuzzleDocument content) {
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
  public KuzzleDataCollection replaceDocument(@NonNull final String documentId, final KuzzleDocument content, final KuzzleResponseListener listener) {
    return this.replaceDocument(documentId, content, null, listener);
  }

  /**
   * Replace document kuzzle data collection.
   *
   * @param documentId the document id
   * @param options    the options
   * @param content    the content
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection replaceDocument(@NonNull final String documentId, final KuzzleOptions options, final KuzzleDocument content) {
    return this.replaceDocument(documentId, content, options, null);
  }

  /**
   * Replace an existing document with a new one.
   *
   * @param documentId the document id
   * @param content    the content
   * @param options    the options
   * @param listener   the listener
   * @return KuzzleDataCollection kuzzle data collection
   */
  public KuzzleDataCollection replaceDocument(@NonNull final String documentId, final KuzzleDocument content, final KuzzleOptions options, final KuzzleResponseListener<KuzzleDocument> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.replaceDocument: documentId required");
    }
    content.setId(documentId);
    try {
      this.kuzzle.addHeaders(content, this.getHeaders());
      this.kuzzle.query(makeQueryArgs("write", "createOrReplace"), content, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              listener.onSuccess(new KuzzleDocument(KuzzleDataCollection.this, response.getJSONObject("result")));
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

  public KuzzleRoom roomFactory() {
    return this.roomFactory(null);
  }

  public KuzzleRoom roomFactory(KuzzleRoomOptions options) {
    return new KuzzleRoom(this, options);
  }

  /**
   * Sets headers.
   *
   * @param content the content
   * @return the headers
   */
  public KuzzleDataCollection setHeaders(final JSONObject content) {
    return this.setHeaders(content, false);
  }

  /**
   * Sets headers.
   *
   * @param content the content
   * @param replace the replace
   * @return the headers
   */
  public KuzzleDataCollection setHeaders(final JSONObject content, final boolean replace) {
    this.kuzzle.setHeaders(content, replace);
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
  public KuzzleRoom subscribe(final JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
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
  public KuzzleRoom subscribe(final KuzzleRoomOptions options, @NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
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
  public KuzzleRoom subscribe(final JSONObject filters, final KuzzleRoomOptions options, @NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.subscribe: listener required");
    }
    this.kuzzle.isValid();
    KuzzleRoom room = new KuzzleRoom(this, options);
    return room.renew(filters, listener);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   * This method is a lot faster than removing all documents using a query.
   *
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection truncate() {
    return this.truncate(null, null);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   * This method is a lot faster than removing all documents using a query.
   *
   * @param options the options
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection truncate(final KuzzleOptions options) {
    return this.truncate(options, null);
  }

  /**
   * Truncate the data collection, removing all stored documents but keeping all associated mappings.
   * This method is a lot faster than removing all documents using a query.
   *
   * @param listener the listener
   * @return the kuzzle data collection
   */
  public KuzzleDataCollection truncate(final KuzzleResponseListener<JSONObject> listener) {
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
  public KuzzleDataCollection truncate(final KuzzleOptions options, final KuzzleResponseListener<JSONObject> listener) {
    JSONObject  data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.kuzzle.getHeaders());
      this.kuzzle.query(makeQueryArgs("admin", "truncateCollection"), data, options, new OnQueryDoneListener() {
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
  public KuzzleDataCollection updateDocument(@NonNull final String documentId, @NonNull final KuzzleDocument content) {
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
  public KuzzleDataCollection updateDocument(@NonNull final String documentId, @NonNull final KuzzleDocument content, final KuzzleOptions options) {
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
  public KuzzleDataCollection updateDocument(@NonNull final String documentId, @NonNull final KuzzleDocument content, final KuzzleResponseListener<KuzzleDocument> listener) {
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
  public KuzzleDataCollection updateDocument(@NonNull final String documentId, @NonNull final KuzzleDocument content, final KuzzleOptions options, final KuzzleResponseListener<KuzzleDocument> listener) {
    if (documentId == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.updateDocument: documentId required");
    }
    if (content == null) {
      throw new IllegalArgumentException("KuzzleDataCollection.updateDocument: content required");
    }
    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.kuzzle.getHeaders());
      data.put("_id", documentId);
      data.put("body", content.getContent());
      this.kuzzle.query(makeQueryArgs("write", "update"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            try {
              listener.onSuccess(new KuzzleDocument(KuzzleDataCollection.this, response.getString("_id"), response.getJSONObject("result")));
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
   * Gets headers.
   *
   * @return the headers
   */
  public JSONObject getHeaders() {
    return kuzzle.getHeaders();
  }

}
