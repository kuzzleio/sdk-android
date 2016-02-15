package io.kuzzle.sdk.security;


import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;

/**
 * This class handles roles management in Kuzzle
 */
public class KuzzleRole extends AbstractKuzzleSecurityDocument {
  public KuzzleRole(final Kuzzle kuzzle, @NonNull final String id, final JSONObject content) throws JSONException {
    super(kuzzle, id, content);
    this.deleteActionName = "deleteRole";
  }

  /**
   * Save this role in Kuzzle
   *
   * @param options - Optional configuration
   * @param listener - Optional callback listener
   * @throws JSONException
   */
  public void save(final KuzzleOptions options, final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    JSONObject data = this.serialize();

    if (listener != null) {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceRole"), data, options, new OnQueryDoneListener() {
        @Override
        public void onSuccess(JSONObject response) {
          listener.onSuccess(KuzzleRole.this);
        }

        @Override
        public void onError(JSONObject error) {
          listener.onError(error);
        }
      });
    }
    else {
      this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceRole"), data, options);
    }
  }

  /**
   * Save this role in Kuzzle
   *
   * @param listener - Optional callback listener
   * @throws JSONException
   */
  public void save(final KuzzleResponseListener<KuzzleRole> listener) throws JSONException {
    this.save(null, listener);
  }

  /**
   * Save this role in Kuzzle
   *
   * @param options - Optional configuration
   * @throws JSONException
   */
  public void save(final KuzzleOptions options) throws JSONException {
    this.save(options, null);
  }

  /**
   * Save this role in Kuzzle
   *
   * @throws JSONException
   */
  public void save() throws JSONException {
    this.save(null, null);
  }
}
