package io.kuzzle.test.testUtils;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleMemoryStorage;
import io.kuzzle.sdk.util.memoryStorage.Action;

public class KuzzleMemoryStorageExtend extends KuzzleMemoryStorage {
  public KuzzleMemoryStorageExtend(@NonNull Kuzzle kuzzle) {
    super(kuzzle);
  }

  public KuzzleMemoryStorage send(final Action action, final JSONObject query) {
    return super.send(action, query);
  }
}
