package io.kuzzle.test.core.KuzzleRoom;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class unsubscribeTest {
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
  public void testUnsubscribe() throws JSONException, URISyntaxException {
    KuzzleOptions opts = new KuzzleOptions();
    opts.setConnect(Mode.MANUAL);
    Socket s = mock(Socket.class);

    KuzzleExtend kuzzle = new KuzzleExtend("http://localhost:7512", opts, null);
    kuzzle.setState(KuzzleStates.CONNECTED);
    kuzzle.setSocket(s);

    kuzzle = spy(kuzzle);
    room = new KuzzleRoomExtend(new KuzzleDataCollection(kuzzle, "index", "test"));
    room.setRoomId("42");
    room.unsubscribe();
    assertEquals(room.getRoomId(), null);
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "off");
  }
}
