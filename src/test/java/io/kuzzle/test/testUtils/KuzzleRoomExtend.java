package io.kuzzle.test.testUtils;

import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;

/**
 * Created by scottinet on 19/02/16.
 */
public class KuzzleRoomExtend extends KuzzleRoom {
  public KuzzleRoomExtend(KuzzleDataCollection kuzzleDataCollection) {
    super(kuzzleDataCollection);
  }

  public KuzzleRoomExtend(KuzzleDataCollection kuzzleDataCollection, KuzzleRoomOptions options) {
    super(kuzzleDataCollection, options);
  }

  public void callAfterRenew(Object args) {
    super.callAfterRenew(args);
  }

  public void setListener(final KuzzleResponseListener listener) {
    this.listener = listener;
  }

  public void setRoomId(final String id) {
    this.roomId = id;
  }

  public void setSubscribing(final boolean isSubscribing) {
    super.subscribing = isSubscribing;
  }

  public void dequeue() {
    super.dequeue();
  }
}
