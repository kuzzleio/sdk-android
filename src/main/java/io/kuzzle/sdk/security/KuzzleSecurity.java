package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Policies;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleSecurityDocumentList;


/**
 * The type Kuzzle security.
 */
public class KuzzleSecurity {
  private final Kuzzle kuzzle;

  /**
   * Instantiates a new Kuzzle security.
   *
   * @param kuzzle the kuzzle
   */
  public KuzzleSecurity(final Kuzzle kuzzle) {
    this.kuzzle = kuzzle;
  }

  /**
   * Helper function meant to easily build the first Kuzzle.query() argument
   *
   * @param action - Security controller action name
   * @return JSONObject - Kuzzle.query() 1st argument object
   * @throws JSONException the json exception
   */
  protected io.kuzzle.sdk.core.Kuzzle.QueryArgs buildQueryArgs(@NonNull final String action) throws JSONException {
    io.kuzzle.sdk.core.Kuzzle.QueryArgs args = new io.kuzzle.sdk.core.Kuzzle.QueryArgs();
    args.action = action;
    args.controller = "security";
    return args;
  }

  /**
   * Retrieves a single Role using its unique Role ID
   *
   * @param id       - unique role ID
   * @param options  - optional query arguments
   * @param listener - response callback
   */
  public void fetchRole(@NonNull final String id, Options options, @NonNull final KuzzleResponseListener<KuzzleRole> listener) {
    JSONObject data;

    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.fetchRole: a role ID is required");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.fetchRole: a listener is required");
    }

    try {
      data = new JSONObject().put("_id", id);
      this.kuzzle.query(buildQueryArgs("fetchRole"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            listener.onSuccess(new KuzzleRole(KuzzleSecurity.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
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
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves a single Role using its unique Role ID
   *
   * @param id       - unique role ID
   * @param listener - response callback
   */
  public void fetchRole(@NonNull final String id, @NonNull final KuzzleResponseListener<KuzzleRole> listener) {
    fetchRole(id, null, listener);
  }


  /**
   * Executes a search on roles using a set of filters
   * * /!\ There is a small delay between role creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a role that was just been created won’t be returned by this function.
   *
   * @param filters  - search filters (see ElasticSearch filters)
   * @param options  - Optional query arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void searchRoles(@NonNull final JSONObject filters, final Options options, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    if (filters == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchRoles: filters cannot be null");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchRoles: a callback listener is required");
    }

    JSONObject data = new JSONObject().put("body", filters);

    this.kuzzle.query(buildQueryArgs("searchRoles"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          JSONArray documents = result.getJSONArray("hits");
          int documentsLength = documents.length();
          ArrayList<AbstractKuzzleSecurityDocument> roles = new ArrayList<>();

          for (int i = 0; i < documentsLength; i++) {
            JSONObject document = documents.getJSONObject(i);
            roles.add(new KuzzleRole(KuzzleSecurity.this.kuzzle, document.getString("_id"), document.getJSONObject("_source")));
          }

          listener.onSuccess(new KuzzleSecurityDocumentList(roles, result.getLong("total")));
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
   * Executes a search on roles using a set of filters
   * * /!\ There is a small delay between role creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a role that was just been created won’t be returned by this function.
   *
   * @param filters  - search filters (see ElasticSearch filters)
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void searchRoles(@NonNull final JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    searchRoles(filters, null, listener);
  }

  /**
   * Create a new role in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same role already exists: throw an error if sets to false.
   * Replace the existing role otherwise
   *
   * @param id       - new role ID
   * @param content  - new role rights definitions
   * @param options  - Optional parameters
   * @param listener - callback listener
   * @throws JSONException the json exception
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content, Options options, final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    String action = "createRole";

    if (id == null || content == null) {
      throw new IllegalArgumentException("KuzzleSecurity.createRole: cannot create a role without an ID or a content");
    }

    JSONObject data = new JSONObject().put("_id", id).put("body", content);

    if (options != null && options.isReplaceIfExist()) {
      action = "createOrReplaceRole";
    }

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs(action), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            listener.onSuccess(new KuzzleRole(KuzzleSecurity.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
          }
          catch(JSONException e) {
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
      this.kuzzle.query(buildQueryArgs(action), data, options);
    }
  }

  /**
   * Create a new role in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same role already exists: throw an error if sets to false.
   * Replace the existing role otherwise
   *
   * @param id       - new role ID
   * @param content  - new role rights definitions
   * @param listener - callback listener
   * @throws JSONException the json exception
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content, final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    createRole(id, content, null, listener);
  }

  /**
   * Create a new role in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same role already exists: throw an error if sets to false.
   * Replace the existing role otherwise
   *
   * @param id      - new role ID
   * @param content - new role rights definitions
   * @param options - Optional parameters
   * @throws JSONException the json exception
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content, Options options) throws JSONException {
    createRole(id, content, options, null);
  }

  /**
   * Create a new role in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same role already exists: throw an error if sets to false.
   * Replace the existing role otherwise
   *
   * @param id      - new role ID
   * @param content - new role rights definitions
   * @throws JSONException the json exception
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content) throws JSONException {
    createRole(id, content, null, null);
  }

  /**
   * Delete role.
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id       - ID of the role to delete
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteRole(@NonNull final String id, final Options options, final KuzzleResponseListener<String> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.deleteRole: cannot delete role without an ID");
    }

    JSONObject data = new JSONObject().put("_id", id);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs("deleteRole"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getString("_id"));
          }
          catch(JSONException e) {
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
      this.kuzzle.query(buildQueryArgs("deleteRole"), data, options);
    }

    return this;
  }

  /**
   * Delete role.
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id       - ID of the role to delete
   * @param listener - Callback listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteRole(@NonNull final String id, final KuzzleResponseListener<String> listener) throws JSONException {
    return deleteRole(id, null, listener);
  }

  /**
   * Delete role.
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id      - ID of the role to delete
   * @param options - Optional arguments
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteRole(@NonNull final String id, final Options options) throws JSONException {
    return deleteRole(id, options, null);
  }

  /**
   * Delete role.
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id - ID of the role to delete
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteRole(@NonNull final String id) throws JSONException {
    return deleteRole(id, null, null);
  }

  /**
   * Update role.
   *
   * @param id       the id
   * @param content  the content
   * @param options  the options
   * @param listener the listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateRole(@NonNull final String id, final JSONObject content, final Options options, final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.updateRole: cannot update role without an ID");
    }

    JSONObject data = new JSONObject().put("_id", id);
    data.put("body", content);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs("updateRole"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(new KuzzleRole(KuzzleSecurity.this.kuzzle, response.getJSONObject("result").getString("_id"), response.getJSONObject("result").getJSONObject("_source")));
          }
          catch(JSONException e) {
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
      this.kuzzle.query(buildQueryArgs("updateRole"), data, options);
    }

    return this;
  }

  /**
   * Update role.
   *
   * @param id       the id
   * @param content  the content
   * @param listener the listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateRole(@NonNull final String id, final JSONObject content, final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    return updateRole(id, content, null, listener);
  }

  /**
   * Update role.
   *
   * @param id      the id
   * @param content the content
   * @param options the options
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateRole(@NonNull final String id, final JSONObject content, final Options options) throws JSONException {
    return updateRole(id, content, options, null);
  }

  /**
   * Update role.
   *
   * @param id      the id
   * @param content the content
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateRole(@NonNull final String id, final JSONObject content) throws JSONException {
    return updateRole(id, content, null, null);
  }

  /**
   * Instantiate a new KuzzleRole object.
   *
   * @param id      - Role ID
   * @param content - Role content
   * @return a new KuzzleRole object
   * @throws JSONException the json exception
   */
  public KuzzleRole role(@NonNull final String id, final JSONObject content) throws JSONException {
    return new KuzzleRole(this.kuzzle, id, content);
  }

  /**
   * Instantiate a new KuzzleRole object.
   *
   * @param id - Role ID
   * @return a new KuzzleRole object
   * @throws JSONException the json exception
   */
  public KuzzleRole role(@NonNull final String id) throws JSONException {
    return new KuzzleRole(this.kuzzle, id, null);
  }

  /**
   * Get a specific profile from kuzzle
   * Takes an optional argument object with the following property:
   *
   * @param id       - ID of the profile to retrieve
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void fetchProfile(@NonNull final String id, final Options options, @NonNull final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.fetchProfile: cannot get 'null' profile");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.fetchProfile: a listener callback is required");
    }

    JSONObject data = new JSONObject().put("_id", id);

    this.kuzzle.query(buildQueryArgs("fetchProfile"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          JSONArray formattedPolicies = new JSONArray();
          JSONArray policies = result.getJSONObject("_source").getJSONArray("policies");

          for (int i = 0; i < policies.length(); i++) {
            JSONObject formattedPolicy = new JSONObject()
                .put("roleId", policies.getJSONObject(i).getString("roleId"));
            if (((JSONObject) policies.get(i)).has("restrictedTo")) {
              formattedPolicy.put("restrictedTo", policies.getJSONObject(i).getJSONArray("restrictedTo"));
            }
            if (((JSONObject) policies.get(i)).has("allowInternalIndex")) {
              formattedPolicy.put("allowInternalIndex", policies.getJSONObject(i).getBoolean("allowInternalIndex"));
            }
            formattedPolicies.put(formattedPolicy);
          }

          result.getJSONObject("_source").remove("policies");
          result.getJSONObject("_source").put("policies", formattedPolicies);

          listener.onSuccess(new KuzzleProfile(KuzzleSecurity.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
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
   * Get a specific profile from kuzzle
   * Takes an optional argument object with the following property:
   *
   * @param id       - ID of the profile to retrieve
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void fetchProfile(@NonNull final String id, @NonNull final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    fetchProfile(id, null, listener);
  }

  /**
   * Executes a search on profiles according to a filter
   * Takes an optional argument object with the following property:
   * /!\ There is a small delay between profile creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been created won’t be returned by this function.
   *
   * @param filters  - Search filters
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void searchProfiles(@NonNull JSONObject filters, final Options options, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    if (filters == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchProfiles: cannot perform a search on null filters");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchProfiles: a listener callback is required");
    }

    JSONObject data = new JSONObject().put("body", filters);

    this.kuzzle.query(buildQueryArgs("searchProfiles"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          JSONArray documents = result.getJSONArray("hits");
          int documentsLength = documents.length();
          ArrayList<AbstractKuzzleSecurityDocument> roles = new ArrayList<>();

          for (int i = 0; i < documentsLength; i++) {
            JSONObject document = documents.getJSONObject(i);
            roles.add(new KuzzleProfile(KuzzleSecurity.this.kuzzle, document.getString("_id"), document.getJSONObject("_source")));
          }

          listener.onSuccess(new KuzzleSecurityDocumentList(roles, result.getLong("total")));
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
   * Executes a search on profiles according to a filter
   * Takes an optional argument object with the following property:
   * /!\ There is a small delay between profile creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been created won’t be returned by this function.
   *
   * @param filters  - Search filters
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void searchProfiles(@NonNull JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    searchProfiles(filters, null, listener);
  }

  /**
   * Create a new profile in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same profile already exists: throw an error if sets to false.
   * Replace the existing profile otherwise
   *
   * @param id       - ID of the new profile
   * @param content  - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @param options  - Optional arguments
   * @param listener - Callback lisener
   * @throws JSONException the json exception
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONArray content, final Options options, final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    String action = "createProfile";

    if (id == null || content == null) {
      throw new IllegalArgumentException("KuzzleSecurity.createProfile: cannot create a profile with null ID or roles");
    }

    JSONObject data = new JSONObject().put("_id", id).put("body", new JSONObject().put("roles", content));

    if (options != null && options.isReplaceIfExist()) {
      action = "createOrReplaceProfile";
    }

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs(action), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            listener.onSuccess(new KuzzleProfile(KuzzleSecurity.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
          }
          catch(JSONException e) {
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
      this.kuzzle.query(buildQueryArgs(action), data, options);
    }
  }

  /**
   * Create a new profile in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same profile already exists: throw an error if sets to false.
   * Replace the existing profile otherwise
   *
   * @param id      - ID of the new profile
   * @param content - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @param options - Optional arguments
   * @throws JSONException the json exception
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONArray content, final Options options) throws JSONException {
    createProfile(id, content, options, null);
  }

  /**
   * Create a new profile in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same profile already exists: throw an error if sets to false.
   * Replace the existing profile otherwise
   *
   * @param id       - ID of the new profile
   * @param content  - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @param listener - Callback lisener
   * @throws JSONException the json exception
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONArray content, final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    createProfile(id, content, null, listener);
  }

  /**
   * Create a new profile in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same profile already exists: throw an error if sets to false.
   * Replace the existing profile otherwise
   *
   * @param id      - ID of the new profile
   * @param content - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @throws JSONException the json exception
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONArray content) throws JSONException {
    createProfile(id, content, null, null);
  }

  /**
   * Delete profile.
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id       - ID of the profile to delete
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteProfile(@NonNull final String id, final Options options, final KuzzleResponseListener<String> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.deleteProfile: cannot delete a profile with ID null");
    }

    JSONObject data = new JSONObject().put("_id", id);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs("deleteProfile"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getString("_id"));
          }
          catch(JSONException e) {
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
      this.kuzzle.query(buildQueryArgs("deleteProfile"), data, options);
    }

    return this;
  }

  /**
   * Delete profile.
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id       - ID of the profile to delete
   * @param listener - Callback listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteProfile(@NonNull final String id, final KuzzleResponseListener<String> listener) throws JSONException {
    return deleteProfile(id, null, listener);
  }

  /**
   * Delete profile.
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id      - ID of the profile to delete
   * @param options - Optional arguments
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteProfile(@NonNull final String id, final Options options) throws JSONException {
    return deleteProfile(id, options, null);
  }

  /**
   * Delete profile.
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id - ID of the profile to delete
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteProfile(@NonNull final String id) throws JSONException {
    return deleteProfile(id, null, null);
  }

  /**
   * Update profile.
   *
   * @param id       the id
   * @param content  the content
   * @param options  the options
   * @param listener the listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateProfile(@NonNull final String id, final JSONObject content, final Options options, final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.updateProfile: cannot update a profile with ID null");
    }

    JSONObject data = new JSONObject().put("_id", id);
    data.put("body", content);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs("updateProfile"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(new KuzzleProfile(KuzzleSecurity.this.kuzzle, response.getJSONObject("result").getString("_id"), response.getJSONObject("result").getJSONObject("_source")));
          }
          catch(JSONException e) {
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
      this.kuzzle.query(buildQueryArgs("updateProfile"), data, options);
    }

    return this;
  }

  /**
   * Update profile.
   *
   * @param id      the id
   * @param content the content
   * @param options the options
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateProfile(@NonNull final String id, final JSONObject content, final Options options) throws JSONException {
    return updateProfile(id, content, options, null);
  }

  /**
   * Update profile.
   *
   * @param id       the id
   * @param content  the content
   * @param listener the listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateProfile(@NonNull final String id, final JSONObject content, final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    return this.updateProfile(id, content, null, listener);
  }

  /**
   * Update profile.
   *
   * @param id      the id
   * @param content the content
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateProfile(@NonNull final String id, final JSONObject content) throws JSONException {
    return updateProfile(id, content, null, null);
  }

  /**
   * Instanciates a new KuzzleProfile object
   *
   * @param id      - Profile ID
   * @param content - Profile content
   * @return a new KuzzleProfile object
   * @throws JSONException the json exception
   */
  public KuzzleProfile profile(@NonNull final String id, final JSONObject content) throws JSONException {
    return new KuzzleProfile(this.kuzzle, id, content);
  }

  /**
   * Instanciates a new KuzzleProfile object
   *
   * @param id - Profile ID
   * @return a new KuzzleProfile object
   * @throws JSONException the json exception
   */
  public KuzzleProfile profile(@NonNull final String id) throws JSONException {
    return new KuzzleProfile(this.kuzzle, id, null);
  }

  /**
   * Get a specific user from kuzzle using its unique ID
   *
   * @param id       - User ID to retrieve
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void fetchUser(@NonNull final String id, final Options options, @NonNull final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.fetchUser: cannot get user with ID null");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.fetchUser: a callback listener is required");
    }

    JSONObject data = new JSONObject().put("_id", id);

    this.kuzzle.query(buildQueryArgs("fetchUser"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          listener.onSuccess(new KuzzleUser(KuzzleSecurity.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
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
   * Get a specific user from kuzzle using its unique ID
   *
   * @param id       - User ID to retrieve
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void fetchUser(@NonNull final String id, @NonNull final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    fetchUser(id, null, listener);
  }

  /**
   * Executes a search on user according to a filter
   * /!\ There is a small delay between user creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a user that was just been created won’t be returned by this function.
   *
   * @param filters  - Search filters
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void searchUsers(@NonNull JSONObject filters, final Options options, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {

    if (filters == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchUsers: cannot perform a search with null filters");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchUsers: a callback listener is required");
    }

    JSONObject data = new JSONObject().put("body", filters);

    this.kuzzle.query(buildQueryArgs("searchUsers"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          JSONArray documents = result.getJSONArray("hits");
          int documentsLength = documents.length();
          ArrayList<AbstractKuzzleSecurityDocument> roles = new ArrayList<>();

          for (int i = 0; i < documentsLength; i++) {
            JSONObject document = documents.getJSONObject(i);
            roles.add(new KuzzleUser(KuzzleSecurity.this.kuzzle, document.getString("_id"), document.getJSONObject("_source")));
          }

          listener.onSuccess(new KuzzleSecurityDocumentList(roles, result.getLong("total")));
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
   * Executes a search on user according to a filter
   * Takes an optional argument object with the following property:
   * /!\ There is a small delay between user creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a user that was just been created won’t be returned by this function.
   *
   * @param filters  - Search filters
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void searchUsers(@NonNull JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    searchUsers(filters, null, listener);
  }

  /**
   * Create a new user in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same user already exists: throw an error if sets to false.
   * Replace the existing user otherwise
   *
   * @param id       - ID of the user to create
   * @param content  - Should contain a 'profile' attribute with the profile ID
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content, final Options options, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    String action = options != null && options.isReplaceIfExist() ? "createOrReplaceUser" : "createUser";
    if (id == null || content == null) {
      throw new IllegalArgumentException("KuzzleSecurity.createUser: cannot create a user with a null ID or content");
    }

    JSONObject data = new JSONObject().put("_id", id).put("body", content);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs(action), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            listener.onSuccess(new KuzzleUser(KuzzleSecurity.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
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
    else {
      this.kuzzle.query(buildQueryArgs(action), data, options);
    }
  }

  /**
   * Create a new user in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same user already exists: throw an error if sets to false.
   * Replace the existing user otherwise
   *
   * @param id      - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @param options - Optional arguments
   * @throws JSONException the json exception
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content, final Options options) throws JSONException {
    createUser(id, content, options, null);
  }

  /**
   * Create a new user in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same user already exists: throw an error if sets to false.
   * Replace the existing user otherwise
   *
   * @param id       - ID of the user to create
   * @param content  - Should contain a 'profile' attribute with the profile ID
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    createUser(id, content, null, listener);
  }

  /**
   * Create a new user in Kuzzle.
   * Takes an optional argument object with the following property:
   * - replaceIfExist (boolean, default: false):
   * If the same user already exists: throw an error if sets to false.
   * Replace the existing user otherwise
   *
   * @param id      - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @throws JSONException the json exception
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content) throws JSONException {
    createUser(id, content, null, null);
  }

  /**
   * Create a new restricted user in Kuzzle.
   *
   * This function will create a new user. It is not usable to update an existing user.
   * This function allows anonymous users to create a "restricted" user with predefined rights.
   *
   * @param id       - ID of the user to create
   * @param content  - Should contain a 'profile' attribute with the profile ID
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void createRestrictedUser(@NonNull final String id, @NonNull final JSONObject content, final Options options, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    if (id == null || content == null) {
      throw new IllegalArgumentException("KuzzleSecurity.createRestrictedUser: cannot create a user with a null ID or content");
    }

    if (content.has("profileIds")) {
      throw new IllegalArgumentException("KuzzleSecurity.createRestrictedUser: cannot provide profileIds");
    }

    JSONObject data = new JSONObject().put("_id", id).put("body", content);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs("createRestrictedUser"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            JSONObject result = response.getJSONObject("result");
            listener.onSuccess(new KuzzleUser(KuzzleSecurity.this.kuzzle, result.getString("_id"), result.getJSONObject("_source")));
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
    else {
      this.kuzzle.query(buildQueryArgs("createRestrictedUser"), data, options);
    }
  }

  /**
   * Create a new restricted user in Kuzzle.
   *
   * This function will create a new user. It is not usable to update an existing user.
   * This function allows anonymous users to create a "restricted" user with predefined rights.
   *
   * @param id      - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @param options - Optional arguments
   * @throws JSONException the json exception
   */
  public void createRestrictedUser(@NonNull final String id, @NonNull final JSONObject content, final Options options) throws JSONException {
    createRestrictedUser(id, content, options, null);
  }

  /**
   * Create a new restricted user in Kuzzle.
   *
   * This function will create a new user. It is not usable to update an existing user.
   * This function allows anonymous users to create a "restricted" user with predefined rights.
   *
   * @param id       - ID of the user to create
   * @param content  - Should contain a 'profile' attribute with the profile ID
   * @param listener - Callback listener
   * @throws JSONException the json exception
   */
  public void createRestrictedUser(@NonNull final String id, @NonNull final JSONObject content, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    createRestrictedUser(id, content, null, listener);
  }

  /**
   * Create a new restricted user in Kuzzle.
   *
   * This function will create a new user. It is not usable to update an existing user.
   * This function allows anonymous users to create a "restricted" user with predefined rights.
   *
   * @param id      - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @throws JSONException the json exception
   */
  public void createRestrictedUser(@NonNull final String id, @NonNull final JSONObject content) throws JSONException {
    createRestrictedUser(id, content, null, null);
  }

  /**
   * Delete user.
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id       - ID of the user to delete
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteUser(@NonNull final String id, final Options options, final KuzzleResponseListener<String> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.deleteUser: cannot delete user with ID null");
    }

    JSONObject data = new JSONObject().put("_id", id);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs("deleteUser"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getString("_id"));
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
    else {
      this.kuzzle.query(buildQueryArgs("deleteUser"), data, options);
    }

    return this;
  }

  /**
   * Delete user.
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id      - ID of the user to delete
   * @param options - Optional arguments
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteUser(@NonNull final String id, final Options options) throws JSONException {
    return deleteUser(id, options, null);
  }

  /**
   * Delete user.
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id       - ID of the user to delete
   * @param listener - Callback listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteUser(@NonNull final String id, final KuzzleResponseListener<String> listener) throws JSONException {
    return deleteUser(id, null, listener);
  }

  /**
   * Delete user.
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id - ID of the user to delete
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity deleteUser(@NonNull final String id) throws JSONException {
    return deleteUser(id, null, null);
  }

  /**
   * Update user.
   *
   * @param id       the id
   * @param content  the content
   * @param options  the options
   * @param listener the listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateUser(@NonNull final String id, final JSONObject content, final Options options, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.updateUser: cannot update user without an ID");
    }

    JSONObject data = new JSONObject().put("_id", id);
    data.put("body", content);

    if (listener != null) {
      this.kuzzle.query(buildQueryArgs("updateUser"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(new KuzzleUser(KuzzleSecurity.this.kuzzle, response.getJSONObject("result").getString("_id"), response.getJSONObject("result").getJSONObject("_source")));
          }
          catch(JSONException e) {
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
      this.kuzzle.query(buildQueryArgs("updateUser"), data, options);
    }

    return this;
  }

  /**
   * Update user.
   *
   * @param id       the id
   * @param content  the content
   * @param listener the listener
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateUser(@NonNull final String id, final JSONObject content, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    return updateUser(id, content, null, listener);
  }

  /**
   * Update user.
   *
   * @param id      the id
   * @param content the content
   * @param options the options
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateUser(@NonNull final String id, final JSONObject content, final Options options) throws JSONException {
    return updateUser(id, content, options, null);
  }

  /**
   * Update user.
   *
   * @param id      the id
   * @param content the content
   * @return KuzzleSecurity this object
   * @throws JSONException the json exception
   */
  public KuzzleSecurity updateUser(@NonNull final String id, final JSONObject content) throws JSONException {
    return updateUser(id, content, null, null);
  }

  /**
   * Instanciates a new KuzzleUser object
   *
   * @param id      - New user ID
   * @param content - User content
   * @return a new KuzzleUser object
   * @throws JSONException the json exception
   */
  public KuzzleUser user(@NonNull final String id, final JSONObject content) throws JSONException {
    return new KuzzleUser(this.kuzzle, id, content);
  }

  /**
   * Instanciates a new KuzzleUser object
   *
   * @param id - New user ID
   * @return a new KuzzleUser object
   * @throws JSONException the json exception
   */
  public KuzzleUser user(@NonNull final String id) throws JSONException {
    return new KuzzleUser(this.kuzzle, id, null);
  }

  /**
   * Tells whether an action is allowed, denied or conditional based on the rights
   * policies provided as the first argument. An action is defined as a couple of
   * action and controller (mandatory), plus an index and a collection(optional).
   * @param policies
   * @param controller
   * @param action
   * @return the KuzzleSecurityObject
   */
  public Policies isActionAllowed(@NonNull final JSONArray policies, @NonNull final String controller, @NonNull final String action) {
    return this.isActionAllowed(policies, controller, action, null, null);
  }

  /**
   * Tells whether an action is allowed, denied or conditional based on the rights
   * policies provided as the first argument. An action is defined as a couple of
   * action and controller (mandatory), plus an index and a collection(optional).
   *
   * @param policies
   * @param controller
   * @param action
   * @param index
   * @return the KuzzleSecurityObject
   */
  public Policies isActionAllowed(@NonNull final JSONArray policies, @NonNull final String controller, @NonNull  final String action, final String index) {
    return this.isActionAllowed(policies, controller, action, index, null);
  }

  /**
   * Tells whether an action is allowed, denied or conditional based on the rights
   * policies provided as the first argument. An action is defined as a couple of
   * action and controller (mandatory), plus an index and a collection(optional).
   *
   * @param policies
   * @param controller
   * @param action
   * @param index
   * @param collection
   * @return the KuzzleSecurityObject
   */
  public Policies isActionAllowed(@NonNull final JSONArray policies, @NonNull final String controller, @NonNull final String action, final String index, final String collection) {
    if (policies == null) {
      throw new IllegalArgumentException("KuzzleSecurity.isActionAllowed: policies are mandatory.");
    }
    if (controller == null || controller.isEmpty()) {
      throw new IllegalArgumentException("KuzzleSecurity.isActionAllowed: controller is mandatory.");
    }
    if (action == null || action.isEmpty()) {
      throw new IllegalArgumentException("KuzzleSecurity.isActionAllowed: action is mandatory.");
    }

    JSONArray filteredPolicies;
    try {
      filteredPolicies = filterPolicy(policies, "controller", controller);
      filteredPolicies = filterPolicy(filteredPolicies, "action", action);
      filteredPolicies = filterPolicy(filteredPolicies, "index", index);
      filteredPolicies = filterPolicy(filteredPolicies, "collection", collection);
      for (int i = 0; i < filteredPolicies.length(); i++) {
        if (filteredPolicies.getJSONObject(i).getString("value").equals(Policies.allowed.toString())) {
          return Policies.allowed;
        }
      }
      for (int i = 0; i < filteredPolicies.length(); i++) {
        if (filteredPolicies.getJSONObject(i).getString("value").equals(Policies.conditional.toString())) {
          return Policies.conditional;
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return Policies.denied;
  }

  private JSONArray  filterPolicy(final JSONArray policies, final String attr, final String attrInput) throws JSONException {
    JSONArray filteredPolicies = new JSONArray();
    for (int i = 0; i < policies.length(); i++) {
      JSONObject policy = policies.getJSONObject(i);
      String attrObject = policy.getString(attr);
      if (attrObject.equals(attrInput) || attrObject.equals("*")) {
        filteredPolicies.put(policy);
      }
    }
    return filteredPolicies;
  }


  /**
   * Gets the rights array of a given user.
   *
   * @param id
   * @param listener
   * @return the KuzzleSecurity instance
   */
  public KuzzleSecurity getUserRights(@NonNull final String id, @NonNull final KuzzleResponseListener<JSONArray> listener) {
    return getUserRights(id, null, listener);
  }

  /**
   * Gets the rights array of a given user.
   *
   * @param id
   * @param options
   * @param listener
   * @return the KuzzleSecurity instance
   */
  public KuzzleSecurity getUserRights(@NonNull final String id, final Options options, @NonNull final KuzzleResponseListener<JSONArray> listener) {
    if (id == null || id.isEmpty()) {
      throw new IllegalArgumentException("KuzzleSecurity.getUserRights: id is mandatory.");
    }
    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.getUserRights: listener is mandatory.");
    }
    try {
      JSONObject data = new JSONObject()
          .put("_id", id);
      kuzzle.query(buildQueryArgs("getUserRights"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          try {
            listener.onSuccess(response.getJSONObject("result").getJSONArray("hits"));
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

}
