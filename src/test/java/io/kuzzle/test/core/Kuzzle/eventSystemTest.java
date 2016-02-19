package io.kuzzle.test.core.Kuzzle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.IKuzzleEventListener;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.util.EventList;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.kuzzle.test.testUtils.QueryArgsHelper;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class eventSystemTest {
  private KuzzleExtend kuzzle;
  private Socket s;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    s = mock(Socket.class);
    kuzzle = new KuzzleExtend("http://localhost:7512", options, null);
    kuzzle.setSocket(s);

    listener = new KuzzleResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
  }

  @Test
  public void testAddListener() {

    assertEquals(kuzzle.getEventListeners(KuzzleEvent.connected), null);
    kuzzle.addListener(KuzzleEvent.connected, mock(IKuzzleEventListener.class));
    assertThat(kuzzle.getEventListeners(KuzzleEvent.connected), instanceOf(EventList.class));
  }

  @Test
  public void testEmitListener() throws JSONException, URISyntaxException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject result = new JSONObject();
        result.put("result", new JSONObject());
        ((Emitter.Listener) invocation.getArguments()[1]).call(result);
        result.put("error", new JSONObject().put("message", new String()));
        ((Emitter.Listener) invocation.getArguments()[1]).call(result);
        return s;
      }
    }).when(s).once(eq("42"), any(Emitter.Listener.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));

    JSONObject query = new JSONObject();
    query.put("requestId", "42");
    kuzzle.connect();
    kuzzle.query(QueryArgsHelper.makeQueryArgs("test", "test"), query, null, mock(OnQueryDoneListener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq("42"), any(Emitter.Listener.class));
  }


  @Test(expected = NullPointerException.class)
  public void testRemoveAllListeners() {
    String id = kuzzle.addListener(KuzzleEvent.connected, null);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.connected).get(id).getType(), KuzzleEvent.connected);
    kuzzle.removeAllListeners();
    kuzzle.getEventListeners(KuzzleEvent.connected).get(id).getType();
  }

  @Test
  public void testRemoveAllListenersType() {
    kuzzle.addListener(KuzzleEvent.connected, null);
    kuzzle.addListener(KuzzleEvent.disconnected, null);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.connected).size(), 1);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.disconnected).size(), 1);
    kuzzle.removeAllListeners(KuzzleEvent.connected);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.connected).size(), 0);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.disconnected).size(), 1);
  }

  @Test
  public void testRemoveListener() {
    String id = kuzzle.addListener(KuzzleEvent.disconnected, mock(IKuzzleEventListener.class));
    String id2 = kuzzle.addListener(KuzzleEvent.connected, mock(IKuzzleEventListener.class));
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.disconnected).get(id).getType(), KuzzleEvent.disconnected);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.connected).get(id2).getType(), KuzzleEvent.connected);
    kuzzle.removeListener(KuzzleEvent.connected, id2);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.connected).size(), 0);
    assertEquals(kuzzle.getEventListeners(KuzzleEvent.disconnected).size(), 1);
  }
}
