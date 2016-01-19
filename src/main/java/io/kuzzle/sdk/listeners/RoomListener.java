package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

import io.kuzzle.sdk.responses.KuzzNotificationResponse;

public interface RoomListener {
  void onNotificationReceived(KuzzNotificationResponse notification);
  void onNotificationError(JSONObject error);
}
