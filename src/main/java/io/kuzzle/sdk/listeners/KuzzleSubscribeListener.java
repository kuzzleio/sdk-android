package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.kuzzle.sdk.core.KuzzleRoom;

public class KuzzleSubscribeListener<T> {
  private JSONObject  error;
  private KuzzleRoom  room;
  private List<KuzzleResponseListener<KuzzleRoom>>  cbs = new ArrayList<>();

  public void onDone(KuzzleResponseListener<KuzzleRoom> cb) {
    if (this.error != null) {
      cb.onError(this.error);
    } else if (this.room != null) {
      cb.onSuccess(this.room);
    } else {
      this.cbs.add(cb);
    }
  }

  public KuzzleSubscribeListener<T> done(JSONObject error, KuzzleRoom room) {
    this.error = error;
    this.room = room;

    for (KuzzleResponseListener<KuzzleRoom> cb : cbs) {
      if (this.error != null) {
        cb.onError(this.error);
      } else if (this.room != null) {
        cb.onSuccess(this.room);
      }
    }
    return this;
  }

  public JSONObject getError() {
    return error;
  }

  public KuzzleRoom getRoom() {
    return room;
  }
}
