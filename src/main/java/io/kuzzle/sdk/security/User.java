package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles users management in Kuzzle
 */
public class User extends AbstractSecurityDocument {
  /**
   * The Profiles Ids List.
   */
  private JSONArray profileIds = null;

  /**
   * Instantiates a new Kuzzle user.
   *
   * @param kuzzle  the kuzzle
   * @param id      the id
   * @param content the content
   * @throws JSONException the json exception
   */
  public User(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
    super(kuzzle, id, null);
    this.deleteActionName = "deleteUser";
    this.updateActionName = "updateUser";

    if (content != null) {
      this.content = new JSONObject(content.toString());

      if (content.has("profileIds")) {
        this.profileIds = content.getJSONArray("profileIds");
      }
    }
  }

  /**
   * Set a new profiles set for this user
   *
   * @param profileIds - Strings array of profiles IDs
   * @return this profiles
   */
  public User setProfiles(@NonNull final String[] profileIds) {
    if (profileIds == null) {
      throw new IllegalArgumentException("User.setProfiles: you must provide an array of profiles IDs strings");
    }

    this.profileIds = new JSONArray(Arrays.asList(profileIds));

    return this;
  }

  /**
   * adds a new profile to the profiles set of this user
   *
   * @param profile - String
   * @return this profiles
   */
  public User addProfile(@NonNull final String profile) {
    if (profile == null) {
      throw new IllegalArgumentException("User.addProfile: you must provide a string");
    }

    this.profileIds.put(profile);

    return this;
  }

   /**
   * Save this user in Kuzzle
   *
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User save(final Options options, final ResponseListener<User> listener) throws JSONException {
    JSONObject data = this.serialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceUser"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          listener.onSuccess(User.this);
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    }
    else {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceUser"), data, options);
    }

    return this;
  }

  /**
   * Save this user in Kuzzle
   *
   * @param listener - Callback listener
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User save(final ResponseListener<User> listener) throws JSONException {
    return save(null, listener);
  }

  /**
   * Save this user in Kuzzle
   *
   * @param options - Optional arguments
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User save(final Options options) throws JSONException {
    return save(options, null);
  }

  /**
   * Save this user in Kuzzle
   *
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User save() throws JSONException {
    return save(null, null);
  }

  /**
   * Saves this user as restricted into Kuzzle.
   *
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User saveRestricted(final Options options, final ResponseListener<User> listener) throws JSONException {
    JSONObject data = this.serialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createRestrictedUser"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          listener.onSuccess(User.this);
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    }
    else {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createRestrictedUser"), data, options);
    }

    return this;
  }

  /**
   * Saves this user as restricted into Kuzzle.
   *
   * @param listener - Callback listener
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User saveRestricted(final ResponseListener<User> listener) throws JSONException {
    return saveRestricted(null, listener);
  }

  /**
   * Saves this user as restricted into Kuzzle.
   *
   * @param options - Optional arguments
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User saveRestricted(final Options options) throws JSONException {
    return saveRestricted(options, null);
  }

  /**
   * Saves this user as restricted into Kuzzle.
   *
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User saveRestricted() throws JSONException {
    return saveRestricted(null, null);
  }

  /**
   * Return a JSONObject representing a serialized version of this object
   *
   * @return serialized version of this object
   * @throws JSONException the json exception
   */
  public JSONObject serialize() throws JSONException {
    JSONObject
      data = new JSONObject().put("_id", this.id),
      content = new JSONObject(this.content.toString());

    if (this.profileIds != null) {
      content.put("profileIds", this.profileIds);
    }

    data.put("body", content);

    return data;
  }

  /**
   * Returns the associated profiles
   *
   * @return an array of strings
   */
  public JSONArray getProfiles() {
    return this.profileIds;
  }
}
