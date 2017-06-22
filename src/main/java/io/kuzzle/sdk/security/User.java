package io.kuzzle.sdk.security;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.ArrayList;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.security.Profile;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles users management in Kuzzle
 */
public class User extends AbstractSecurityDocument {
  /**
   * The Profiles Ids List.
   */
  private ArrayList<String> profileIds = new ArrayList<>();

  private JSONObject credentials = new JSONObject();

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
        JSONArray profiles = content.getJSONArray("profileIds");

        for (int i = 0; i < profiles.length(); i++) {
          this.profileIds.add(profiles.getString(i));
        }
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

    this.profileIds.clear();
    this.profileIds.addAll(Arrays.asList(profileIds));

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

    this.profileIds.add(profile);

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
  public User replace(final Options options, final ResponseListener<User> listener) throws JSONException {
    JSONObject data = this.serialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("replaceUser"), data, options, new OnQueryDoneListener() {
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
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("replaceUser"), data, options);
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
  public User replace(final ResponseListener<User> listener) throws JSONException {
    return replace(null, listener);
  }

  /**
   * Save this user in Kuzzle
   *
   * @param options - Optional arguments
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User replace(final Options options) throws JSONException {
    return replace(options, null);
  }

  /**
   * Save this user in Kuzzle
   *
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User replace() throws JSONException {
    return replace(null, null);
  }

  /**
   * Create this user into Kuzzle.
   *
   * @param options  - Optional arguments
   * @param listener - Callback listener
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User create(final Options options, final ResponseListener<User> listener) throws JSONException {
    JSONObject data = this.creationSerialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createUser"), data, options, new OnQueryDoneListener() {
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
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createUser"), data, options);
    }

    return this;
  }

  /**
   * Create this user into Kuzzle.
   *
   * @param listener - Callback listener
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User create(final ResponseListener<User> listener) throws JSONException {
    return create(null, listener);
  }

  /**
   * Create this user into Kuzzle.
   *
   * @param options - Optional arguments
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User create(final Options options) throws JSONException {
    return create(options, null);
  }

  /**
   * Create this user into Kuzzle.
   *
   * @return this kuzzle user
   * @throws JSONException the json exception
   */
  public User create() throws JSONException {
    return create(null, null);
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

    if (this.profileIds.size() > 0) {
      content.put("profileIds", new JSONArray(this.profileIds));
    }

    data.put("body", content);

    return data;
  }

  /**
   * Return a JSONObject representing a serialized version of this object
   *
   * @return serialized version of this object
   * @throws JSONException the json exception
   */
  public JSONObject creationSerialize() throws JSONException {
    JSONObject
      data = new JSONObject().put("_id", this.id),
      body = new JSONObject(),
      content = new JSONObject(this.content.toString()),
      credentials = new JSONObject(this.credentials.toString());

    if (this.profileIds.size() > 0) {
      content.put("profileIds", new JSONArray(this.profileIds));
    }

    body.put("content", content);
    body.put("credentials", credentials);
    data.put("body", body);

    return data;
  }

  /**
   * Returns the associated profiles
   *
   * @return an array of strings
   */
  public String[] getProfileIds() {
    return this.profileIds.toArray(new String[0]);
  }

  /**
   * Resolves to the associated profiles as Profile objects
   */
  public void getProfiles(final ResponseListener<Profile[]> listener) throws JSONException {
    getProfiles(null, listener);
  }

  public void getProfiles(final Options options, final ResponseListener<Profile[]> listener) throws JSONException {
    if (listener == null) {
      throw new IllegalArgumentException("User.getProfiles: a valid ResponseListener object is required");
    }

    final Profile[] profiles = new Profile[this.profileIds.size()];

    if (this.profileIds.size() == 0) {
      listener.onSuccess(profiles);
      return;
    }

    // using an array to allow these variables to be final
    // while keeping the possibility to change their value
    final int[] fetched = {0};
    final boolean[] errored = {false};

    for (int i = 0; i < this.profileIds.size(); i++) {
      this.kuzzleSecurity.fetchProfile(this.profileIds.get(i), options, new ResponseListener<Profile>() {
        @Override
        public void onSuccess(Profile response) {
          profiles[fetched[0]] = response;
          fetched[0]++;

          if (fetched[0] == User.this.profileIds.size()) {
            listener.onSuccess(profiles);
          }
        }

        @Override
        public void onError(JSONObject error) {
          // prevents triggering the listener multiple times
          if (errored[0]) {
            return;
          }

          errored[0] = true;
          listener.onError(error);
        }
      });
    }
  }

  /**
   * @param credentials the user credentials
   */
  public User setCredentials(JSONObject credentials) {
    this.credentials = credentials;

    return this;
  }
}
