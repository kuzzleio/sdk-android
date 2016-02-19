package io.kuzzle.test.core.KuzzleRoom;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.kuzzle.test.testUtils.KuzzleRoomExtend;
import io.kuzzle.test.testUtils.QueryArgsHelper;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class renewTest {
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

  @Test
  public void testRenew() throws JSONException, URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    Socket s = mock(Socket.class);
    KuzzleExtend kuzzle = new KuzzleExtend("http://localhost:7512", options, null);
    kuzzle.setState(KuzzleStates.CONNECTED);

    final Kuzzle kuzzleSpy = spy(kuzzle);
    KuzzleRoom testRoom = new KuzzleRoom(new KuzzleDataCollection(kuzzleSpy, "index", "collection"));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(mockNotif);
        return null;
      }
    }).when(s).on(any(String.class), any(Emitter.Listener.class));
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
    }).when(kuzzleSpy).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    testRoom.renew(new JSONObject(), listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzleSpy, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "on");
  }

  @Test(expected = RuntimeException.class)
  public void testRenewException() throws JSONException {
    doThrow(JSONException.class).when(listener).onError(any(JSONObject.class));
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    renew.callAfterRenew(new JSONObject().put("error", mock(JSONObject.class)));
  }

  @Test
  public void testRenewWithError() throws URISyntaxException, JSONException {
    // stub to call the callback from KuzzleRoom.renew method
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("error", "foo");
        //Call callback with error response
        ((KuzzleResponseListener) invocation.getArguments()[4]).onError(response);
        return null;
      }
    }).when(k).query(eq(QueryArgsHelper.makeQueryArgs("subscribe", "on")), any(JSONObject.class), any(OnQueryDoneListener.class));
    room.renew(mock(JSONObject.class), mock(KuzzleResponseListener.class));
  }
}
