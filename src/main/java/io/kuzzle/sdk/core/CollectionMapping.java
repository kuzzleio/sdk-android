package io.kuzzle.sdk.core;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * The type Kuzzle data mapping.
 */
public class CollectionMapping {

  private JSONObject headers;
  private JSONObject mapping;
  private Kuzzle kuzzle;
  private String collection;
  private Collection dataCollection;

  /**
   * When creating a new data collection in the persistent data storage layer, Kuzzle uses a default mapping.
   * It means that, by default, you won't be able to exploit the full capabilities of our persistent data storage layer
   * (currently handled by ElasticSearch), and your searches may suffer from below-average performances, depending on
   * the amount of data you stored in a collection and the complexity of your database.
   * The CollectionMapping object allow to get the current mapping of a data collection and to modify it if needed.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public CollectionMapping(final Collection kuzzleDataCollection) {
    this(kuzzleDataCollection, null);
  }

  /**
   * Instantiates a new Kuzzle data mapping.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   * @param mapping              the mapping
   */
  public CollectionMapping(final Collection kuzzleDataCollection, final JSONObject mapping) {
    try {
      this.headers = new JSONObject(kuzzleDataCollection.getHeaders().toString());
    }
    catch(JSONException e) {
      throw new RuntimeException(e);
    }

    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.collection = kuzzleDataCollection.getCollection();
    this.mapping = mapping == null ? new JSONObject() : mapping;
    this.dataCollection = kuzzleDataCollection;
  }

  /**
   * Copy constructor
   *
   * @param kuzzleDataMapping the CollectionMapping object to copy
   */
  public CollectionMapping(final CollectionMapping kuzzleDataMapping) {
    this(kuzzleDataMapping.dataCollection, kuzzleDataMapping.mapping);
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @return the kuzzle data mapping
   */
  public CollectionMapping apply() {
    return this.apply(null, null);
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @param options the options
   * @return the kuzzle data mapping
   */
  public CollectionMapping apply(final KuzzleOptions options) {
    return this.apply(options, null);
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @param listener the listener
   * @return the kuzzle data mapping
   */
  public CollectionMapping apply(final KuzzleResponseListener<CollectionMapping> listener) {
    return this.apply(null, listener);
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @param options  the options
   * @param listener the cb
   * @return the kuzzle data mapping
   */
  public CollectionMapping apply(final KuzzleOptions options, final KuzzleResponseListener<CollectionMapping> listener) {
    JSONObject data = new JSONObject();
    JSONObject properties = new JSONObject();
    try {
      properties.put("properties", this.mapping);
      data.put("body", properties);
      this.kuzzle.addHeaders(data, this.headers);
      this.kuzzle.query(this.dataCollection.makeQueryArgs("collection", "updateMapping"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          if (listener != null) {
            listener.onSuccess(CollectionMapping.this);
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
  public void refresh(final KuzzleResponseListener<CollectionMapping> listener) {
    refresh(null, listener);
  }

  /**
   * Gets a refreshed copy of the current object
   *
   * @param options  the options
   * @param listener the listener
   */
  public void refresh(final KuzzleOptions options, @NonNull final KuzzleResponseListener<CollectionMapping> listener) {
    if (listener == null) {
      throw new IllegalArgumentException("CollectionMapping.refresh: listener callback missing");
    }

    JSONObject data = new JSONObject();
    try {
      this.kuzzle.addHeaders(data, this.headers);
      this.kuzzle.query(this.dataCollection.makeQueryArgs("collection", "getMapping"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject args) {
          try {
            CollectionMapping newMapping = new CollectionMapping(CollectionMapping.this.dataCollection);
            JSONObject mappings = args.getJSONObject(CollectionMapping.this.dataCollection.getIndex()).getJSONObject("mappings");
            newMapping.mapping = mappings.getJSONObject(CollectionMapping.this.collection);

            listener.onSuccess(newMapping);
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject object) {
          listener.onError(object);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds or updates a field mapping.
   * Changes made by this function won't be applied until you call the apply method
   *
   * @param field the field
   * @return kuzzle data mapping
   */
  public CollectionMapping remove(final String field) {
    if (this.mapping.has(field)) {
      CollectionMapping.this.mapping.remove(field);
    }

    return this;
  }

  /**
   * Set kuzzle data mapping.
   *
   * @param field   the field
   * @param mapping the mapping
   * @return the kuzzle data mapping
   * @throws JSONException the json exception
   */
  public CollectionMapping set(final String field, final JSONObject mapping) throws JSONException {
    this.mapping.put(field, mapping);
    return this;
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
   * Helper function allowing to set headers while chaining calls.
   * If the replace argument is set to true, replace the current headers with the provided content.
   * Otherwise, it appends the content to the current headers, only replacing already existing values
   *
   * @param content the headers
   * @return the headers
   */
  public CollectionMapping setHeaders(final JSONObject content) {
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
  public CollectionMapping setHeaders(final JSONObject content, final boolean replace) {
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
   * Gets mapping.
   *
   * @return the mapping
   */
  public JSONObject getMapping() {
    return mapping;
  }
}
