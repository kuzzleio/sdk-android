package io.kuzzle.test.core.Kuzzle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.kuzzle.sdk.core.Collection;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.core.Room;
import io.kuzzle.sdk.enums.Event;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.EventListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.responses.TokenValidity;
import io.kuzzle.sdk.state.States;
import io.kuzzle.sdk.util.QueryObject;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.EngineIOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class connectionManagementTest {
  private KuzzleExtend kuzzle;
  private Socket s;
  private ResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    Options options = new Options();
    options.setConnect(Mode.MANUAL);
    options.setDefaultIndex("testIndex");

    s = mock(Socket.class);
    listener = new ResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
    kuzzle = new KuzzleExtend("localhost", options, listener);
    kuzzle.setSocket(s);
  }

  @Test
  public void testMaxTTLWithReconnectListener() throws JSONException, URISyntaxException {
    Options options = new Options();
    options.setQueueTTL(1);
    options.setReplayInterval(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setOfflineMode(Mode.AUTO);
    options.setAutoReconnect(true);

    QueryObject o = new QueryObject();
    o.setTimestamp(new Date());
    o.setAction("test");
    JSONObject query = new JSONObject("{\"controller\":\"test3\",\"volatile\":{},\"requestId\":\"a476ae61-497e-4338-b4dd-751ac22c6b61\",\"action\":\"test3\",\"collection\":\"test3\"}");
    o.setQuery(query);

    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setSocket(s);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));

    kuzzle.connect();
    kuzzle.setOfflineQueue(o);

    EventListener listener = mock(EventListener.class);
    kuzzle.addListener(Event.reconnected, listener);
    kuzzle.connect();
    verify(listener, times(1)).trigger();
  }

  @Test
  public void testRenewSubscriptionsAfterReconnection() throws URISyntaxException, JSONException, InterruptedException {
    Options options = new Options();
    options.setAutoReconnect(true);
    options.setQueueTTL(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);
    KuzzleExtend extended = new KuzzleExtend("localhost", options, null);
    extended.setSocket(s);
    extended.setState(States.INITIALIZING);
    final Kuzzle kuzzleSpy = spy(extended);

    Room
      room1 = new Room(new Collection(kuzzleSpy, "test", "index")),
      room2 = new Room(new Collection(kuzzleSpy, "test2", "index"));

    room1.renew(listener);
    room2.renew(listener);

    Map<String, ConcurrentHashMap<String, Room>> subscriptions = kuzzle.getSubscriptions();
    subscriptions.put("room", new ConcurrentHashMap<String, Room>());
    subscriptions.get("room").put("42", room1);
    subscriptions.get("room").put("43", room2);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, listener);

        return s;
      }
    }).when(s).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));
    kuzzleSpy.connect();

    Thread.sleep(2);
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzleSpy, atLeastOnce()).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "realtime");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "subscribe");
  }

  @Test
  public void testAutoReconnect() throws URISyntaxException {
    Options options = new Options();
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(false);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));

    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setSocket(s);
    kuzzle = spy(kuzzle);

    kuzzle.connect();

    // since "connect" is called 2 times with a socket set, disconnect
    // is called each time
    verify(kuzzle, times(2)).disconnect();
  }

  @Test
  public void testConnectNotValid() throws URISyntaxException {
    listener = spy(listener);
    kuzzle.setState(States.LOGGED_OUT);
    kuzzle.setListener(listener);
    kuzzle = spy(kuzzle);
    kuzzle.connect();
    verify(listener, atLeastOnce()).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testOnConnectError() throws URISyntaxException {
    listener = spy(listener);
    kuzzle.setListener(listener);
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        EngineIOException engineIOException = new EngineIOException("foo");
        engineIOException.code = "42";
        ((Emitter.Listener) invocation.getArguments()[1]).call(engineIOException);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
    kuzzle.connect();
    verify(listener, times(1)).onError(any(JSONObject.class));
  }

  @Test
  public void testConnectEventConnect() throws URISyntaxException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    kuzzle = spy(kuzzle);
    ResponseListener<Void> fake = kuzzle.spyAndGetConnectionCallback();
    doReturn(true).when(kuzzle).isValidState();
    kuzzle.connect();
    verify(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    verify(fake).onSuccess(any(Void.class));
  }

  @Test
  public void testDisconnect() {
    kuzzle.setState(States.CONNECTED);
    assertNotNull(kuzzle.getSocket());
    kuzzle.disconnect();
    assertNull(kuzzle.getSocket());
  }

  @Test(expected = RuntimeException.class)
  public void testIsValid() throws Exception {
    kuzzle.disconnect();
    kuzzle.isValid();
  }

  @Test
  public void testJWTTokenAfterReconnect() throws URISyntaxException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));
    kuzzle = spy(kuzzle);
    kuzzle.setJwtToken("foo");
    TokenValidity tokenValidity = spy(new TokenValidity());
    doReturn(false).when(tokenValidity).isValid();
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ResponseListener) invocation.getArguments()[1]).onSuccess(mock(TokenValidity.class));
        return null;
      }
    }).when(kuzzle).checkToken(eq("foo"), any(ResponseListener.class));
    kuzzle.connect();
    assertNull(kuzzle.getJwtToken());
    doReturn(true).when(tokenValidity).isValid();
    kuzzle.setJwtToken("foo");
    kuzzle.connect();
    assertEquals("foo", kuzzle.getJwtToken());
  }

  @Test
  public void testJWTTokenErrorAfterReconnect() throws URISyntaxException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));
    kuzzle = spy(kuzzle);
    kuzzle.setJwtToken("foo");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ResponseListener) invocation.getArguments()[1]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).checkToken(eq("foo"), any(ResponseListener.class));
    kuzzle.connect();
    assertNull(kuzzle.getJwtToken());
  }
}
