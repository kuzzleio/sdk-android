package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.SubscribeListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.NotificationResponse;

/**
 * The type Kuzzle document.
 */
public class Document {
  private final Collection dataCollection;
  private final String collection;
  private final Kuzzle kuzzle;
  private JSONObject headers;

  private String id;
  private JSONObject content;
  private JSONObject meta;
  private long version = -1;

  /**
   * Kuzzle handles documents either as real-time messages or as stored documents.
   * Document is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - An instantiated Collection object
   * @param id                   - Unique document identifier
   * @param content              - The content of the document
   * @param meta                 - Document metadata
   * @throws JSONException the json exception
   */
  public Document(@NonNull final Collection kuzzleDataCollection, final String id, final JSONObject content, final JSONObject meta) throws JSONException {
    if (kuzzleDataCollection == null) {
      throw new IllegalArgumentException("Document: Collection argument missing");
    }

    this.dataCollection = kuzzleDataCollection;
    this.collection = kuzzleDataCollection.getCollection();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.setId(id);
    this.setContent(content, true);
    this.setMeta(meta, true);
    this.headers = kuzzleDataCollection.getHeaders();
  }

  /**
   * Kuzzle handles documents either as real-time messages or as stored documents.
   * Document is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - An instantiated Collection object
   * @param id                   - Unique document identifier
   * @param content              - The content of the document
   * @throws JSONException the json exception
   */
  public Document(@NonNull final Collection kuzzleDataCollection, final String id, final JSONObject content) throws JSONException {
    this(kuzzleDataCollection, id, content, null);
  }

  /**
   * Kuzzle handles documents either as real-time messages or as stored documents.
   * Document is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - An instantiated Collection object
   * @throws JSONException the json exception
   */
  public Document(final Collection kuzzleDataCollection) throws JSONException {
    this(kuzzleDataCollection, null, null, null);
  }


  /**
   * Kuzzle handles documents either as real-time messages or as stored documents.
   * Document is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - An instantiated Collection object
   * @param id                   - Unique document identifier
   * @throws JSONException the json exception
   */
  public Document(final Collection kuzzleDataCollection, final String id) throws JSONException {
    this(kuzzleDataCollection, id, null, null);
  }

  /**
   * Kuzzle handles documents either as real-time messages or as stored documents.
   * Document is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - An instantiated Collection object
   * @param content              - The content of the document
   * @throws JSONException the json exception
   */
  public Document(final Collection kuzzleDataCollection, final JSONObject content) throws JSONException {
    this(kuzzleDataCollection, null, content, null);
  }

  /**
   * Kuzzle handles documents either as real-time messages or as stored documents.
   * Document is the object representation of one of these documents.
   *
   * @param kuzzleDataCollection - An instantiated Collection object
   * @param content              - The content of the document
   * @param meta                 - Document metadata
   * @throws JSONException the json exception
   */
  public Document(final Collection kuzzleDataCollection, final JSONObject content, final JSONObject meta) throws JSONException {
    this(kuzzleDataCollection, null, content, meta);
  }

  /**
   * Delete kuzzle document.
   *
   * @param options the options
   */
  public void delete(final Options options) {
    this.delete(options, null);
  }

  /**
   * Delete kuzzle document.
   *
   * @param listener the listener
   */
  public void delete(final ResponseListener<String> listener) {
    this.delete(null, listener);
  }

  /**
   * Delete kuzzle document.
   */
  public void delete() {
    this.delete(null, null);
  }

