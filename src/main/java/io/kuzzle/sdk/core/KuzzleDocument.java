package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleNotificationResponse;

/**
 * The type Kuzzle document.
 */
public class KuzzleDocument {
  private final KuzzleDataCollection dataCollection;
  private final String collection;
  private final Kuzzle kuzzle;
  private JSONObject headers;

  private String id;
  private JSONObject content;
  private long version = -1;

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - an instanciated KuzzleDataCollection object
   * @param id                   the id
   * @param content              the content
   */
  public KuzzleDocument(@NonNull final KuzzleDataCollection kuzzleDataCollection, final String id, final JSONObject content) throws JSONException {
    if (kuzzleDataCollection == null) {
      throw new IllegalArgumentException("KuzzleDocument: KuzzleDataCollection argument missing");
    }

    this.dataCollection = kuzzleDataCollection;
    this.collection = kuzzleDataCollection.getCollection();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.setId(id);
    this.setContent(content, true);
    this.headers = kuzzleDataCollection.getHeaders();
  }

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public KuzzleDocument(final KuzzleDataCollection kuzzleDataCollection) throws JSONException {
    this(kuzzleDataCollection, null, null);
  }


  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param id                   the id
   */
  public KuzzleDocument(final KuzzleDataCollection kuzzleDataCollection, final String id) throws JSONException {
    this(kuzzleDataCollection, id, null);
  }

  /**
   * Kuzzle handles documents either as realtime messages or as stored documents.
   * KuzzleDocument is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param content              the content
   */
  public KuzzleDocument(final KuzzleDataCollection kuzzleDataCollection, final JSONObject content) throws JSONException {
    this(kuzzleDataCollection, null, content);
  }

  /**
   * Delete kuzzle document.
   *
   * @param options the options
   * @return the kuzzle document
   */
  public KuzzleDocument delete(final KuzzleOptions options) {
    return this.delete(options, null);
  }

  /**
   * Delete kuzzle document.
   *
   * @param listener the listener
   * @return the kuzzle document
   */
  public KuzzleDocument delete(final KuzzleResponseListener<KuzzleDocument> listener) {
    return this.delete(null, listener);
  }

  /**
   * Delete kuzzle document.
   *
   * @return the kuzzle document
   */
  public KuzzleDocument delete() {
    return this.delete(null, null);
  }

