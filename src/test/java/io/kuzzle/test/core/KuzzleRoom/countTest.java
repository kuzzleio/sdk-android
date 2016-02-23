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
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.kuzzle.test.testUtils.KuzzleRoomExtend;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class countTest {
  private KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
  private KuzzleResponseListener  spyListener = spy(new KuzzleResponseListener() {
    @Override
    public void onSuccess(Object response) {

    }

    @Override
    public void onError(JSONObject error) {

    }
  });
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
  public void testCountIllegalArgument() {
    room.count(null);
  }

  @Test
  public void testCountWhileSubscribing() throws JSONException, URISyntaxException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    KuzzleExtend extended = new KuzzleExtend("http://localhost:7512", opts, null);
    extended.setSocket(mock(Socket.class));
    extended.setState(KuzzleStates.CONNECTED);
    extended = spy(extended);
    room = new KuzzleRoomExtend(new KuzzleDataCollection(extended, "index", "test"));
    room.setRoomId("foobar");
    room.setSubscribing(true);
    room.count(spyListener);
    room = spy(room);
    room.setSubscribing(false);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("result", new JSONObject()
            .put("channel", "channel")
            .put("roomId", "42")
            .put("count", 42));
        //Call callback with response
        if (invocation.getArguments()[3] != null) {
          ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(result);
          ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject());
        }
        return null;
      }
    }).when(extended).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    room.renew(listener);
    verify(room).dequeue();
  }

  @Test(expected = RuntimeException.class)
  public void testCountQueryException() throws JSONException {
    doThrow(JSONException.class).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(OnQueryDoneListener.class));
    room.count(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testCountException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[4]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(Integer.class));
    room.count(listener);
  }

  @Test
  public void testCount() throws JSONException {
    JSONObject o = mock(JSONObject.class);
    when(o.put(any(String.class), any(Object.class))).thenReturn(new JSONObject());

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[2]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        ((OnQueryDoneListener)invocation.getArguments()[2]).onError(new JSONObject());
        return null;
      }
    }).when(k).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(OnQueryDoneListener.class));
    room.setRoomId("foobar");
    room.count(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(k, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "count");
  }

}
