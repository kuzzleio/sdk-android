package io.kuzzle.sdk.listeners;

import org.json.JSONObject;
import android.util.Log;
import java.util.Arrays;

public abstract class OnQueryDoneListener {
  public abstract void onSuccess(JSONObject response);
  public void onError(JSONObject error) {
    if (error != null) {
      Log.e(ResponseListener.LOG_TAG, "Default error handler invoked: " + error.toString());
    } 
  }
}
