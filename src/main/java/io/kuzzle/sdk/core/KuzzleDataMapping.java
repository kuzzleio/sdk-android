package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.ResponseListener;

/**
 * The type Kuzzle data mapping.
 */
public class KuzzleDataMapping {

  private JSONObject headers;
  private JSONObject mapping;
  private Kuzzle kuzzle;
  private String collection;

  /**
   * When creating a new data collection in the persistent data storage layer, Kuzzle uses a default mapping.
   * It means that, by default, you won't be able to exploit the full capabilities of our persistent data storage layer
   * (currently handled by ElasticSearch), and your searches may suffer from below-average performances, depending on
   * the amount of data you stored in a collection and the complexity of your database.
   
   * The KuzzleDataMapping object allow to get the current mapping of a data collection and to modify it if needed.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public KuzzleDataMapping(KuzzleDataCollection kuzzleDataCollection) {
    this(kuzzleDataCollection, null);
  }

  public KuzzleDataMapping(KuzzleDataCollection kuzzleDataCollection, JSONObject mapping) {
    this.headers = kuzzleDataCollection.getHeaders();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.collection = kuzzleDataCollection.getCollection();
    this.mapping = mapping == null ? new JSONObject() : mapping;
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @return the kuzzle data mapping
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataMapping  apply() throws KuzzleException, JSONException {
    return this.apply(null, null);
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @param options the options
   * @return the kuzzle data mapping
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataMapping  apply(KuzzleOptions options) throws KuzzleException, JSONException {
    return this.apply(options, null);
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @param listener the listener
   * @return the kuzzle data mapping
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataMapping  apply(ResponseListener listener) throws KuzzleException, JSONException {
    return this.apply(null, listener);
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @param options the options
   * @param cb      the cb
   * @return the kuzzle data mapping
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataMapping apply(KuzzleOptions options, ResponseListener cb) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    JSONObject properties = new JSONObject();
    properties.put("properties", this.mapping);
    data.put("body", properties);
    this.kuzzle.addHeaders(data, this.headers);
    this.kuzzle.query(this.collection, "admin", "putMapping", data, options, cb);
    return this;
  }

  /**
   * Refresh kuzzle data mapping.
   *
   * @return the kuzzle data mapping
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataMapping refresh() throws KuzzleException, JSONException {
    return this.refresh(null, null);
  }

  /**
   * Replaces the current content with the mapping stored in Kuzzle
   
   * Calling this function will discard any uncommited changes. You can commit changes by calling the "apply" function
   *
   * @param options the options
   * @return the kuzzle data mapping
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataMapping refresh(KuzzleOptions options) throws JSONException, KuzzleException {
    return refresh(options, null);
  }

  /**
   * Replaces the current content with the mapping stored in Kuzzle
   
   * Calling this function will discard any uncommited changes. You can commit changes by calling the "apply" function
   *
   * @param listener the listener
   * @return the kuzzle data mapping
   * @throws KuzzleException the kuzzle exception
   * @throws JSONException   the json exception
   */
  public KuzzleDataMapping refresh(ResponseListener listener) throws KuzzleException, JSONException {
    return refresh(null, listener);
  }

  /**
   * Replaces the current content with the mapping stored in Kuzzle
   
   * Calling this function will discard any uncommited changes. You can commit changes by calling the "apply" function
   *
   * @param options the options
   * @param cb      the cb
   * @return the kuzzle data mapping
   * @throws JSONException   the json exception
   * @throws KuzzleException the kuzzle exception
   */
  public KuzzleDataMapping refresh(KuzzleOptions options, final ResponseListener cb) throws JSONException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.headers);
    this.kuzzle.query(this.collection, "admin", "getMapping", data, options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) {
        try {
          if (!args.isNull(KuzzleDataMapping.this.kuzzle.getIndex())) {
            JSONObject mappings = args.getJSONObject(KuzzleDataMapping.this.kuzzle.getIndex()).getJSONObject("mappings");
            if (!mappings.isNull(KuzzleDataMapping.this.collection))
              KuzzleDataMapping.this.mapping = mappings.getJSONObject(KuzzleDataMapping.this.collection);
            if (cb != null)
              cb.onSuccess(mappings);
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject object) {
        if (cb != null)
          cb.onError(object);
      }

    });
    return this;
  }

  /**
   * Adds or updates a field mapping.
   
   * Changes made by this function won't be applied until you call the apply method
   *
   * @param field the field
   * @return kuzzle data mapping
   * @throws JSONException the json exception
   */
  public KuzzleDataMapping remove(String field) throws JSONException {
    if (!KuzzleDataMapping.this.mapping.isNull(field))
      KuzzleDataMapping.this.mapping.remove(field);
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
  public KuzzleDataMapping set(String field, JSONObject mapping) throws JSONException {
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
   * @throws JSONException the json exception
   */
  public KuzzleDataMapping setHeaders(JSONObject content) throws JSONException {
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
   * @throws JSONException the json exception
   */
  public KuzzleDataMapping setHeaders(JSONObject content, boolean replace) throws JSONException {
    if (this.headers == null) {
      this.headers = new JSONObject();
    }
    if (replace) {
      this.headers = content;
    } else {
      if (content != null) {
        for (Iterator ite = content.keys(); ite.hasNext(); ) {
          String key = (String) ite.next();
          this.headers.put(key, content.get(key));
        }
      }
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
