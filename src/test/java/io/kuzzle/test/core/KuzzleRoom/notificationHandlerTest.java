package io.kuzzle.test.core.KuzzleRoom;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.listeners.IKuzzleEventListener;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.test.testUtils.KuzzleRoomExtend;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class notificationHandlerTest {
  private KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
  private JSONObject mockNotif = new JSONObject();
  private JSONObject  mockResponse = new JSONObject();
  private Kuzzle k;
  private KuzzleRoomExtend room;

  @Before
  public void setUp() throws JSONException {
    mockNotif.put("type", "type")
      .put("index", "index")
      .put("status", 200)
      .put("collection", "collection")
      .put("controller", "controller")
      .put("action", "action")
      .put("state", "ALL")
      .put("scope", "ALL")
      .put("metadata", new JSONObject())
      .put("result", new JSONObject())
      .put("requestId", "42");
    mockResponse.put("result", new JSONObject().put("channel", "channel").put("roomId", "42"));
    k = mock(Kuzzle.class);
    when(k.getHeaders()).thenReturn(new JSONObject());
    room = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCallAfterRenewWithNoResponse() {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    // Should throw an exception
    renew.callAfterRenew(null);
  }

  @Test
  public void testCallAfterRenewWithError() throws JSONException {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    JSONObject errorResponse = new JSONObject();
    errorResponse.put("error", "error");
    KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
    renew.setListener(listener);
    renew.callAfterRenew(errorResponse);
    verify(listener, atLeastOnce()).onError(any(JSONObject.class));
  }

  @Test
  public void testCallAfterRenew() throws JSONException {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    renew.setListener(listener);
    JSONObject mockResponse = new JSONObject().put("result", new JSONObject());
    mockResponse.put("requestId", "42");
    renew.callAfterRenew(mockNotif);
  }

  @Test
  public void testCallAfterRenewWithSubscribeToSelf() throws JSONException {
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setSubscribeToSelf(true);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"), options);
    renew.setListener(listener);
    Map<String, Date> m = new HashMap();
    m.put("42", new Date());
    renew.callAfterRenew(mockNotif);
    verify(listener, atLeastOnce()).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testJwtTokenExpiredNotification() throws JSONException, URISyntaxException {
    k = new Kuzzle("http://localhost:7512");
    IKuzzleEventListener listener = spy(new IKuzzleEventListener() {
      @Override
      public void trigger(Object... args) {

      }
    });
    k.addListener(KuzzleEvent.jwtTokenExpired, listener);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    renew.setListener(mock(KuzzleResponseListener.class));
    JSONObject mockResponse = new JSONObject().put("result", new JSONObject());
    mockResponse.put("requestId", "42");
    mockNotif.put("action", "jwtTokenExpired");
    renew.callAfterRenew(mockNotif);
    verify(listener, times(1)).trigger();
  }
}
