package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

public abstract class OnQueryDoneListener {
  public abstract void onSuccess(JSONObject response);
  public void onError(JSONObject error) {
    if (error != null) {
      System.err.println("Default error handler invoked: " + error.toString());
    } else {
      new Throwable().printStackTrace(System.err);
    }
  }
}
