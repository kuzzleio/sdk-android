package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

public interface OnLoginDoneListener {
  void onSuccess(JSONObject result);
  void onError(JSONObject error);
}
