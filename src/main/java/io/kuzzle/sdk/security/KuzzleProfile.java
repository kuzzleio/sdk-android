package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles profiles management in Kuzzle
 */
public class KuzzleProfile extends AbstractKuzzleSecurityDocument {
  private JSONArray policies = new JSONArray();

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

    if (content != null) {
      this.content = new JSONObject(content.toString());

      if (content.has("policies")) {
        this.policies = content.getJSONArray("policies");

        this.content.remove("policies");
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

    if (this.policies.length() == 0) {
      throw new IllegalArgumentException("Cannot save the profile " + this.id + ": no policies defined");
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
   * @param policy - Policy to add to this profile
   * @return this kuzzle profile
   * @throws IllegalArgumentException, JSONException
   */
  public KuzzleProfile addPolicy(final JSONObject policy) throws IllegalArgumentException, JSONException {
    if (policy.getString("roleId") == null) {
      throw new IllegalArgumentException("The policy must have, at least, a roleId set.");
    }
    this.policies.put(policy);
    return this;
  }

  /**
   * Add a new policy to the list of policies of this profile via its roleId
   *
   * @param  roleId - Name of the role to add to this profile
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public KuzzleProfile addPolicy(final String roleId) throws JSONException {
    JSONObject policy = new JSONObject();
    policy.put("roleId", roleId);
    this.policies.put(policy);
    return this;
  }

  /**
   * Replace the current policies list with a new one
   *
   * @param policies - New policies list
   * @return this roles
   * @throws IllegalArgumentException, JSONException
   */
  public KuzzleProfile setPolicies(final JSONArray policies) throws IllegalArgumentException, JSONException {
    for (int i = 0; i < policies.length(); i++) {
      if (policies.getJSONObject(i).getString("roleId") == null) {
        throw new IllegalArgumentException("All pocicies must have at least a roleId set.");
      }
    }
    this.policies = policies;

    return this;
  }

  /**
   * Replace the current policies list with a new one via its rolesIds
   *
   * @param rolesIds - New roles list
   * @return this roles
   * @throws JSONException
   */
  public KuzzleProfile setPolicies(final String rolesIds[]) throws JSONException {
    for (int i = 0; i < rolesIds.length; i++) {
      JSONObject policy = new JSONObject();
      policy.put("roleId", rolesIds[i]);
      this.policies.put(policy);
    }

    return this;
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

    if (this.policies.length() > 0) {
      content.put("policies", this.policies);
    }

    data
      .put("_id", this.id)
      .put("body", content);

    return data;
  }

  /**
   * Return the list of the policies assigned to this profile
   *
   * @return a JSONArray of policies" objects
   */
  public JSONArray getPolicies() {
    return this.policies;
  }
}