  /**
   * Deletes this document in Kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   * @return kuzzle document
   */
  public KuzzleDocument delete(final KuzzleOptions options, final KuzzleResponseListener<KuzzleDocument> listener) {
    try {
      if (this.id == null) {
        throw new IllegalStateException("KuzzleDocument.delete: cannot delete a document without a document ID");
      }

      this.kuzzle.query(this.dataCollection.makeQueryArgs("write", "delete"), this.serialize(), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          setId(null);
          if (listener != null) {
            listener.onSuccess(KuzzleDocument.this);
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
   * Gets a refreshed copy of the current object
   *
   * @param listener the listener
   */
  public void refresh(final KuzzleResponseListener<KuzzleDocument> listener) {
    this.refresh(null, listener);
  }

  /**
   * Gets a refreshed copy of the current object
   *
   * @param options  the options
   * @param listener the listener
   */
  public void refresh(final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleDocument> listener) {
    if (this.id == null) {
      throw new IllegalStateException("KuzzleDocument.refresh: cannot retrieve a document if no id has been provided");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleDocument.refresh: a valid KuzzleResponseListener object is required");
    }

    try {
      JSONObject content = new JSONObject();
      content.put("_id", this.getId());

      this.kuzzle.query(this.dataCollection.makeQueryArgs("read", "get"), content, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject args) {
          try {
            JSONObject result = args.getJSONObject("result");
            KuzzleDocument newDocument = new KuzzleDocument(
              KuzzleDocument.this.dataCollection,
              result.getString("_id"),
              result.getJSONObject("_source")
            );

            newDocument.setVersion(result.getLong("_version"));
            listener.onSuccess(newDocument);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject arg) {
          listener.onError(arg);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
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
  public KuzzleDocument save(final KuzzleOptions options) {
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
  public KuzzleDocument save(final KuzzleResponseListener<KuzzleDocument> listener) {
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
  public KuzzleDocument save(final KuzzleOptions options, final KuzzleResponseListener<KuzzleDocument> listener) {
    try {
      kuzzle.query(this.dataCollection.makeQueryArgs("write", "createOrReplace"), this.serialize(), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            KuzzleDocument.this.setId(result.getString("_id"));
            KuzzleDocument.this.setVersion(result.getLong("_version"));

            if (listener != null) {
              listener.onSuccess(KuzzleDocument.this);
            }
          } catch (JSONException e) {
            throw new RuntimeException(e);
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
   * Sends the content of this document as a realtime message.
   *
   * @param options the options
   * @return kuzzle document
   */
  public KuzzleDocument publish(final KuzzleOptions options) {
    try {
      kuzzle.query(this.dataCollection.makeQueryArgs("write", "publish"), this.serialize(), options, null);
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
   * @param content    the data
   * @param replace the replace
   * @return content content
   */
  public KuzzleDocument setContent(final JSONObject content, final boolean replace) throws JSONException {
    if (replace) {
      if (content != null) {
        this.content = new JSONObject(content.toString());
      }
      else {
        this.content = new JSONObject();
      }
    } else if (content != null) {
      for (Iterator iterator = content.keys(); iterator.hasNext(); ) {
        String key = (String) iterator.next();
        this.content.put(key, content.get(key));
      }
    }

    if (this.content.has("version")) {
      this.version = this.content.getLong("version");
      this.content.remove("version");
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
  public KuzzleDocument setContent(@NonNull final String key, final Object value) throws JSONException {
    if (key == null) {
      throw new IllegalArgumentException("KuzzleDocument.setContent: key required");
    }

    this.content.put(key, value);
    return this;
  }

  /**
   * Sets content.
   *
   * @param data the data
   * @return the content
   */
  public KuzzleDocument setContent(final JSONObject data) throws JSONException {
    this.setContent(data, false);
    return this;
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle document
   */
  public KuzzleDocument subscribe(@NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
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
  public KuzzleDocument subscribe(final KuzzleRoomOptions options,  @NonNull final KuzzleResponseListener<KuzzleNotificationResponse> listener) {
    if (this.id == null) {
      throw new IllegalStateException("KuzzleDocument.subscribe: cannot subscribe to a document if no ID has been provided");
    }

    try {
      JSONObject filters = new JSONObject("{" +
        "\"ids\": {" +
          "\"values\": [\"" + this.id + "\"]" +
        "}" +
      "}");

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
    return this.content;
  }


  /**
   * Gets content from body
   *
   * @param key the key
   * @return the content
   */
  public Object getContent(final String key) throws JSONException {
    if (this.content.has(key)) {
      return this.content.get(key);
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
  public KuzzleDocument setHeaders(final JSONObject content) {
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
  public KuzzleDocument setHeaders(final JSONObject content, final boolean replace) {
    try {
      if (content == null) {
        if (replace) {
          this.content = new JSONObject();
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
   * Gets headers.
   *
   * @return the headers
   */
  public JSONObject getHeaders() {
    return this.headers;
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {
    return this.id;
  }

  /**
   * Sets id.
   *
   * @param id the id
   * @return the id
   */
  public KuzzleDocument setId(final String id) {
    this.id = id;
    return this;
  }

  /**
   * Gets version.
   *
   * @return the version
   */
  public long getVersion() {
    return this.version;
  }

  /**
   * Serializes this object into a plain old JSON object
   *
   * @return JSON object representing this document
   */
  public JSONObject serialize() {
    JSONObject data = new JSONObject();

    try {
      if (this.id != null) {
        data.put("_id", this.getId());
      }

      if (this.version != -1) {
        data.put("_version", this.version);
      }

      data.put("body", this.getContent());
      this.kuzzle.addHeaders(data, getHeaders());
    }
    catch (JSONException e) {
      e.printStackTrace();
    }

    return data;
  }

  public String toString() {
    return this.serialize().toString();
  }

  public void setVersion(long version) {
    if (version > 0) {
      this.version = version;
    }
  }
}
