package io.kuzzle.sdk.test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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
   * <p>
   * The KuzzleDataMapping object allow to get the current mapping of a data collection and to modify it if needed.
   *
   * @param kuzzleDataCollection the kuzzle data collection
   */
  public KuzzleDataMapping(KuzzleDataCollection kuzzleDataCollection) {
    this.headers = kuzzleDataCollection.getHeaders();
    this.kuzzle = kuzzleDataCollection.getKuzzle();
    this.collection = kuzzleDataCollection.getCollection();
  }

  /**
   * Applies the new mapping to the data collection.
   *
   * @param cb the cb
   * @return the kuzzle data mapping
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataMapping apply(ResponseListener cb) throws JSONException, IOException, KuzzleException {
    JSONObject data = new JSONObject();
    JSONObject properties = new JSONObject();
    properties.put("properties", this.mapping);
    data.put("body", properties);
    this.kuzzle.addHeaders(data, this.headers);
    this.kuzzle.query(this.collection, "admin", "putMapping", data, cb);
    return this;
  }

  /**
   * Apply kuzzle data mapping.
   *
   * @return the kuzzle data mapping
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataMapping apply() throws JSONException, IOException, KuzzleException {
    return apply(null);
  }

  /**
   * Replaces the current content with the mapping stored in Kuzzle
   * <p>
   * Calling this function will discard any uncommited changes. You can commit changes by calling the "apply" function
   *
   * @param cb the cb
   * @return the kuzzle data mapping
   * @throws JSONException the json exception
   * @throws IOException   the io exception
   */
  public KuzzleDataMapping refresh(final ResponseListener cb) throws JSONException, IOException, KuzzleException {
    JSONObject data = new JSONObject();
    this.kuzzle.addHeaders(data, this.headers);
    this.kuzzle.query(this.collection, "admin", "getMapping", data, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        JSONObject mappings = (JSONObject) ((JSONObject) ((JSONObject) args).get("mainindex")).get("mappings");
        if (!mappings.isNull(KuzzleDataMapping.this.collection))
          KuzzleDataMapping.this.mapping = (JSONObject) mappings.get(KuzzleDataMapping.this.collection);
        if (cb != null)
          cb.onSuccess(args);
      }

      @Override
      public void onError(JSONObject object) throws Exception {
        if (cb != null)
          cb.onError(object);
      }

    });
    return this;
  }

  /**
   * Refresh kuzzle data mapping.
   *
   * @return the kuzzle data mapping
   * @throws IOException   the io exception
   * @throws JSONException the json exception
   */
  public KuzzleDataMapping refresh() throws IOException, JSONException, KuzzleException {
    return refresh(null);
  }

  /**
   * Adds or updates a field mapping.
   * <p>
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
  public KuzzleDataMapping set(String field, JSONArray mapping) throws JSONException {
    KuzzleDataMapping.this.mapping.put(field, mapping);
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
   * Sets headers.
   *
   * @param headers the headers
   */
  public void setHeaders(JSONObject headers) {
    this.headers = headers;
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
