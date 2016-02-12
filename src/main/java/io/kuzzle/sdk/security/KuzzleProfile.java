package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Arrays;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles profiles management in Kuzzle
 */
public class KuzzleProfile extends AbstractKuzzleSecurityDocument {
  ArrayDeque<KuzzleRole> roles;

  public KuzzleProfile(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
    super(kuzzle, id, null);
    this.deleteActionName = "deleteProfile";
    this.roles = new ArrayDeque<>();

    if (content != null) {
      this.content = new JSONObject(content.toString());

      if (content.has("roles")) {
        JSONArray roles = content.getJSONArray("roles");
        int rolesLength = roles.length();

        this.content.remove("roles");

        for (int i = 0; i < rolesLength; i++) {
          JSONObject role = roles.getJSONObject(i);

          if (role.has("_id")) {
            if (role.has("_source")) {
              this.roles.add(new KuzzleRole(this.kuzzle, role.getString("_id"), role.getJSONObject("_source")));
            } else {
              this.roles.add(new KuzzleRole(this.kuzzle, role.getString("_id"), null));
            }
          }
        }
      }
    }
  }

  /**
   * Save this profile in Kuzzle
   *
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @return this
   * @throws JSONException
   */
  public KuzzleProfile save(final KuzzleOptions options, final KuzzleResponseListener listener) throws JSONException {
    JSONObject data;

    if (this.roles.isEmpty()) {
      throw new IllegalArgumentException("Cannot save the profile " + this.id + ": no role defined");
    }

    data = this.serialize();

    this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceProfile"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        if (listener != null) {
          listener.onSuccess(this);
        }
      }

      @Override
      public void onError(JSONObject error) {
        if (listener != null) {
          listener.onError(error);
        }
      }
    });

    return this;
  }

  /**
   * Save this profile in Kuzzle
   *
   * @param listener - Callback listener
   * @return this
   * @throws JSONException
   */
  public KuzzleProfile save(final KuzzleResponseListener listener) throws JSONException {
    return this.save(null, listener);
  }

  /**
   * Save this profile in Kuzzle
   *
   * @param options - Optional arguments
   * @return this
   * @throws JSONException
   */
  public KuzzleProfile save(final KuzzleOptions options) throws JSONException {
    return this.save(options, null);
  }

  /**
   * Save this profile in Kuzzle
   *
   * @return this
   * @throws JSONException
   */
  public KuzzleProfile save() throws JSONException {
    return this.save(null, null);
  }

  /**
   * Add a new role to the list of allowed roles of this profile
   *
   * @param role - Role to add to this profile
   * @return this
   */
  public KuzzleProfile addRole(final KuzzleRole role) {
    this.roles.add(role);
    return this;
  }

  /**
   * Add a new role to the list of allowed roles of this profile
   *
   * @param role - Name of the role to add to this profile
   * @return this
   */
  public KuzzleProfile addRole(final String role) {
    try {
      this.roles.add(new KuzzleRole(this.kuzzle, role, null));
    }
    catch(JSONException e) {
      throw new RuntimeException(e);
    }

    return this;
  }

  /**
   * Replace the current roles list with a new one
   *
   * @param roles - New roles list
   * @return this
   */
  public KuzzleProfile setRoles(final KuzzleRole roles[]) {
    this.roles.addAll(Arrays.asList(roles));

    return this;
  }

  /**
   * Replace the current roles list with a new one
   *
   * @param roles - New roles list
   * @return this
   */
  public KuzzleProfile setRoles(final String roles[]) {
    for(String role : roles) {
      try {
        this.roles.add(new KuzzleRole(this.kuzzle, role, null));
      }
      catch(JSONException e) {
        throw new RuntimeException(e);
      }
    }

    return this;
  }

  /**
   * Hydrate the profile - get real KuzzleRole and not just ids
   * Trying to hydrate when new unsaved roles have been added will fail
   *
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void hydrate(final KuzzleOptions options, @NonNull final KuzzleResponseListener listener) throws JSONException {
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleRole.hydrate: a callback listener is required");
    }

    JSONArray ids = new JSONArray();

    for(KuzzleRole role : this.roles) {
      ids.put(role.id);
    }

    JSONObject data = new JSONObject().put("body", ids);

    this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("mGetRoles"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          listener.onSuccess(new KuzzleProfile(KuzzleProfile.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
        }
        catch (JSONException e) {
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
   * Hydrate the profile - get real KuzzleRole and not just ids
   * Trying to hydrate when new unsaved roles have been added will fail
   *
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void hydrate(@NonNull final KuzzleResponseListener listener) throws JSONException {
    hydrate(null, listener);
  }

  /**
   * Serialize the content of this object to a JSON Object
   * @return a serialized version of this object
   * @throws JSONException
   */
  public JSONObject serialize() throws JSONException {
    JSONObject
      data = new JSONObject(),
      content = new JSONObject(this.content.toString());

    if (!this.roles.isEmpty()) {
      JSONArray roles = new JSONArray();

      for (KuzzleRole role : this.roles) {
        roles.put(role.id);
      }

      content.put("roles", roles);
    }

    data
      .put("_id", this.id)
      .put("body", content);

    return data;
  }
}
