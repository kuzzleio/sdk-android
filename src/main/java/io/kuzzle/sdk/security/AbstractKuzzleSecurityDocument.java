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
  /**
   * The Kuzzle.
   */
  protected final Kuzzle kuzzle;
  /**
   * The Kuzzle security.
   */
  protected final KuzzleSecurity kuzzleSecurity;
  /**
   * The Delete action name.
   */
  protected String deleteActionName;
  /**
   * The Update action name.
   */
  protected String  updateActionName;
  /**
   * The Id.
   */
  public final String id;
  /**
   * The Content.
   */
  public JSONObject content;

  /**
   * Instantiates a new Abstract kuzzle security document.
   *
   * @param kuzzle  the kuzzle
   * @param id      the id
   * @param content the content
   * @throws JSONException the json exception
   */
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
   * @throws JSONException the json exception
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
   * @throws JSONException the json exception
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
   * @param options  - Optional configuration
   * @param listener - Optional response callback
   * @throws JSONException the json exception
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
   * @throws JSONException the json exception
   */
  public void delete(final KuzzleResponseListener<String> listener) throws JSONException {
    this.delete(null, listener);
  }

  /**
   * Delete this role/profile/user from Kuzzle
   *
   * @param options - Optional configuration
   * @throws JSONException the json exception
   */
  public void delete(final KuzzleOptions options) throws JSONException {
    this.delete(options, null);
  }

  /**
   * Delete this role/profile/user from Kuzzle
   *
   * @throws JSONException the json exception
   */
  public void delete() throws JSONException {
    this.delete(null, null);
  }

  /**
   * Getter for the "id" property
   *
   * @return the document id
   */
  public String getId() {
    return this.id;
  }

  /**
   * Getter for the "content" property
   *
   * @return the document content
   */
  public JSONObject getContent() {
    return this.content;
  }

  /**
   * Perform a partial update on this object
   *
   * @param content - content used to update the object
   * @return AbstractKuzzleSecurityDocument this object
   * @throws JSONException - JSONException
   */
  public AbstractKuzzleSecurityDocument update(final JSONObject content) throws JSONException {
    return this.update(content, null, null);
  }

  /**
   * Perform a partial update on this object
   *
   * @param content - content used to update the object
   * @param options - optional arguments
   * @return AbstractKuzzleSecurityDocument this object
   * @throws JSONException - JSONException
   */
  public AbstractKuzzleSecurityDocument update(final JSONObject content, final KuzzleOptions options) throws JSONException {
    return this.update(content, options, null);
  }

  /**
   * Perform a partial update on this object
   *
   * @param content - content used to update the object
   * @param listener the listener
   * @return AbstractKuzzleSecurityDocument this object
   * @throws JSONException the json exception
   */
  public AbstractKuzzleSecurityDocument update(final JSONObject content, final KuzzleResponseListener<AbstractKuzzleSecurityDocument> listener) throws JSONException {
    return this.update(content, null, listener);
  }

  /**
   * Perform a partial update on this object
   *
   * @param content - content used to update the object
   * @param options  the options
   * @param listener the listener
   * @return AbstractKuzzleSecurityDocument this object
   * @throws JSONException the json exception
   */
  public AbstractKuzzleSecurityDocument update(final JSONObject content, final KuzzleOptions options, final KuzzleResponseListener<AbstractKuzzleSecurityDocument> listener) throws JSONException {
    JSONObject data = new JSONObject()
      .put("_id", this.id)
      .put("body", content);

    if (listener != null) {
      this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.updateActionName), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject updatedContent = response.getJSONObject("result").getJSONObject("_source");
            AbstractKuzzleSecurityDocument.this.setContent(updatedContent);
            listener.onSuccess(AbstractKuzzleSecurityDocument.this);
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
      this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.updateActionName), data, options);
    }

    return this;
  }
}
