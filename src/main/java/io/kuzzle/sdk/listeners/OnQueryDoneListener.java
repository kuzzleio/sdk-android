package io.kuzzle.sdk.listeners;

import org.json.JSONObject;
import android.util.Log;
import java.util.Arrays;

public abstract class OnQueryDoneListener {
  public abstract void onSuccess(JSONObject response);
  public void onError(JSONObject error) {
    if (error != null) {
      Log.e(ResponseListener.TAG_API_ERROR, "Default error handler invoked: " + error.toString());
    } else {
      Log.e(ResponseListener.TAG_API_ERROR, Arrays.toString(Thread.currentThread().getStackTrace()));
    }
  }
}
