package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

import io.kuzzle.sdk.core.KuzzleDocument;

/**
 * The interface Response listener.
 */
public interface ResponseListener {
  /**
   * On success.
   *
   * @param object the object
   * @throws Exception the exception
   */
  void onSuccess(JSONObject object) throws Exception;

  /**
   * On error.
   *
   * @param error the error
   * @throws Exception the exception
   */
  void onError(JSONObject error) throws Exception;
}
