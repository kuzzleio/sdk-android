package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * Base class for the Role, Profile and User classes
 */
public class AbstractSecurityDocument {
  /**
   * The Kuzzle.
   */
  protected final Kuzzle kuzzle;
  /**
   * The Kuzzle security.
   */
  protected final Security kuzzleSecurity;
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
  public AbstractSecurityDocument(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
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
   * @return AbstractSecurityDocument - this object
   * @throws JSONException the json exception
   */
  public AbstractSecurityDocument setContent(@NonNull final JSONObject content) throws JSONException {
    if (content == null) {
      throw new IllegalArgumentException("AbstractSecurityDocument.setContent: cannot set null content");
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
  public void delete(final Options options, final ResponseListener<String> listener) throws JSONException {
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
  public void delete(final ResponseListener<String> listener) throws JSONException {
    this.delete(null, listener);
  }

  /**
   * Delete this role/profile/user from Kuzzle
   *
   * @param options - Optional configuration
   * @throws JSONException the json exception
   */
  public void delete(final Options options) throws JSONException {
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
   * @return AbstractSecurityDocument this object
   * @throws JSONException - JSONException
   */
  public AbstractSecurityDocument update(final JSONObject content) throws JSONException {
    return this.update(content, null, null);
  }

  /**
   * Perform a partial update on this object
   *
   * @param content - content used to update the object
   * @param options - optional arguments
   * @return AbstractSecurityDocument this object
   * @throws JSONException - JSONException
   */
  public AbstractSecurityDocument update(final JSONObject content, final Options options) throws JSONException {
    return this.update(content, options, null);
  }

  /**
   * Perform a partial update on this object
   *
   * @param content - content used to update the object
   * @param listener the listener
   * @return AbstractSecurityDocument this object
   * @throws JSONException the json exception
   */
  public AbstractSecurityDocument update(final JSONObject content, final ResponseListener<AbstractSecurityDocument> listener) throws JSONException {
    return this.update(content, null, listener);
  }

  /**
   * Perform a partial update on this object
   *
   * @param content - content used to update the object
   * @param options  the options
   * @param listener the listener
   * @return AbstractSecurityDocument this object
   * @throws JSONException the json exception
   */
  public AbstractSecurityDocument update(final JSONObject content, final Options options, final ResponseListener<AbstractSecurityDocument> listener) throws JSONException {
    JSONObject data = new JSONObject()
      .put("_id", this.id)
      .put("body", content);

    if (listener != null) {
      this.kuzzle.query(kuzzleSecurity.buildQueryArgs(this.updateActionName), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject updatedContent = response.getJSONObject("result").getJSONObject("_source");
            AbstractSecurityDocument.this.setContent(updatedContent);
            listener.onSuccess(AbstractSecurityDocument.this);
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
