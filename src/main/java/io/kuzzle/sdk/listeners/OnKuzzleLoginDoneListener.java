package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

public interface OnKuzzleLoginDoneListener {
  void onSuccess(JSONObject result);
  void onError(JSONObject error);
}
