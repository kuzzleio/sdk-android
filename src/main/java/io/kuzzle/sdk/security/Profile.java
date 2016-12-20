package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles profiles management in Kuzzle
 */
public class Profile extends AbstractSecurityDocument {
  private JSONArray policies = new JSONArray();

  /**
   * Instantiates a new Kuzzle profile.
   *
   * @param kuzzle  the kuzzle
   * @param id      the id
   * @param content the content
   * @throws JSONException the json exception
   */
  public Profile(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
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
  public Profile save(final Options options, final ResponseListener<Profile> listener) throws JSONException {
    JSONObject data;

    if (this.policies.length() == 0) {
      throw new IllegalArgumentException("Cannot save the profile " + this.id + ": no policies defined");
    }

    data = this.serialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceProfile"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          listener.onSuccess(Profile.this);
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
  public Profile save(final ResponseListener<Profile> listener) throws JSONException {
    return this.save(null, listener);
  }

  /**
   * Save this profile in Kuzzle
   *
   * @param options - Optional arguments
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public Profile save(final Options options) throws JSONException {
    return this.save(options, null);
  }

  /**
   * Save this profile in Kuzzle
   *
   * @return this kuzzle profile
   * @throws JSONException the json exception
   */
  public Profile save() throws JSONException {
    return this.save(null, null);
  }

  /**
   * Add a new role to the list of allowed roles of this profile
   *
   * @param policy - Policy to add to this profile
   * @return this kuzzle profile
   * @throws IllegalArgumentException
   */
  public Profile addPolicy(final JSONObject policy) throws IllegalArgumentException {
    if (!policy.has("roleId")) {
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
   */
  public Profile addPolicy(final String roleId) {
    JSONObject policy = new JSONObject();
    try {
      policy.put("roleId", roleId);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    this.policies.put(policy);
    return this;
  }

  /**
   * Replace the current policies list with a new one
   *
   * @param policies - New policies list
   * @return this roles
   * @throws IllegalArgumentException
   */
  public Profile setPolicies(final JSONArray policies) throws IllegalArgumentException {
    try {
      for (int i = 0; i < policies.length(); i++) {
        if (!policies.getJSONObject(i).has("roleId")) {
          throw new IllegalArgumentException("All pocicies must have at least a roleId set.");
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }

    this.policies = policies;

    return this;
  }

  /**
   * Replace the current policies list with a new one via its rolesIds
   *
   * @param rolesIds - New roles list
   * @return this roles
   */
  public Profile setPolicies(final String rolesIds[]) {
    try {
      for (int i = 0; i < rolesIds.length; i++) {
        JSONObject policy = new JSONObject();

        policy.put("roleId", rolesIds[i]);

        this.policies.put(policy);
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
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
