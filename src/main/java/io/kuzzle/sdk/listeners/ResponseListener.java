package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

/**
 * The interface Response listener.
 */
public interface ResponseListener {
  /**
   * On success.
   *
   * @param object the object
   */
  void onSuccess(JSONObject object);

  /**
   * On error.
   *
   * @param error the error
   */
  void onError(JSONObject error);
}
