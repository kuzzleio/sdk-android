package io.kuzzle.test.testUtils;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;

/**
 * Created by scottinet on 19/02/16.
 */
public class KuzzleRoomExtend extends KuzzleRoom {

  public KuzzleRoomExtend(Collection kuzzleDataCollection) {
    super(kuzzleDataCollection);
  }

  public KuzzleRoomExtend(Collection kuzzleDataCollection, KuzzleRoomOptions options) {
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

  @Override
  public KuzzleRoom unsubscribe() {
    // do nothing
    return this;
  }

  public KuzzleRoom superUnsubscribe() {
    return super.unsubscribe();
  }

  public TimerTask  unsubscribeTask(final Timer timer, final String roomId, final JSONObject data) {
    return super.unsubscribeTask(timer, roomId, data);
  }

  public KuzzleRoom makeHeadersNull() {
    super.headers = null;
    return this;
  }

}
