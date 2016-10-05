package io.kuzzle.test.core.KuzzleRoom;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;
import java.util.Date;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.IKuzzleEventListener;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.kuzzle.test.testUtils.KuzzleRoomExtend;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
    room = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test", "index"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCallAfterRenewWithNoResponse() {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test", "index"));
    // Should throw an exception
    renew.callAfterRenew(null);
  }

  @Test
  public void testCallAfterRenewWithError() throws JSONException {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test", "index"));
    JSONObject errorResponse = new JSONObject();
    errorResponse.put("error", "error");
    KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
    renew.setListener(listener);
    renew.callAfterRenew(errorResponse);
    verify(listener, atLeastOnce()).onError(any(JSONObject.class));
  }

  @Test
  public void testCallAfterRenew() throws JSONException {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test", "index"));
    renew.setListener(listener);
    JSONObject mockResponse = new JSONObject().put("result", new JSONObject());
    mockResponse.put("requestId", "42");
    renew.callAfterRenew(mockNotif);
  }

  @Test
  public void testCallAfterRenewWithSubscribeToSelf() throws JSONException, URISyntaxException {
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setSubscribeToSelf(true);
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    Socket s = mock(Socket.class);
    extended.setSocket(s);
    extended.setState(KuzzleStates.CONNECTED);
    extended = spy(extended);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(extended, "test", "index"), options);
    renew.setListener(listener);
    extended.getRequestHistory().put("42", new Date());
    renew.callAfterRenew(mockNotif);
    verify(listener, atLeastOnce()).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testRenewOn() throws JSONException, URISyntaxException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    Socket s = mock(Socket.class);
    extended.setSocket(s);
    extended.setState(KuzzleStates.CONNECTED);
    extended = spy(extended);
    room = new KuzzleRoomExtend(new KuzzleDataCollection(extended, "collection", "index"));
    room.setRoomId("foobar");
    room = spy(room);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("result", new JSONObject().put("channel", "channel").put("roomId", "42"));
        //Call callback with response
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(result);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject());
        return null;
      }
    }).when(extended).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(mockNotif);
        verify(room).callAfterRenew(any(Object.class));

        return null;
      }
    }).when(s).on(any(String.class), any(Emitter.Listener.class));
    room.renew(listener);
  }

  @Test
  public void testJwtTokenExpiredNotification() throws JSONException, URISyntaxException {
    k = new Kuzzle("localhost");
    IKuzzleEventListener listener = spy(new IKuzzleEventListener() {
      @Override
      public void trigger(Object... args) {

      }
    });
    k.addListener(KuzzleEvent.jwtTokenExpired, listener);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test", "index"));
    renew.setListener(mock(KuzzleResponseListener.class));
    JSONObject mockResponse = new JSONObject().put("result", new JSONObject());
    mockResponse.put("requestId", "42");
    mockNotif.put("action", "jwtTokenExpired");
    renew.callAfterRenew(mockNotif);
    verify(listener, times(1)).trigger();
  }

  @Test(expected = RuntimeException.class)
  public void testCallAfterRenewException() throws URISyntaxException, JSONException {
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("localhost", opts, null);
    Socket s = mock(Socket.class);
    extended.setSocket(s);
    extended.setState(KuzzleStates.CONNECTED);
    extended = spy(extended);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(extended, "test", "index"), options);
    mockNotif = spy(mockNotif);
    doThrow(JSONException.class).when(mockNotif).isNull(any(String.class));
    renew.setListener(listener);
    extended.getRequestHistory().put("42", new Date());
    mockNotif.put("error", mock(JSONObject.class));
    renew.callAfterRenew(mockNotif);
    // should trigger listener.onError
    verify(listener, atLeastOnce()).onError(any(JSONObject.class));
    // Now simulate exception on the onError
    doThrow(JSONException.class).when(listener).onError(any(JSONObject.class));
    mockNotif.remove("error");
    renew.callAfterRenew(mockNotif);
  }
}
