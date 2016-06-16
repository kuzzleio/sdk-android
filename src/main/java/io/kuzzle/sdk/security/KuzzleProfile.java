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
  private ArrayDeque<KuzzleRole> roles;

  /**
   * Instantiates a new Kuzzle profile.
   *
   * @param kuzzle  the kuzzle
   * @param id      the id
   * @param content the content
   * @throws JSONException the json exception
   */
  public KuzzleProfile(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
    super(kuzzle, id, null);
    this.deleteActionName = "deleteProfile";
    this.updateActionName = "updateProfile";
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
              this.roles.add(new KuzzleRole(this.kuzzle, role.getString("_id"), role));
            }
          }
        }
      }
    }
  }

  /**
   * Save this profile in Kuzzle
   *
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public KuzzleProfile save(final KuzzleOptions options, final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    JSONObject data;

    if (this.roles.isEmpty()) {
      throw new IllegalArgumentException("Cannot save the profile " + this.id + ": no role defined");
    }

    data = this.serialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceProfile"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          listener.onSuccess(KuzzleProfile.this);
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    }
    else {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceProfile"), data, options);
    }

    return this;
  }

  /**
   * Save this profile in Kuzzle
   *
   * @param listener - Callback listener
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public KuzzleProfile save(final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    return this.save(null, listener);
  }

  /**
   * Save this profile in Kuzzle
   *
   * @param options - Optional arguments
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public KuzzleProfile save(final KuzzleOptions options) throws JSONException {
    return this.save(options, null);
  }

  /**
   * Save this profile in Kuzzle
   *
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public KuzzleProfile save() throws JSONException {
    return this.save(null, null);
  }

  /**
   * Add a new role to the list of allowed roles of this profile
   *
   * @param role - Role to add to this profile
   * @return this kuzzle profile
   */
  public KuzzleProfile addRole(final KuzzleRole role) {
    this.roles.add(role);
    return this;
  }

  /**
   * Add a new role to the list of allowed roles of this profile
   *
   * @param role - Name of the role to add to this profile
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public KuzzleProfile addRole(final String role) throws JSONException {
    this.roles.add(new KuzzleRole(this.kuzzle, role, null));
    return this;
  }

  /**
   * Replace the current roles list with a new one
   *
   * @param roles - New roles list
   * @return this roles
   */
  public KuzzleProfile setRoles(final KuzzleRole roles[]) {
    this.roles.clear();
    this.roles.addAll(Arrays.asList(roles));

    return this;
  }

  /**
   * Replace the current roles list with a new one
   *
   * @param roles - New roles list
   * @return this roles
   */
  public KuzzleProfile setRoles(final String roles[]) {
    this.roles.clear();
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
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void hydrate(final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
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
          JSONArray hits = result.getJSONArray("hits");
          KuzzleProfile profile = new KuzzleProfile(KuzzleProfile.this.kuzzle, KuzzleProfile.this.id, null);

          for (int i = 0; i < hits.length(); i++) {
            JSONObject role = hits.getJSONObject(i);
            profile.addRole(new KuzzleRole(KuzzleProfile.this.kuzzle, role.getString("_id"), role.getJSONObject("_source")));
          }

          listener.onSuccess(profile);
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
   * @throws JSONException the json exception
   */
  public void hydrate(@NonNull final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    hydrate(null, listener);
  }

  /**
   * Serialize the content of this object to a JSON Object
   * @return a serialized version of this object
   * @throws JSONException the json exception
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

  /**
   * Return the list of roles assigned to this profile
   *
   * @return an array of KuzzleRole objects
   */
  public KuzzleRole[] getRoles() {
    return this.roles.toArray(new KuzzleRole[this.roles.size()]);
  }
}