  /**
   * Deletes this document in Kuzzle.
   *
   * @param options  the options
   * @param listener the listener
   */
  public void delete(final Options options, final ResponseListener<String> listener) {
    try {
      if (this.id == null) {
        throw new IllegalStateException("Document.delete: cannot delete a document without a document ID");
      }

      this.kuzzle.query(this.dataCollection.makeQueryArgs("document", "delete"), this.serialize(), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          setId(null);
          if (listener != null) {
            try {
              listener.onSuccess(object.getString("result"));
            } catch (JSONException e) {
              e.printStackTrace();
            };
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
  }

  public void exists(@NonNull final ResponseListener<JSONObject> listener) {
    this.exists(null, listener);
  }

  public void exists(final Options options, @NonNull final ResponseListener<JSONObject> listener) {
    if (this.id == null) {
      throw new IllegalStateException("Document.exists: cannot check if the document exists if no id has been provided");
    }

    if (listener == null) {
      throw new IllegalArgumentException("Document.exists: a valid ResponseListener object is required");
    }

    try {
      this.kuzzle.query(this.dataCollection.makeQueryArgs("document", "exists"), this.serialize(), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject object) {
          listener.onSuccess(object);
          ;
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
   * Gets a refreshed copy of the current object
   *
   * @param listener the listener
   */
  public void refresh(@NonNull final ResponseListener<Document> listener) {
    this.refresh(null, listener);
  }

  /**
   * Gets a refreshed copy of the current object
   *
   * @param options  the options
   * @param listener the listener
   */
  public void refresh(final Options options, @NonNull final ResponseListener<Document> listener) {
    if (this.id == null) {
      throw new IllegalStateException("Document.refresh: cannot retrieve a document if no id has been provided");
    }

    if (listener == null) {
      throw new IllegalArgumentException("Document.refresh: a valid ResponseListener object is required");
    }

    try {
      JSONObject content = new JSONObject();
      content.put("_id", this.getId());

      this.kuzzle.query(this.dataCollection.makeQueryArgs("document", "get"), content, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject args) {
          try {
            JSONObject result = args.getJSONObject("result");
            Document newDocument = new Document(
              Document.this.dataCollection,
              result.getString("_id"),
              result.getJSONObject("_source"),
              result.getJSONObject("_meta")
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
  public Document save() {
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
  public Document save(final Options options) {
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
  public Document save(final ResponseListener<Document> listener) {
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
  public Document save(final Options options, final ResponseListener<Document> listener) {
    try {
      kuzzle.query(this.dataCollection.makeQueryArgs("document", "createOrReplace"), this.serialize(), options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            Document.this.setId(result.getString("_id"));
            Document.this.setVersion(result.getLong("_version"));

            if (listener != null) {
              listener.onSuccess(Document.this);
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
  public Document publish(final Options options) {
    try {
      kuzzle.query(this.dataCollection.makeQueryArgs("realtime", "publish"), this.serialize(), options, null);
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
  public Document publish() {
    return this.publish(null);
  }

  /**
   * Sets content
   *
   * @param content the data
   * @param replace the replace
   * @return content content
   * @throws JSONException the json exception
   */
  public Document setContent(final JSONObject content, final boolean replace) throws JSONException {
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
   * @throws JSONException the json exception
   */
  public Document setContent(@NonNull final String key, final Object value) throws JSONException {
    if (key == null) {
      throw new IllegalArgumentException("Document.setContent: key required");
    }

    this.content.put(key, value);
    return this;
  }

  /**
   * Sets content.
   *
   * @param data the data
   * @return the content
   * @throws JSONException the json exception
   */
  public Document setContent(final JSONObject data) throws JSONException {
    this.setContent(data, false);
    return this;
  }

  /**
   * Sets metadata
   *
   * @param meta the metadata
   * @param replace if true: replaces the current metadata with the provided ones, otherwise merges it
   * @return the Kuzzle document
   * @throws JSONException the json exception
   */
  public Document setMeta(final JSONObject meta, final boolean replace) throws JSONException {
    if (replace) {
      if (meta != null) {
        this.meta = new JSONObject(meta.toString());
      }
      else {
        this.meta = new JSONObject();
      }
    } else if (meta != null) {
      for (Iterator iterator = meta.keys(); iterator.hasNext(); ) {
        String key = (String) iterator.next();
        this.meta.put(key, meta.get(key));
      }
    }

    return this;
  }

  /**
   * Sets metadata.
   *
   * @param key   the key
   * @param value the value
   * @return the Kuzzle document
   * @throws JSONException the json exception
   */
  public Document setMeta(@NonNull final String key, final Object value) throws JSONException {
    if (key == null) {
      throw new IllegalArgumentException("Document.setMeta: key required");
    }

    this.meta.put(key, value);

    return this;
  }

  /**
   * Sets metadata.
   *
   * @param meta the metadata
   * @return the Kuzzle document
   * @throws JSONException the json exception
   */
  public Document setMeta(final JSONObject meta) throws JSONException {
    this.setMeta(meta, false);
    
    return this;
  }

  /**
   * Listens to changes occuring on this document.
   * Throws an error if this document has not yet been created in Kuzzle.
   *
   * @param listener the listener
   * @return the kuzzle document
   */
  public SubscribeListener subscribe(@NonNull final ResponseListener<NotificationResponse> listener) {
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
  public SubscribeListener subscribe(final RoomOptions options, @NonNull final ResponseListener<NotificationResponse> listener) {
    if (this.id == null) {
      throw new IllegalStateException("Document.subscribe: cannot subscribe to a document if no ID has been provided");
    }

    SubscribeListener returnValue;

    try {
      JSONObject filters = new JSONObject("{" +
        "\"ids\": {" +
          "\"values\": [\"" + this.id + "\"]" +
        "}" +
      "}");
      returnValue = this.dataCollection.subscribe(filters, options, listener);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return returnValue;
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
   * @throws JSONException the json exception
   */
  public Object getContent(final String key) throws JSONException {
    if (this.content.has(key)) {
      return this.content.get(key);
    }

    return null;
  }

  /**
   * Gets metadata.
   *
   * @return the metadata
   */
  public JSONObject getMeta() {
    return this.meta;
  }


  /**
   * Gets metadata by key.
   *
   * @param key the key
   * @return the metadata
   * @throws JSONException the json exception
   */
  public Object getMeta(final String key) throws JSONException {
    if (this.meta.has(key)) {
      return this.meta.get(key);
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
  public Document setHeaders(final JSONObject content) {
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
  public Document setHeaders(final JSONObject content, final boolean replace) {
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
  public Document setId(final String id) {
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
      throw new RuntimeException(e);
    }

    return data;
  }

  public String toString() {
    return this.serialize().toString();
  }

  /**
   * Sets version.
   *
   * @param version the version
   */
  public void setVersion(long version) {
    if (version > 0) {
      this.version = version;
    }
  }
}
