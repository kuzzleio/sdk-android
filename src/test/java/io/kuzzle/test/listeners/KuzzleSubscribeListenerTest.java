package io.kuzzle.test.listeners;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.KuzzleSubscribeListener;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class KuzzleSubscribeListenerTest {

  private KuzzleSubscribeListener subListener;
  private KuzzleResponseListener<KuzzleRoom> callback;
  private JSONObject  json = new JSONObject();

  @Before
  public void setUp() {
    subListener = new KuzzleSubscribeListener();
    callback = spy(new KuzzleResponseListener<KuzzleRoom>() {
      @Override
      public void onSuccess(KuzzleRoom response) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
  }

  @Test
  public void testOnDoneError() {
    subListener.onDone(callback);
    subListener.done(json, null);
    verify(callback).onError(any(JSONObject.class));
  }

  @Test
  public void testOnDoneSuccess() {
    subListener.onDone(callback);
    subListener.done(null, mock(KuzzleRoom.class));
    verify(callback).onSuccess(any(KuzzleRoom.class));
  }

  @Test
  public void testPostOnDoneError() {
    subListener.done(json, null);
    subListener.onDone(callback);
    verify(callback).onError(any(JSONObject.class));
  }

  @Test
  public void testPostOnDoneSuccess() {
    subListener.done(null, mock(KuzzleRoom.class));
    subListener.onDone(callback);
    verify(callback).onSuccess(any(KuzzleRoom.class));
  }

}
