package io.kuzzle.sdk.util;

import org.json.JSONObject;

public interface KuzzleQueueFilter {
  boolean filter(JSONObject object);
}
