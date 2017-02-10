package io.kuzzle.test.testUtils;

import android.support.annotation.NonNull;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.MemoryStorage;
import io.kuzzle.sdk.util.KuzzleJSONObject;
import io.kuzzle.sdk.util.memoryStorage.Action;

public class MemoryStorageExtend extends MemoryStorage {
  public MemoryStorageExtend(@NonNull Kuzzle kuzzle) {
    super(kuzzle);
  }

  public MemoryStorage send(final Action action, final KuzzleJSONObject query) {
    return super.send(action, query);
  }
}
