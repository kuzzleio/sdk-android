package io.kuzzle.sdk.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.exceptions.KuzzleException;

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
   * @throws JSONException the json exception
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection, final String id, JSONObject content) throws JSONException {
    this.dataCollection = kuzzleDataCollection;
    this.collection = kuzzleDataCollection.getCollection();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
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
  }

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @throws JSONException the json exception
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection) throws JSONException {
    this(kuzzleDataCollection, null, null);
  }


  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param id                   the id
   * @throws JSONException the json exception
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection, final String id) throws JSONException {
    this(kuzzleDataCollection, id, null);
  }

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param content              the content
   * @throws JSONException the json exception
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection, JSONObject content) throws JSONException {
    this(kuzzleDataCollection, null, content);
  }

  /**
   * Deletes this document in Kuzzle.
   *
   * @return kuzzle document
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument delete() throws JSONException, IOException, KuzzleException {
    if (this.getId() == null)
      return this;
    this.kuzzle.addHeaders(this, this.getJSONObject("headers"));
    this.kuzzle.query(this.collection, "write", "delete", this, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) throws Exception {
        put("_id", null);
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    return this;
  }

  /**
   * Replaces the current content with the last version of this document stored in Kuzzle.
   *
   * @return the kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws IOException     the io exception
   * @throws JSONException   the json exception
   */
  public KuzzleDocument refresh() throws KuzzleException, IOException, JSONException {
    return this.refresh(null);
  }

  /**
   * Replaces the current content with the last version of this document stored in Kuzzle.
   *
   * @param cb the cb
   * @return kuzzle document
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument refresh(final ResponseListener cb) throws JSONException, IOException, KuzzleException {
    if (this.getId() == null) {
      throw new KuzzleException("KuzzleDocument.refresh: cannot retrieve a document if no id has been provided");
    }

    JSONObject content = new JSONObject();
    content.put("_id", this.getId());
    this.kuzzle.query(this.collection, "read", "get", content, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        KuzzleDocument.this.put("body", args.getJSONObject("_source"));
        KuzzleDocument.this.put("_version", args.get("_version"));
        if (cb != null)
          cb.onSuccess(args);
      }

      @Override
      public void onError(JSONObject arg) throws Exception {
        if (cb != null)
          cb.onError(arg);
      }
    });
    return this;
  }

  /**
   * Saves this document into Kuzzle.
   * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
   * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
   *
   * @return the kuzzle document
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument save() throws JSONException, IOException, KuzzleException {
    return save(null, null);
  }

  /**
   * Saves this document into Kuzzle.
   * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
   * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
   *
   * @param options the options
   * @return the kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws IOException     the io exception
   * @throws JSONException   the json exception
   */
  public KuzzleDocument save(KuzzleOptions options) throws KuzzleException, IOException, JSONException {
    return this.save(options, null);
  }

  /**
   * Saves this document into Kuzzle.
   * If this is a new document, this function will create it in Kuzzle and the id property will be made available.
   * Otherwise, this method will replace the latest version of this document in Kuzzle by the current content of this object.
   *
   * @param listener the listener
   * @return the kuzzle document
   * @throws IOException     the io exception
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument save(ResponseListener listener) throws IOException, JSONException, KuzzleException {
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
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument save(final KuzzleOptions options, final ResponseListener listener) throws JSONException, IOException, KuzzleException {
    this.kuzzle.addHeaders(this, getJSONObject("headers"));

    ResponseListener queryCB = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        put("_id", args.getString("_id"));
        put("_version", args.getString("_version"));
        if (listener != null) {
          listener.onSuccess(KuzzleDocument.this);
        }
      }

      @Override
      public void onError(JSONObject args) throws Exception {
        if (listener != null)
          listener.onError(args);
      }
    };
    this.put("persist", true);
    kuzzle.query(this.collection, "write", "createOrUpdate", this, options, queryCB);
    return this;
  }

  /**
   * Sends the content of this document as a realtime message.
   *
   * @param options the options
   * @return kuzzle document
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument publish(KuzzleOptions options) throws JSONException, IOException, KuzzleException {
    put("persist", false);
    kuzzle.query(this.collection, "write", "create", this, options, null);
    return this;
  }

  /**
   * Publish kuzzle document.
   *
   * @return the kuzzle document
   * @throws IOException     the io exception
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument publish() throws IOException, JSONException, KuzzleException {
    return this.publish(null);
  }

  /**
   * Sets content
   *
   * @param data    the data
   * @param replace the replace
   * @return content content
   * @throws JSONException the json exception
   */
  public KuzzleDocument setContent(JSONObject data, boolean replace) throws JSONException {
    if (replace) {
      put("body", data);
    } else {
      for (Iterator iterator = data.keys(); iterator.hasNext(); ) {
        String key = (String) iterator.next();
        getJSONObject("body").put(key, data.get(key));
      }
    }
    return this;
  }

  /**
   * Sets content.
   *
   * @param key   the key
   * @param value the value
   * @return the content
   * @throws JSONException the json exception
   */
  public KuzzleDocument setContent(String key, Object value) throws JSONException {
    getJSONObject("body").put(key, value);
    return this;
  }

  /**
   * Sets content.
   *
   * @param data the data
   * @return the content
   * @throws JSONException the json exception
   */
  public KuzzleDocument setContent(JSONObject data) throws JSONException {
    put("body", data);
    return this;
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @return the kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   */
  public KuzzleDocument subscribe() throws KuzzleException, JSONException, IOException {
    return this.subscribe(null, null);
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param options the options
   * @return the kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   */
  public KuzzleDocument subscribe(KuzzleRoomOptions options) throws KuzzleException, JSONException, IOException {
    return this.subscribe(options, null);
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   */
  public KuzzleDocument subscribe(ResponseListener listener) throws KuzzleException, JSONException, IOException {
    return this.subscribe(null, listener);
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   * @return kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   */
  public KuzzleDocument subscribe(KuzzleRoomOptions options, ResponseListener listener) throws KuzzleException, JSONException, IOException {
    if (this.getId() == null) {
      throw new KuzzleException("KuzzleDocument.subscribe: cannot subscribe to a document if no ID has been provided");
    }
    JSONObject filters = new JSONObject();
    JSONObject values = new JSONObject();
    JSONArray ids = new JSONArray();
    ids.put(this.getId());
    values.put("values", ids);
    filters.put("ids", values);
    this.dataCollection.subscribe(filters, options, listener);
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
   * Gets kuzzle.
   *
   * @return the kuzzle
   */
  public Kuzzle getKuzzle() {
    return kuzzle;
  }

  /**
   * Gets content.
   *
   * @return the content
   * @throws JSONException the json exception
   */
  public JSONObject getContent() throws JSONException {
    if (isNull("body")) {
      this.put("body", new JSONObject());
    }
    return this.getJSONObject("body");
  }

  /**
   * Sets headers.
   *
   * @param headers the headers
   * @return the headers
   * @throws JSONException the json exception
   */
  public KuzzleDocument setHeaders(JSONObject headers) throws JSONException {
    return this.setHeaders(headers, false);
  }

  /**
   * Sets headers.
   *
   * @param headers the headers
   * @param replace the replace
   * @return the headers
   * @throws JSONException the json exception
   */
  public KuzzleDocument setHeaders(JSONObject headers, boolean replace) throws JSONException {
    this.kuzzle.setHeaders(headers, replace);
    return this;
  }

  /**
   * Gets id.
   *
   * @return the id
   * @throws JSONException the json exception
   */
  public String getId() throws JSONException {
    if (!isNull("_id")) {
      return getString("_id");
    }
    return null;
  }

  /**
   * Sets id.
   *
   * @param id the id
   * @return the id
   * @throws JSONException the json exception
   */
  public KuzzleDocument setId(final String id) throws JSONException {
    put("_id", id);
    return this;
  }

  /**
   * Gets version.
   *
   * @return the version
   * @throws JSONException the json exception
   */
  public String getVersion() throws JSONException {
    if (!isNull("_version")) {
      return getString("_version");
    }
    return null;
  }

}
