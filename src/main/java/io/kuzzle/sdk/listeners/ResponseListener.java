package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

/**
 * The interface Response listener.
 */
public interface ResponseListener<T> {
  /**
   * On success.
   *
   * @param response the object
   */
  void onSuccess(T response);

  /**
   * On error.
   *
   * @param error the error
   */
  void onError(JSONObject error);
}
