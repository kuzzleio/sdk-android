package io.kuzzle.sdk.listeners;

import org.json.JSONObject;
import android.util.Log;
import java.util.Arrays;

/**
 * The interface Response listener.
 */
public abstract class ResponseListener<T> {
  public static final String TAG_API_ERROR = "Kuzzle SDK Error";

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
      Log.e(TAG_API_ERROR, "Default error handler invoked: " + error.toString());
    } else {
      Log.e(TAG_API_ERROR, Arrays.toString(Thread.currentThread().getStackTrace()));
    }
  }
}
