package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles users management in Kuzzle
 */
public class KuzzleUser extends AbstractKuzzleSecurityDocument {
  public KuzzleProfile profile = null;

  public KuzzleUser(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
    super(kuzzle, id, null);
    this.deleteActionName = "deleteUser";

    if (content != null) {
      this.content = new JSONObject(content.toString());

      if (content.has("profile")) {
        JSONObject profile = content.getJSONObject("profile");

        if (profile.has("_id")) {
          if (profile.has("_source")) {
            this.profile = new KuzzleProfile(this.kuzzle, profile.getString("_id"), profile.getJSONObject("_source"));
          }
          else {
            this.profile = new KuzzleProfile(this.kuzzle, profile.getString("_id"), null);
          }
        }
      }
    }
  }

  /**
   * This function allow to get the hydrated user of the corresponding current user.
   * The hydrated user has profiles and roles.
   *
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void hydrate(final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleUser.hydrate: a callback listener is required");
    }

    if (this.profile == null) {
      throw new IllegalArgumentException("KuzzleUser.hydrate: cannot save a user without a profile");
    }

    JSONObject data = new JSONObject().put("_id", this.profile.id);

    this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("getProfile"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          KuzzleUser user = new KuzzleUser(KuzzleUser.this.kuzzle, KuzzleUser.this.id, null);
          user.setProfile(new KuzzleProfile(KuzzleUser.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
          listener.onSuccess(user);
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

  /**
   * This function allow to get the hydrated user of the corresponding current user.
   * The hydrated user has profiles and roles.
   *
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void hydrate(@NonNull final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    hydrate(null, listener);
  }

  /**
   * Set a new profile for this user
   *
   * @param profile - new profile
   * @return this
   */
  public KuzzleUser setProfile(@NonNull final KuzzleProfile profile) {
    if (profile == null) {
      throw new IllegalArgumentException("KuzzleUser.setProfile: cannot set null profile");
    }

    this.profile = profile;

    return this;
  }

  /**
   * Set a new profile for this user
   *
   * @param id - new profile ID
   * @return this
   */
  public KuzzleUser setProfile(@NonNull final String id) throws JSONException {
    setProfile(new KuzzleProfile(this.kuzzle, id, null));
    return this;
  }

  /**
   * Save this user in Kuzzle
   *
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @return this
   * @throws JSONException
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
   * @return this
   * @throws JSONException
   */
  public KuzzleUser save(final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    return save(null, listener);
  }

  /**
   * Save this user in Kuzzle
   *
   * @param options - Optional arguments
   * @return this
   * @throws JSONException
   */
  public KuzzleUser save(final KuzzleOptions options) throws JSONException {
    return save(options, null);
  }

  /**
   * Save this user in Kuzzle
   *
   * @return this
   * @throws JSONException
   */
  public KuzzleUser save() throws JSONException {
    return save(null, null);
  }

  /**
   * Return a JSONObject representing a serialized version of this object
   *
   * @return serialized version of this object
   * @throws JSONException
   */
  public JSONObject serialize() throws JSONException {
    JSONObject
      data = new JSONObject().put("_id", this.id),
      content = new JSONObject(this.content.toString());

    if (profile != null) {
      content.put("profile", this.profile.id);
    }

    data.put("body", content);

    return data;
  }
}
