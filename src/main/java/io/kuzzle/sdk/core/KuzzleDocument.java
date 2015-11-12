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
public class KuzzleDocument {

  private final String collection;
  private final Kuzzle kuzzle;
  private JSONObject content;
  private Date createdTimestamp;
  private JSONObject headers;
  private String id;
  private Date modifiedTimestamp;

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - an instanciated KuzzleDataCollection object
   */
  public KuzzleDocument(KuzzleDataCollection kuzzleDataCollection) {
    this.collection = kuzzleDataCollection.getCollection();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.headers = kuzzleDataCollection.getHeaders();
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
    JSONObject data = new JSONObject();
    if (this.getId() != null)
      data.put("_id", this.getId());
    data.put("body", this.content);
    this.kuzzle.addHeaders(data, this.headers);
    this.kuzzle.query(this.collection, "write", "delete", data);
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
    if (this.getId() == null)
      return this;
    JSONObject data = new JSONObject();
    data.put("_id", this.getId());
    this.kuzzle.query(this.collection, "read", "get", data, new ResponseListener() {

      @Override
      public void onSuccess(JSONObject args) throws Exception {
        KuzzleDocument.this.content = (JSONObject) args;
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
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   * @throws IOException     the io exception
   */
  public KuzzleDocument save(final boolean replace, final ResponseListener listener) throws JSONException, KuzzleException, IOException {
    JSONObject data = new JSONObject();
    if (this.getId() != null)
      data.put("_id", this.getId());
    data.put("body", this.content);
    KuzzleDocument.this.kuzzle.addHeaders(data, this.headers);

    ResponseListener queryCB = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        if (KuzzleDocument.this.getId() == null)
          KuzzleDocument.this.id = ((JSONObject) ((JSONObject) args).get("result")).get("_id").toString();
      }

      @Override
      public void onError(JSONObject args) throws Exception {
        if (listener != null)
          listener.onError(args);
      }
    };
    data.put("persist", true);
    String action;
    if (KuzzleDocument.this.getId() == null || replace)
      action = "createOrUpdate";
    else
      action = "update";
    KuzzleDocument.this.kuzzle.query(KuzzleDocument.this.collection, "write", action, data, queryCB);
    return this;
  }

  /**
   * Save kuzzle document.
   *
   * @param replace the replace
   * @return the kuzzle document
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   * @throws IOException     the io exception
   */
  public KuzzleDocument save(boolean replace) throws KuzzleException, JSONException, IOException {
    return save(replace, null);
  }

  /**
   * Sends the content of this document as a realtime message.
   *
   * @return kuzzle document
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDocument send() throws JSONException, IOException {
    JSONObject data = new JSONObject();
    if (this.getId() != null)
      data.put("_id", this.getId());
    data.put("persist", false);
    KuzzleDocument.this.kuzzle.query(KuzzleDocument.this.collection, "write", "create", data);
    return this;
  }

  /**
   * Listens to events concerning this document. Has no effect if the document does not have an ID
   * (i.e. if the document has not yet been created as a persisted document).
   *
   * @param data    the data
   * @param replace the replace
   * @return content content
   * @throws JSONException the json exception
   */
  public KuzzleDocument setContent(JSONObject data, boolean replace) throws JSONException {
    if (replace)
      KuzzleDocument.this.content = data;
    else {
      for (Iterator iterator = data.keys(); iterator.hasNext(); ) {
        String key = (String) iterator.next();
        if (KuzzleDocument.this.content == null || KuzzleDocument.this.content.isNull(key)) {
          if (KuzzleDocument.this.content == null)
            KuzzleDocument.this.content = new JSONObject();
          KuzzleDocument.this.content.put(key, data.get(key));
        }
      }
    }
    return this;
  }

  /**
   * Listens to events concerning this document. Has no effect if the document does not have an ID
   * (i.e. if the document has not yet been created as a persisted document).
   *
   * @return kuzzle document
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDocument subscribe() throws KuzzleException {
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
   */
  public JSONObject getContent() {
    return content;
  }

  /**
   * Sets content.
   *
   * @param content the content
   */
  public void setContent(JSONObject content) {
    this.content = content;
  }

  /**
   * Gets created timestamp.
   *
   * @return the created timestamp
   */
  public Date getCreatedTimestamp() {
    return createdTimestamp;
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

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Gets modified timestamp.
   *
   * @return the modified timestamp
   */
  public Date getModifiedTimestamp() {
    return modifiedTimestamp;
  }
}
