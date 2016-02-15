package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * Base class for the KuzzleRole, KuzzleProfile and KuzzleUser classes
 */
public class AbstractKuzzleSecurityDocument {
  protected final Kuzzle kuzzle;
  protected final KuzzleSecurity kuzzleSecurity;
  protected String deleteActionName;
  public final String id;
  public JSONObject content;

  public AbstractKuzzleSecurityDocument(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("Cannot initialize with a null ID");
    }

    this.kuzzle = kuzzle;
    this.kuzzleSecurity = kuzzle.security;
    this.id = id;

    if (content != null) {
      setContent(content);
    } else {
      this.content = new JSONObject();
    }
  }

  /**
   * Sets the content of this object
   *
   * @param content - new content
   * @return AbstractKuzzleSecurityDocument - this object
   */
  public AbstractKuzzleSecurityDocument setContent(@NonNull final JSONObject content) throws JSONException {
    if (content == null) {
      throw new IllegalArgumentException("AbstractKuzzleSecurityDocument.setContent: cannot set null content");
    }

    this.content = new JSONObject(content.toString());

    return this;
  }

  /**
   * Serializes this object to a plain-old JSON object
   *
   * @return JSONObject - the serialized version of this object
   */
  public JSONObject serialize() throws JSONException {
    JSONObject data;

    data = new JSONObject()
      .put("_id", this.id)
      .put("body", content);

    return data;
  }

  /**
   * Delete this role/profile/user from Kuzzle
   *
   * @param options - Optional configuration
   * @param listener - Optional response callback
   * @throws JSONException
   */
  public void delete(final KuzzleOptions options, final KuzzleResponseListener<String> listener) throws JSONException {
    JSONObject data = new JSONObject().put("_id", this.id);

    if (listener != null) {
      this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.deleteActionName), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getString("_id"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    }
    else {
      this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.deleteActionName), data, options);
    }
  }

  /**
   * Delete this role/profile/user from Kuzzle
   *
   * @param listener - Optional response callback
   * @throws JSONException
   */
  public void delete(final KuzzleResponseListener<String> listener) throws JSONException {
    this.delete(null, listener);
  }

  /**
   * Delete this role/profile/user from Kuzzle
   *
   * @param options - Optional configuration
   * @throws JSONException
   */
  public void delete(final KuzzleOptions options) throws JSONException {
    this.delete(options, null);
  }

  /**
   * Delete this role/profile/user from Kuzzle
   *
   * @throws JSONException
   */
  public void delete() throws JSONException {
    this.delete(null, null);
  }
}
