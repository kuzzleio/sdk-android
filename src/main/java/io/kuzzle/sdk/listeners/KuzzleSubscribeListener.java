package io.kuzzle.sdk.listeners;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.kuzzle.sdk.core.Room;

public class KuzzleSubscribeListener {
  private JSONObject  error;
  private Room room;
  private List<KuzzleResponseListener<Room>>  cbs = new ArrayList<>();

  public void onDone(KuzzleResponseListener<Room> cb) {
    if (this.error != null) {
      cb.onError(this.error);
    } else if (this.room != null) {
      cb.onSuccess(this.room);
    } else {
      this.cbs.add(cb);
    }
  }

  public KuzzleSubscribeListener done(JSONObject error, Room room) {
    this.error = error;
    this.room = room;

    for (KuzzleResponseListener<Room> cb : cbs) {
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

  public Room getRoom() {
    return room;
  }
}
