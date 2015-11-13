package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.exceptions.KuzzleException;

/**
 * The type Kuzzle document.
 */
public class KuzzleDocument extends JSONObject {

  private final String  collection;
  private final Kuzzle  kuzzle;

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - an instanciated KuzzleDataCollection object
   * @param content              the content
   * @throws JSONException the json exception
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection, JSONObject content) throws JSONException {
    this.collection = kuzzleDataCollection.getCollection();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.put("headers", kuzzleDataCollection.getHeaders());
    this.put("body", new JSONObject());
    if (content != null) {
      for (Iterator iterator = content.keys(); iterator.hasNext(); ) {
        String key = (String) iterator.next();
        put(key, content.get(key));
      }
    }
  }

  /**
   * Instantiates a new Kuzzle document.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @throws JSONException the json exception
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection) throws JSONException {
    this(kuzzleDataCollection, null);
  }

  /**
   * Deletes this document in Kuzzle.
   *
   * @return kuzzle document
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDocument delete() throws JSONException, IOException {
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
   * @param cb the cb
   * @return kuzzle document
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDocument refresh(final ResponseListener cb) throws JSONException, IOException {
    this.kuzzle.query(this.collection, "read", "get", this, new ResponseListener() {

      @Override
      public void onSuccess(JSONObject args) throws Exception {
        KuzzleDocument.this.put("body", args);
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
   * If this is a new document, this function will create it in Kuzzle. Otherwise, you can specify whether you want
   * to merge this document with the one stored in Kuzzle, or if you want to replace it.
   *
   * @param replace  the replace
   * @param listener the listener
   * @return kuzzle document
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDocument save(final boolean replace, final ResponseListener listener) throws JSONException, IOException {
    this.kuzzle.addHeaders(this, getJSONObject("headers"));

    ResponseListener queryCB = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        if (KuzzleDocument.this.isNull("_id"))
          put("_id", args.getString("_id"));
        if (listener != null)
          listener.onSuccess(args);
      }

      @Override
      public void onError(JSONObject args) throws Exception {
        if (listener != null)
          listener.onError(args);
      }
    };
    this.put("persist", true);
    String action;
    if (isNull("_id") || replace)
      action = "createOrUpdate";
    else
      action = "update";
    kuzzle.query(this.collection, "write", action, this, queryCB);
    return this;
  }

  /**
   * Save kuzzle document.
   *
   * @param replace the replace
   * @return the kuzzle document
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDocument save(boolean replace) throws JSONException, IOException {
    return save(replace, null);
  }

  /**
   * Save kuzzle document.
   *
   * @return the kuzzle document
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDocument save() throws JSONException, IOException {
    return save(false, null);
  }

  /**
   * Save kuzzle document.
   *
   * @param listener the listener
   * @return the kuzzle document
   * @throws IOException   the io exception
   * @throws JSONException the json exception
   */
  public KuzzleDocument save(ResponseListener listener) throws IOException, JSONException {
    return save(false, listener);
  }

  /**
   * Sends the content of this document as a realtime message.
   *
   * @return kuzzle document
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDocument send() throws JSONException, IOException {
    put("persist", false);
    kuzzle.query(this.collection, "write", "create", this);
    return this;
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
   * Listens to events concerning this document. Has no effect if the document does not have an ID
   * (i.e. if the document has not yet been created as a persisted document).
   *
   * @return kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDocument subscribe() throws KuzzleException, JSONException {
    if (this.getId() == null)
      throw new KuzzleException("Cannot subscribe to a document that has not been created into Kuzzle");
    // TODO: implement this function once KuzzleRoom has been implemented
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
    return this.getJSONObject("body");
  }

  /**
   * Gets headers.
   *
   * @return the headers
   * @throws JSONException the json exception
   */
  public JSONObject getHeaders() throws JSONException {
    return this.getJSONObject("headers");
  }

  /**
   * Sets headers.
   *
   * @param headers the headers
   * @throws JSONException the json exception
   */
  public void setHeaders(JSONObject headers) throws JSONException {
    this.put("headers", headers);
  }

  /**
   * Gets id.
   *
   * @return the id
   * @throws JSONException the json exception
   */
  public String getId() throws JSONException {
    return getString("_id");
  }

  /**
   * Sets id.
   *
   * @param id the id
   * @throws JSONException the json exception
   */
  public void setId(final String id) throws JSONException {
    put("_id", id);
  }

}
