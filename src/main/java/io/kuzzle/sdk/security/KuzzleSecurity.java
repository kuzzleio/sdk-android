package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedList;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.KuzzleSecurityDocumentList;


public class KuzzleSecurity {
  private final Kuzzle kuzzle;

  public KuzzleSecurity(final Kuzzle kuzzle) {
    this.kuzzle = kuzzle;
  }

  /**
   * Helper function meant to easily build the first Kuzzle.query() argument
   *
   * @param action - Security controller action name
   * @return JSONObject - Kuzzle.query() 1st argument object
   * @throws JSONException
   */
  protected Kuzzle.QueryArgs buildQueryArgs(@NonNull final String action) throws JSONException {
    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.action = action;
    args.controller = "security";
    return args;
  }

  /**
   * Retrieves a single Role using its unique Role ID
   *
   * @param id - unique role ID
   * @param options - optional query arguments
   * @param listener - response callback
   */
  public void getRole(@NonNull final String id, KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleRole> listener) {
    JSONObject data;

    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.getRole: a role ID is required");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.getRole: a listener is required");
    }

    try {
      data = new JSONObject().put("_id", id);
      this.kuzzle.query(buildQueryArgs("getRole"), data, options, new OnQueryDoneListener() {
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
   * @param id - unique role ID
   * @param listener - response callback
   */
  public void getRole(@NonNull final String id, @NonNull final KuzzleResponseListener<KuzzleRole> listener) {
    getRole(id, null, listener);
  }


  /**
   * Executes a search on roles using a set of filters
   *
   *  * /!\ There is a small delay between role creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a role that was just been created won’t be returned by this function.
   *
   * @param filters - search filters (see ElasticSearch filters)
   * @param options - Optional query arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void searchRoles(@NonNull final JSONObject filters, final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
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
          LinkedList<AbstractKuzzleSecurityDocument> roles = new LinkedList<>();

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
   *
   *  * /!\ There is a small delay between role creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a role that was just been created won’t be returned by this function.
   *
   * @param filters - search filters (see ElasticSearch filters)
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void searchRoles(@NonNull final JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    searchRoles(filters, null, listener);
  }

  /**
   * Create a new role in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same role already exists: throw an error if sets to false.
   *        Replace the existing role otherwise
   *
   * @param id - new role ID
   * @param content - new role rights definitions
   * @param options - Optional parameters
   * @param listener - callback listener
   * @throws JSONException
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content, KuzzleOptions options, final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    String action = "createRole";

    if (id == null || content == null) {
      throw new IllegalArgumentException("KuzzleSecurity.createRole: cannot create a role without an ID or a content");
    }

    JSONObject data = new JSONObject().put("_id", id).put("body", content);

    if (options != null && options.isUpdateIfExists()) {
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
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same role already exists: throw an error if sets to false.
   *        Replace the existing role otherwise
   *
   * @param id - new role ID
   * @param content - new role rights definitions
   * @param listener - callback listener
   * @throws JSONException
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content, final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    createRole(id, content, null, listener);
  }

  /**
   * Create a new role in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same role already exists: throw an error if sets to false.
   *        Replace the existing role otherwise
   *
   * @param id - new role ID
   * @param content - new role rights definitions
   * @param options - Optional parameters
   * @throws JSONException
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content, KuzzleOptions options) throws JSONException {
    createRole(id, content, options, null);
  }

  /**
   * Create a new role in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same role already exists: throw an error if sets to false.
   *        Replace the existing role otherwise
   *
   * @param id - new role ID
   * @param content - new role rights definitions
   * @throws JSONException
   */
  public void createRole(@NonNull final String id, @NonNull final JSONObject content) throws JSONException {
    createRole(id, content, null, null);
  }

  /**
   * Delete role.
   *
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id - ID of the role to delete
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void deleteRole(@NonNull final String id, final KuzzleOptions options, final KuzzleResponseListener<String> listener) throws JSONException {
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
  }

  /**
   * Delete role.
   *
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id - ID of the role to delete
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void deleteRole(@NonNull final String id, final KuzzleResponseListener<String> listener) throws JSONException {
    deleteRole(id, null, listener);
  }

  /**
   * Delete role.
   *
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id - ID of the role to delete
   * @param options - Optional arguments
   * @throws JSONException
   */
  public void deleteRole(@NonNull final String id, final KuzzleOptions options) throws JSONException {
    deleteRole(id, options, null);
  }

  /**
   * Delete role.
   *
   * There is a small delay between role deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a role that was just been delete will be returned by this function
   *
   * @param id - ID of the role to delete
   * @throws JSONException
   */
  public void deleteRole(@NonNull final String id) throws JSONException {
    deleteRole(id, null, null);
  }

  /**
   * Instantiate a new KuzzleRole object.
   *
   * @param id - Role ID
   * @param content - Role content
   * @return a new KuzzleRole object
   * @throws JSONException
   */
  public KuzzleRole roleFactory(@NonNull final String id, final JSONObject content) throws JSONException {
    return new KuzzleRole(this.kuzzle, id, content);
  }

  /**
   * Instantiate a new KuzzleRole object.
   *
   * @param id - Role ID
   * @return a new KuzzleRole object
   * @throws JSONException
   */
  public KuzzleRole roleFactory(@NonNull final String id) throws JSONException {
    return new KuzzleRole(this.kuzzle, id, null);
  }

  /**
   * Get a specific profile from kuzzle
   *
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *
   * @param id - ID of the profile to retrieve
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void getProfile(@NonNull final String id, final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.getProfile: cannot get 'null' profile");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.getProfile: a listener callback is required");
    }

    JSONObject data = new JSONObject().put("_id", id);
    // TODO: handle the options.hydrate parameter

    this.kuzzle.query(buildQueryArgs("getProfile"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
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
   *
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *
   * @param id - ID of the profile to retrieve
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void getProfile(@NonNull final String id, @NonNull final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    getProfile(id, null, listener);
  }

  /**
   * Executes a search on profiles according to a filter
   *
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *         Because hydrate need to fetch all related KuzzleRole object, leave hydrate to true will have a performance cost
   *
   * /!\ There is a small delay between profile creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been created won’t be returned by this function.
   *
   * @param filters - Search filters
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void searchProfiles(@NonNull JSONObject filters, final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
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
          LinkedList<AbstractKuzzleSecurityDocument> roles = new LinkedList<>();

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
   *
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *         Because hydrate need to fetch all related KuzzleRole object, leave hydrate to true will have a performance cost
   *
   * /!\ There is a small delay between profile creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been created won’t be returned by this function.
   *
   * @param filters - Search filters
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void searchProfiles(@NonNull JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    searchProfiles(filters, null, listener);
  }

  /**
   * Create a new profile in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same profile already exists: throw an error if sets to false.
   *        Replace the existing profile otherwise
   *
   * @param id - ID of the new profile
   * @param content - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @param options - Optional arguments
   * @param listener - Callback lisener
   * @throws JSONException
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONObject content, final KuzzleOptions options, final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    String action = "createProfile";

    if (id == null || content == null) {
      throw new IllegalArgumentException("KuzzleSecurity.createProfile: cannot create a profile with null ID or roles");
    }

    JSONObject data = new JSONObject().put("_id", id).put("body", content);

    if (options != null && options.isUpdateIfExists()) {
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
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same profile already exists: throw an error if sets to false.
   *        Replace the existing profile otherwise
   *
   * @param id - ID of the new profile
   * @param content - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @param options - Optional arguments
   * @throws JSONException
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONObject content, final KuzzleOptions options) throws JSONException {
    createProfile(id, content, options, null);
  }

  /**
   * Create a new profile in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same profile already exists: throw an error if sets to false.
   *        Replace the existing profile otherwise
   *
   * @param id - ID of the new profile
   * @param content - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @param listener - Callback lisener
   * @throws JSONException
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONObject content, final KuzzleResponseListener<KuzzleProfile> listener) throws JSONException {
    createProfile(id, content, null, listener);
  }

  /**
   * Create a new profile in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same profile already exists: throw an error if sets to false.
   *        Replace the existing profile otherwise
   *
   * @param id - ID of the new profile
   * @param content - Should contain a 'roles' attributes containing the roles referenced by this profile
   * @throws JSONException
   */
  public void createProfile(@NonNull final String id, @NonNull final JSONObject content) throws JSONException {
    createProfile(id, content, null, null);
  }

  /**
   * Delete profile.
   *
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id - ID of the profile to delete
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void deleteProfile(@NonNull final String id, final KuzzleOptions options, final KuzzleResponseListener<String> listener) throws JSONException {
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
  }

  /**
   * Delete profile.
   *
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id - ID of the profile to delete
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void deleteProfile(@NonNull final String id, final KuzzleResponseListener<String> listener) throws JSONException {
    deleteProfile(id, null, listener);
  }

  /**
   * Delete profile.
   *
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id - ID of the profile to delete
   * @param options - Optional arguments
   * @throws JSONException
   */
  public void deleteProfile(@NonNull final String id, final KuzzleOptions options) throws JSONException {
    deleteProfile(id, options, null);
  }

  /**
   * Delete profile.
   *
   * There is a small delay between profile deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a profile that was just been delete will be returned by this function
   *
   * @param id - ID of the profile to delete
   * @throws JSONException
   */
  public void deleteProfile(@NonNull final String id) throws JSONException {
    deleteProfile(id, null, null);
  }

  /**
   * Instanciates a new KuzzleProfile object
   *
   * @param id - Profile ID
   * @param content - Profile content
   * @return a new KuzzleProfile object
   * @throws JSONException
   */
  public KuzzleProfile profileFactory(@NonNull final String id, final JSONObject content) throws JSONException {
    return new KuzzleProfile(this.kuzzle, id, content);
  }

  /**
   * Instanciates a new KuzzleProfile object
   *
   * @param id - Profile ID
   * @return a new KuzzleProfile object
   * @throws JSONException
   */
  public KuzzleProfile profileFactory(@NonNull final String id) throws JSONException {
    return new KuzzleProfile(this.kuzzle, id, null);
  }

  /**
   * Get a specific user from kuzzle using its unique ID
   *
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *
   * @param id - User ID to retrieve
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void getUser(@NonNull final String id, final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    if (id == null) {
      throw new IllegalArgumentException("KuzzleSecurity.getUser: cannot get user with ID null");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.getUser: a callback listener is required");
    }

    JSONObject data = new JSONObject().put("_id", id);
    // TODO: manage the "hydrate" options

    this.kuzzle.query(buildQueryArgs("getUser"), data, options, new OnQueryDoneListener() {
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
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *
   * @param id - User ID to retrieve
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void getUser(@NonNull final String id, @NonNull final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    getUser(id, null, listener);
  }

  /**
   * Executes a search on user according to a filter
   *
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *         Because hydrate need to fetch all related KuzzleRole object, leave hydrate to true will have a performance cost
   *
   * /!\ There is a small delay between user creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a user that was just been created won’t be returned by this function.
   *
   * @param filters - Search filters
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void searchUsers(@NonNull JSONObject filters, final KuzzleOptions options, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    boolean hydrate = options != null ? options.isHydrated() : true;

    if (filters == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchUsers: cannot perform a search with null filters");
    }

    if (listener == null) {
      throw new IllegalArgumentException("KuzzleSecurity.searchUsers: a callback listener is required");
    }

    JSONObject data = new JSONObject().put("body", filters);
    data.getJSONObject("body").put("hydrate", hydrate);

    this.kuzzle.query(buildQueryArgs("searchUsers"), data, options, new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {
        try {
          JSONObject result = response.getJSONObject("result");
          JSONArray documents = result.getJSONArray("hits");
          int documentsLength = documents.length();
          LinkedList<AbstractKuzzleSecurityDocument> roles = new LinkedList<>();

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
   *
   * Takes an optional argument object with the following property:
   *    - hydrate (boolean, default: true):
   *         if is set to false, return a list id in role instead of KuzzleRole.
   *         Because hydrate need to fetch all related KuzzleRole object, leave hydrate to true will have a performance cost
   *
   * /!\ There is a small delay between user creation and their existence in our persistent search layer,
   * usually a couple of seconds.
   * That means that a user that was just been created won’t be returned by this function.
   *
   * @param filters - Search filters
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void searchUsers(@NonNull JSONObject filters, @NonNull final KuzzleResponseListener<KuzzleSecurityDocumentList> listener) throws JSONException {
    searchUsers(filters, null, listener);
  }

  /**
   * Create a new user in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same user already exists: throw an error if sets to false.
   *        Replace the existing user otherwise
   *
   * @param id - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content, final KuzzleOptions options, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    String action = options != null && options.isUpdateIfExists() ? "createOrReplaceUser" : "createUser";
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
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same user already exists: throw an error if sets to false.
   *        Replace the existing user otherwise
   *
   * @param id - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @param options - Optional arguments
   * @throws JSONException
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content, final KuzzleOptions options) throws JSONException {
    createUser(id, content, options, null);
  }

  /**
   * Create a new user in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same user already exists: throw an error if sets to false.
   *        Replace the existing user otherwise
   *
   * @param id - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content, final KuzzleResponseListener<KuzzleUser> listener) throws JSONException {
    createUser(id, content, null, listener);
  }

  /**
   * Create a new user in Kuzzle.
   *
   * Takes an optional argument object with the following property:
   *    - replaceIfExist (boolean, default: false):
   *        If the same user already exists: throw an error if sets to false.
   *        Replace the existing user otherwise
   *
   * @param id - ID of the user to create
   * @param content - Should contain a 'profile' attribute with the profile ID
   * @throws JSONException
   */
  public void createUser(@NonNull final String id, @NonNull final JSONObject content) throws JSONException {
    createUser(id, content, null, null);
  }

  /**
   * Delete user.
   *
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id - ID of the user to delete
   * @param options - Optional arguments
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void deleteUser(@NonNull final String id, final KuzzleOptions options, final KuzzleResponseListener<String> listener) throws JSONException {
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
  }

  /**
   * Delete user.
   *
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id - ID of the user to delete
   * @param options - Optional arguments
   * @throws JSONException
   */
  public void deleteUser(@NonNull final String id, final KuzzleOptions options) throws JSONException {
    deleteUser(id, options, null);
  }

  /**
   * Delete user.
   *
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id - ID of the user to delete
   * @param listener - Callback listener
   * @throws JSONException
   */
  public void deleteUser(@NonNull final String id, final KuzzleResponseListener<String> listener) throws JSONException {
    deleteUser(id, null, listener);
  }

  /**
   * Delete user.
   *
   * There is a small delay between user deletion and their deletion in our advanced search layer,
   * usually a couple of seconds.
   * That means that a user that was just been delete will be returned by this function
   *
   * @param id - ID of the user to delete
   * @throws JSONException
   */
  public void deleteUser(@NonNull final String id) throws JSONException {
    deleteUser(id, null, null);
  }

  /**
   * Instanciates a new KuzzleUser object
   * @param id - New user ID
   * @param content - User content
   * @return a new KuzzleUser object
   * @throws JSONException
   */
  public KuzzleUser userFactory(@NonNull final String id, final JSONObject content) throws JSONException {
    return new KuzzleUser(this.kuzzle, id, content);
  }

  /**
   * Instanciates a new KuzzleUser object
   * @param id - New user ID
   * @return a new KuzzleUser object
   * @throws JSONException
   */
  public KuzzleUser userFactory(@NonNull final String id) throws JSONException {
    return new KuzzleUser(this.kuzzle, id, null);
  }
}
