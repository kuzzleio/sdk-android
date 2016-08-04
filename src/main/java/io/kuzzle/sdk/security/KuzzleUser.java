package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles users management in Kuzzle
 */
public class KuzzleUser extends AbstractKuzzleSecurityDocument {
  /**
   * The Profiles Ids List.
   */
  private JSONArray profilesIds = null;

  /**
   * Instantiates a new Kuzzle user.
   *
   * @param kuzzle  the kuzzle
   * @param id      the id
   * @param content the content
   * @throws JSONException the json exception
   */
  public KuzzleUser(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
    super(kuzzle, id, null);
    this.deleteActionName = "deleteUser";
    this.updateActionName = "updateUser";

    if (content != null) {
      this.content = new JSONObject(content.toString());

      if (content.has("profilesIds")) {
        this.profilesIds = content.getJSONArray("profilesIds");
      }
    }
  }

  /**
   * Set a new profiles set for this user
   *
   * @param profilesIds - Strings array of profiles IDs
   * @return this profiles
   */
  public KuzzleUser setProfiles(@NonNull final String[] profilesIds) {
    if (profilesIds == null) {
      throw new IllegalArgumentException("KuzzleUser.setProfiles: you must provide an array of profiles IDs strings");
    }

    this.profilesIds = new JSONArray(Arrays.asList(profilesIds));

    return this;
  }

  /**
   * adds a new profile to the profiles set of this user
   *
   * @param profile - String
   * @return this profiles
   */
  public KuzzleUser addProfile(@NonNull final String profile) {
    if (profile == null) {
      throw new IllegalArgumentException("KuzzleUser.addProfile: you must provide a string");
    }

    this.profilesIds.put(profile);

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
  public KuzzleUser save(final KuzzleOptions options, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    JSONObject data = this.serialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceUser"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          listener.onSuccess(KuzzleUser.this);
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
  public KuzzleUser save(final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    return save(null, listener);
  }

  /**
   * Save this user in Kuzzle
   *
   * @param options - Optional arguments
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public KuzzleUser save(final KuzzleOptions options) throws JSONException {
    return save(options, null);
  }

  /**
   * Save this user in Kuzzle
   *
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public KuzzleUser save() throws JSONException {
    return save(null, null);
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

    if (this.profilesIds != null) {
      content.put("profilesIds", this.profilesIds);
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
    return this.profilesIds;
  }
}
