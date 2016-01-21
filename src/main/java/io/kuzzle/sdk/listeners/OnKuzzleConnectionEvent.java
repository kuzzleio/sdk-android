package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

public interface OnKuzzleConnectionEvent {
  void onSuccess(JSONObject success);
  void onError(JSONObject error);
}
