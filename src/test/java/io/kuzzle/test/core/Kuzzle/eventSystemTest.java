package io.kuzzle.test.core.Kuzzle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Event;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.EventListener;
import io.kuzzle.sdk.listeners.ResponseListener;
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
  private ResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    Options options = new Options();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    s = mock(Socket.class);
    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setSocket(s);

    listener = new ResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }
    };
  }

  @Test
  public void testAddListener() {
    assertEquals(kuzzle.getEventListeners(Event.connected), null);
    kuzzle.addListener(Event.connected, mock(EventListener.class));
    assertThat(kuzzle.getEventListeners(Event.connected), instanceOf(EventList.class));
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
    kuzzle.addListener(Event.connected, mock(EventListener.class));
    kuzzle.addListener(Event.connected, mock(EventListener.class));
    kuzzle.addListener(Event.disconnected, mock(EventListener.class));
    kuzzle.addListener(Event.disconnected, mock(EventListener.class));
    assertEquals(kuzzle.getEventListeners(Event.connected).size(), 2);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).size(), 2);
    kuzzle.removeAllListeners();
    assertEquals(kuzzle.getEventListeners(Event.connected).size(), 0);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).size(), 0);
  }

  @Test
  public void testRemoveAllListenersType() {
    kuzzle.addListener(Event.connected, mock(EventListener.class));
    kuzzle.addListener(Event.connected, mock(EventListener.class));
    kuzzle.addListener(Event.disconnected, mock(EventListener.class));
    kuzzle.addListener(Event.disconnected, mock(EventListener.class));
    assertEquals(kuzzle.getEventListeners(Event.connected).size(), 2);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).size(), 2);
    kuzzle.removeAllListeners(Event.connected);
    assertEquals(kuzzle.getEventListeners(Event.connected).size(), 0);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).size(), 2);
  }

  @Test
  public void testRemoveListener() {
    EventListener listener1 = mock(EventListener.class);
    EventListener listener2 = mock(EventListener.class);
    EventListener listener3 = mock(EventListener.class);
    EventListener listener4 = mock(EventListener.class);
    kuzzle.addListener(Event.disconnected, listener1);
    kuzzle.addListener(Event.connected, listener2);
    kuzzle.addListener(Event.disconnected, listener3);
    kuzzle.addListener(Event.connected, listener4);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).get(listener1).getType(), Event.disconnected);
    assertEquals(kuzzle.getEventListeners(Event.connected).get(listener2).getType(), Event.connected);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).get(listener3).getType(), Event.disconnected);
    assertEquals(kuzzle.getEventListeners(Event.connected).get(listener4).getType(), Event.connected);
    kuzzle.removeListener(Event.connected, listener2);
    assertEquals(kuzzle.getEventListeners(Event.connected).size(), 1);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).size(), 2);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).get(listener1).getType(), Event.disconnected);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).get(listener2), null);
    assertEquals(kuzzle.getEventListeners(Event.disconnected).get(listener3).getType(), Event.disconnected);
    assertEquals(kuzzle.getEventListeners(Event.connected).get(listener4).getType(), Event.connected);
  }
}
