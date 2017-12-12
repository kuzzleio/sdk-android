package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

/**
 * The interface Response listener.
 */
public abstract class ResponseListener<T> {
  /**
   * On success.
   *
   * @param response Raw Kuzzle API response
   */
  public abstract void onSuccess(T response);

  /**
   * On error.
   *
   * @param error Raw Kuzzle API error content
   */
  public void onError(JSONObject error) {
    if (error != null) {
      System.err.println("Default error handler invoked: " + error.toString());
    } else {
      new Throwable().printStackTrace(System.err);
    }
  }
}
