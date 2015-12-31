package io.kuzzle.sdk.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.kuzzle.sdk.listeners.ResponseListener;

/**
 * The type Kuzzle document.
 */
public class KuzzleDocument extends JSONObject {

  private final KuzzleDataCollection  dataCollection;
  private final String  collection;
  private final Kuzzle  kuzzle;

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - an instanciated KuzzleDataCollection object
   * @param id                   the id
   * @param content              the content
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection, final String id, JSONObject content) {
    this.dataCollection = kuzzleDataCollection;
    this.collection = kuzzleDataCollection.getCollection();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    try {
      this.put("headers", kuzzleDataCollection.getHeaders());
      this.put("body", new JSONObject());
      if (id != null) {
        this.setId(id);
      }
      if (content != null) {
        for (Iterator iterator = content.keys(); iterator.hasNext(); ) {
          String key = (String) iterator.next();
          put(key, content.get(key));
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection) {
    this(kuzzleDataCollection, null, null);
  }


  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param id                   the id
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection, final String id) {
    this(kuzzleDataCollection, id, null);
  }

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param content              the content
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection, JSONObject content) {
    this(kuzzleDataCollection, null, content);
  }

  /**
   * Deletes this document in Kuzzle.
   *
   * @return kuzzle document
   */
  public KuzzleDocument delete() {
    try {
      if (this.getId() == null)
        return this;
      this.kuzzle.addHeaders(this, this.getJSONObject("headers"));
      this.kuzzle.query(this.collection, "write", "delete", this, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject object) {
          try {
            put("_id", null);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onError(JSONObject error) {
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Replaces the current content with the last version of this document stored in Kuzzle.
   *
   * @return the kuzzle document
   */
  public KuzzleDocument refresh() {
    return this.refresh(null);
  }

  /**
   * Replaces the current content with the last version of this document stored in Kuzzle.
   *
   * @param cb the cb
   * @return kuzzle document
   */
  public KuzzleDocument refresh(final ResponseListener cb) {
    if (this.getId() == null) {
      throw new RuntimeException("KuzzleDocument.refresh: cannot retrieve a document if no id has been provided");
    }
    try {
      JSONObject content = new JSONObject();
      content.put("_id", this.getId());
      this.kuzzle.query(this.collection, "read", "get", content, new ResponseListener() {
        @Override
        public void onSuccess(JSONObject args) {
          try {
            KuzzleDocument.this.put("body", args.getJSONObject("_source"));
            KuzzleDocument.this.put("_version", args.get("_version"));
            if (cb != null)
              cb.onSuccess(KuzzleDocument.this);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void onError(JSONObject arg) {
          if (cb != null)
            cb.onError(arg);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Saves this document into Kuzzle.
   * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
   * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
   *
   * @return the kuzzle document
   */
  public KuzzleDocument save() {
    return save(null, null);
  }

  /**
   * Saves this document into Kuzzle.
   * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
   * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
   *
   * @param options the options
   * @return the kuzzle document
   */
  public KuzzleDocument save(KuzzleOptions options) {
    return this.save(options, null);
  }

  /**
   * Saves this document into Kuzzle.
   * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
   * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
   *
   * @param listener the listener
   * @return the kuzzle document
   */
  public KuzzleDocument save(ResponseListener listener) {
    return save(null, listener);
  }

  /**
   * Saves this document into Kuzzle.
   * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
   * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
   *
   * @param options  the options
   * @param listener the listener
   * @return kuzzle document
   */
  public KuzzleDocument save(final KuzzleOptions options, final ResponseListener listener) {
    try {
      this.kuzzle.addHeaders(this, getJSONObject("headers"));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }

    ResponseListener queryCB = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) {
        try {
          put("_id", args.getString("_id"));
          put("_version", args.getString("_version"));
          if (listener != null) {
            listener.onSuccess(KuzzleDocument.this);
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject args) {
        if (listener != null)
          listener.onError(args);
      }
    };
    try {
      this.put("persist", true);
      kuzzle.query(this.collection, "write", "createOrUpdate", this, options, queryCB);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Sends the content of this document as a realtime message.
   *
   * @param options the options
   * @return kuzzle document
   */
  public KuzzleDocument publish(KuzzleOptions options) {
    try {
      kuzzle.query(this.collection, "write", "publish", this, options, null);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Publish kuzzle document.
   *
   * @return the kuzzle document
   */
  public KuzzleDocument publish() {
    return this.publish(null);
  }

  /**
   * Sets content
   *
   * @param data    the data
   * @param replace the replace
   * @return content content
   */
  public KuzzleDocument setContent(JSONObject data, boolean replace) {
    try {
      if (replace) {
        put("body", data);
      } else {
        for (Iterator iterator = data.keys(); iterator.hasNext(); ) {
          String key = (String) iterator.next();
          getJSONObject("body").put(key, data.get(key));
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException();
    }
    return this;
  }

  /**
   * Sets content.
   *
   * @param key   the key
   * @param value the value
   * @return the content
   */
  public KuzzleDocument setContent(String key, Object value) {
    try {
      getJSONObject("body").put(key, value);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Sets content.
   *
   * @param data the data
   * @return the content
   */
  public KuzzleDocument setContent(JSONObject data) {
    try {
      put("body", data);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @return the kuzzle document
   
   */
  public KuzzleDocument subscribe() {
    return this.subscribe(null, null);
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param options the options
   * @return the kuzzle document
   
   */
  public KuzzleDocument subscribe(KuzzleRoomOptions options) {
    return this.subscribe(options, null);
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle document
   
   */
  public KuzzleDocument subscribe(ResponseListener listener) {
    return this.subscribe(null, listener);
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   * @return kuzzle document
   
   */
  public KuzzleDocument subscribe(KuzzleRoomOptions options, ResponseListener listener) {
    if (this.getId() == null) {
      throw new RuntimeException("KuzzleDocument.subscribe: cannot subscribe to a document if no ID has been provided");
    }
    JSONObject filters = new JSONObject();
    JSONObject values = new JSONObject();
    JSONArray ids = new JSONArray();
    ids.put(this.getId());
    try {
      values.put("values", ids);
      filters.put("ids", values);
      this.dataCollection.subscribe(filters, options, listener);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return null;
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
   * Gets content.
   *
   * @return the content
   */
  public JSONObject getContent() {
    try {
      if (isNull("body")) {
        this.put("body", new JSONObject());
      }
      return this.getJSONObject("body");
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Gets content from body
   *
   * @param key the key
   * @return the content
   */
  public Object getContent(String key) {
    try {
      if (!getJSONObject("body").isNull(key)) {
        return this.getJSONObject("body").get(key);
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  /**
   * Helper function allowing to set headers while chaining calls.
   * If the replace argument is set to true, replace the current headers with the provided content.
   * Otherwise, it appends the content to the current headers, only replacing already existing values
   *
   * @param content the headers
   * @return the headers
   */
  public KuzzleDocument setHeaders(JSONObject content) {
    return this.setHeaders(content, false);
  }

  /**
   * Helper function allowing to set headers while chaining calls.
   * If the replace argument is set to true, replace the current headers with the provided content.
   * Otherwise, it appends the content to the current headers, only replacing already existing values
   *
   * @param content - new headers content
   * @param replace - default: false = append the content. If true: replace the current headers with tj
   * @return the headers
   */
  public KuzzleDocument setHeaders(JSONObject content, boolean replace) {
    try {
      if (replace) {
        this.put("headers", content);
      } else {
        JSONObject headers = new JSONObject();
        if (content != null) {
          for (Iterator ite = content.keys(); ite.hasNext(); ) {
            String key = (String) ite.next();
            headers.put(key, content.get(key));
          }
          this.put("headers", headers);
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Gets headers.
   *
   * @return the headers
   */
  public JSONObject getHeaders() {
    try {
      return this.getJSONObject("headers");
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {
    try {
      if (!isNull("_id")) {
        return getString("_id");
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  /**
   * Sets id.
   *
   * @param id the id
   * @return the id
   */
  public KuzzleDocument setId(final String id) {
    try {
      put("_id", id);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Gets version.
   *
   * @return the version
   */
  public String getVersion() {
    try {
      if (!isNull("_version")) {
        return getString("_version");
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

}
