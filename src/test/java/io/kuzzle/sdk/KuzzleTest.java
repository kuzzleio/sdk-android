package io.kuzzle.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.IEventListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.util.KuzzleQueryObject;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleTest {

  private Kuzzle kuzzle;
  private Socket  s;

  @Before
  public void setUp() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    s = mock(Socket.class);
    kuzzle.setSocket(s);
  }

  @Test
  public void testConnectEventConnect() throws Exception {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("foo", "bar");
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(result);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          assertEquals(object.getString("foo"), "bar");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    kuzzle.setSocket(s);
    kuzzle.connect(null);
    verify(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadUriConnection() throws Exception {
    kuzzle = new Kuzzle(null);
  }

  @Test
  public void testKuzzleConstructor() throws Exception {
    assertNotNull(kuzzle);
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    assertNotNull(kuzzle);
  }

  @Test(expected = KuzzleException.class)
  public void testIsValid() throws Exception {
    kuzzle.logout();
    kuzzle.isValid();
  }

  @Test
  public void testAddListener() throws Exception {
    assertEquals(kuzzle.getEventListeners(), new ArrayList<IEventListener>());
    IEventListener event = new IEventListener() {
      @Override
      public void trigger(String id, JSONObject result) {
      }
    };
    List<IEventListener> listenerList = new ArrayList<>();
    listenerList.add(event);
    kuzzle.addListener(EventType.SUBSCRIBED, event);
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.SUBSCRIBED);
  }

  @Test
  public void testDataCollectionFactory() throws Exception {
    assertEquals(kuzzle.dataCollectionFactory("test").fetchDocument("test", null).getCollection(), "test");
    assertEquals(kuzzle.dataCollectionFactory("test2").fetchDocument("test2", null).getCollection(), "test2");
  }

  @Test
  public void testLogout() throws Exception {
    assertNotNull(kuzzle.getSocket());
    kuzzle.logout();
    assertNull(kuzzle.getSocket());
  }

  @Test
  public void testNow() throws Exception {
    Kuzzle kuzzleSpy = spy(kuzzle);
    kuzzleSpy.now(null);
    verify(kuzzleSpy).now(any(ResponseListener.class));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testRemoveAllListeners() throws Exception {
    kuzzle.addListener(EventType.SUBSCRIBED, null);
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.SUBSCRIBED);
    kuzzle.removeAllListeners();
    kuzzle.getEventListeners().get(0);
  }

  @Test
  public void testRemoveAllListenersType() throws Exception {
    kuzzle.addListener(EventType.SUBSCRIBED, null);
    kuzzle.addListener(EventType.UNSUBSCRIBED, null);
    assertEquals(kuzzle.getEventListeners().size(), 2);
    kuzzle.removeAllListeners(EventType.SUBSCRIBED);
    assertEquals(kuzzle.getEventListeners().size(), 1);
  }

  @Test
  public void testRemoveListener() throws Exception {
    assertNotNull(kuzzle.getSocket());
    Kuzzle spy = spy(kuzzle);
    IEventListener event = new IEventListener() {
      @Override
      public void trigger(String id, JSONObject result) {
      }
    };
    String id = spy.addListener(EventType.UNSUBSCRIBED, event);
    String id2 = spy.addListener(EventType.SUBSCRIBED, event);
    assertEquals(spy.getEventListeners().get(0).getType(), EventType.UNSUBSCRIBED);
    assertEquals(spy.getEventListeners().get(1).getType(), EventType.SUBSCRIBED);
    spy.removeListener(EventType.SUBSCRIBED, id2);
    spy.removeListener(EventType.UNSUBSCRIBED, id);
    assertEquals(spy.getEventListeners().size(), 0);
  }

  @Test
  public void testReplayQueue() {

  }

  @Test
  public void testSetHeaders() throws Exception {
    JSONObject content = new JSONObject();
    content.put("foo", "bar");
    kuzzle.setHeaders(content);
    assertEquals(kuzzle.getHeaders().getString("foo"), "bar");
    content.put("foo", "baz");
    kuzzle.setHeaders(content, true);
    assertEquals(kuzzle.getHeaders().getString("foo"), "baz");
  }

  @Test
  public void testAddHeaders() throws Exception {
    JSONObject query = new JSONObject();
    JSONObject headers = new JSONObject();
    headers.put("testPurpose", "test");
    kuzzle.addHeaders(query, headers);
    assertEquals(query.get("testPurpose"), "test");
  }

  @Test
  public void testMetadataOptions() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    options.setMetadata(meta);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");

    kuzzle.query("collection", "controller", "action", jsonObj, options, null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

  @Test
  public void testMetadataInKuzzle() throws Exception {
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    KuzzleOptions options = new KuzzleOptions();
    options.setMetadata(meta);
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);
    kuzzle.query("collection", "controller", "action", jsonObj, options);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

  @Test
  public void testGetAllStatsSuccess() throws Exception {
    Kuzzle spy = spy(kuzzle);

    final JSONObject response = new JSONObject();
    JSONObject stats = new JSONObject();
    JSONObject frame = new JSONObject();

    stats.put("connections", new JSONObject());
    stats.put("ongoingRequests", new JSONObject());
    stats.put("completedRequests", new JSONObject());
    stats.put("failedRequests", new JSONObject());
    frame.put("2015-11-16T13:36:45.558Z", stats);
    response.put("statistics", frame);

    JSONArray array = new JSONArray();
    final JSONObject responseCallback = new JSONObject();
    array.put(stats);
    responseCallback.put("statistics", array);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
        return null;
      }
    }).when(spy).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    spy.getAllStatistics(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          JSONArray array = object.getJSONArray("statistics");
          for (int i = 0; i < array.length(); i++) {
            for (Iterator ite = array.getJSONObject(i).keys(); ite.hasNext(); ) {
              String key = (String) ite.next();
              assertEquals(array.getJSONObject(i).get(key), responseCallback.getJSONArray("statistics").getJSONObject(i).get(key));
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(spy, times(1)).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testGetAllStatsError() throws Exception {
    Kuzzle spy = spy(kuzzle);

    final JSONObject responseError = new JSONObject();
    responseError.put("error", "rorre");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ResponseListener) invocation.getArguments()[5]).onError(responseError);
        return null;
      }
    }).when(spy).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    spy.getAllStatistics(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
      }

      @Override
      public void onError(JSONObject error) {
        try {
          assertEquals(error.get("error"), "rorre");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    });
    verify(spy, times(1)).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testGetLastStatistic() throws Exception {
    Kuzzle spy = spy(kuzzle);
    final JSONObject response = new JSONObject();
    JSONObject stats = new JSONObject();
    JSONObject frame = new JSONObject();

    stats.put("connections", new JSONObject());
    stats.put("ongoingRequests", new JSONObject());
    stats.put("completedRequests", new JSONObject());
    stats.put("failedRequests", new JSONObject());
    frame.put("2015-11-16T13:36:45.558Z", stats);
    response.put("statistics", frame);

    final JSONObject responseCallback = new JSONObject();
    String firstKey = response.getJSONObject("statistics").keys().next();
    JSONObject oFrame = response.getJSONObject("statistics").getJSONObject(firstKey);
    frame.put("timestamp", firstKey);
    responseCallback.put("statistics", oFrame);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
        return null;
      }
    }).when(spy).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    spy.getStatistics(null, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(spy, times(1)).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testGetStatistic() throws Exception {
    Kuzzle spy = spy(kuzzle);
    final JSONObject response = new JSONObject();
    JSONObject stats = new JSONObject();
    JSONObject frame = new JSONObject();

    stats.put("connections", new JSONObject());
    stats.put("ongoingRequests", new JSONObject());
    stats.put("completedRequests", new JSONObject());
    stats.put("failedRequests", new JSONObject());
    frame.put("2015-11-16T13:36:45.558Z", stats);
    response.put("statistics", frame);

    final JSONObject responseCallback = new JSONObject();
    String firstKey = response.getJSONObject("statistics").keys().next();
    JSONObject oFrame = response.getJSONObject("statistics").getJSONObject(firstKey);
    frame.put("timestamp", firstKey);
    responseCallback.put("statistics", oFrame);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(response);
        ((ResponseListener) invocation.getArguments()[5]).onError(null);
        return null;
      }
    }).when(spy).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    spy.getStatistics("2015-11-15T13:36:45.558Z", new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(spy, times(1)).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testDequeue() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(10000);
    options.setAutoReplay(true);
    options.setReplayInterval(1);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        kuzzle.setAutoReconnect(false);
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzle.setAutoReconnect(true);
    kuzzle.connect(null);

    KuzzleQueryObject o = new KuzzleQueryObject();
    o.setTimestamp(new Date());
    o.setAction("test");

    JSONObject query = new JSONObject("{\"controller\":\"test3\",\"metadata\":{},\"requestId\":\"a476ae61-497e-4338-b4dd-751ac22c6b61\",\"action\":\"test3\",\"collection\":\"test3\"}");
    o.setQuery(query);
    kuzzle.getOfflineQueue().add(o);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
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
    kuzzle.connect(null);
    kuzzle.setAutoReplay(false);
    kuzzle.replayQueue();
    verify(s, atLeastOnce()).emit(eq("kuzzle"), eq(query));
  }

  @Test
  public void testQueueMaxSize() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1000);
    options.setQueueMaxSize(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        kuzzle.setAutoReconnect(false);
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzle.setAutoReconnect(true);
    kuzzle.connect(null);

    kuzzle.query("test", "test", "test", new JSONObject(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    kuzzle.query("test2", "test2", "test2", new JSONObject());
    kuzzle.query("test3", "test3", "test3", new JSONObject());
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    assertEquals(kuzzle.getOfflineQueue().peek().getQuery().getString("controller"), "test3");
    verify(s, never()).emit(any(String.class), any(Emitter.Listener.class));
  }

  @Test
  public void testEmitListener() throws Exception {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject result = new JSONObject();
        result.put("result", new JSONObject());
        ((Emitter.Listener) invocation.getArguments()[1]).call(result);
        result.put("error", new JSONObject());
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
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    JSONObject query = new JSONObject();
    query.put("requestId", "42");
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);
    kuzzle.connect(null);
    kuzzle.query("test", "test", "test", query, null, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq("42"), any(Emitter.Listener.class));
  }

  @Test
  public void testMaxTTLWithReconnectListener() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setReplayInterval(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);

    KuzzleQueryObject o = new KuzzleQueryObject();
    o.setTimestamp(new Date());
    o.setAction("test");
    JSONObject query = new JSONObject("{\"controller\":\"test3\",\"metadata\":{},\"requestId\":\"a476ae61-497e-4338-b4dd-751ac22c6b61\",\"action\":\"test3\",\"collection\":\"test3\"}");
    o.setQuery(query);

    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        kuzzle.setAutoReconnect(false);
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
    kuzzle.setAutoReconnect(true);
    kuzzle.connect(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    kuzzle.setOfflineQueue(o);

    IEventListener listener = mock(IEventListener.class);
    kuzzle.addListener(EventType.RECONNECTED, listener);
    kuzzle.connect(null);
    verify(listener, times(1)).trigger(any(String.class), any(JSONObject.class));
  }

  @Test
  public void testRenewSubscriptionsAfterReconnection() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);

    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);
    final Kuzzle kuzzleSpy = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(new JSONObject());
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject args = new JSONObject();
        args.put("roomId", "42");
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(args);
        return null;
      }
    }).when(kuzzleSpy).query(any(String.class), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));

    ResponseListener listener = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
    ResponseListener spyListener = spy(listener);
    kuzzleSpy.connect(spyListener);
    verify(spyListener, times(1)).onSuccess(any(JSONObject.class));
    KuzzleDataCollection collection = new KuzzleDataCollection(kuzzleSpy, "test");
    KuzzleDataCollection collection2 = new KuzzleDataCollection(kuzzleSpy, "test2");
    collection.subscribe();
    collection2.subscribe();
    reset(s);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        kuzzleSpy.setAutoReconnect(false);
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzleSpy.setAutoReconnect(true);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));
    kuzzleSpy.connect(null);
    verify(kuzzleSpy, times(4)).query(any(String.class), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    verify(s, times(2)).emit(eq("kuzzle"), any(JSONObject.class));
  }

  @Test
  public void testAutoReconnect() throws Exception {
    kuzzle.setAutoReconnect(true);
    final Kuzzle  kuzzleSpy = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        kuzzleSpy.setSocket(s);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzleSpy.connect(null);
    verify(kuzzleSpy, times(1)).logout();
  }

  @Test
  public void testConnectNotValid() throws Exception {
    Kuzzle kuzzleSpy = spy(kuzzle);
    when(kuzzleSpy.isValidSate()).thenReturn(false);
    ResponseListener listener = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
    ResponseListener spy = spy(listener);
    kuzzleSpy.connect(spy);
    verify(spy, times(1)).onError(any(JSONObject.class));
  }

  @Test
  public void testOnConnectError() throws Exception {
    Kuzzle kuzzleSpy = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        EngineIOException engineIOException = new EngineIOException("foo");
        engineIOException.code = "42";
        ((Emitter.Listener) invocation.getArguments()[1]).call(engineIOException);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
    ResponseListener listener = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          assertEquals(object.get("message"), "foo");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {

      }
    };
    ResponseListener listenerSpy = spy(listener);
    kuzzleSpy.connect(listenerSpy);
    verify(listenerSpy, times(1)).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testFlushQueue() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);

    kuzzle = new Kuzzle("http://localhost:7512", options);
    KuzzleQueryObject o = new KuzzleQueryObject();
    o.setTimestamp(new Date());
    o.setAction("test");
    o.setQuery(new JSONObject("{\"controller\":\"test\",\"metadata\":{},\"requestId\":\"a476ae61-497e-4338-b4dd-751ac22c6b61\",\"action\":\"test\",\"collection\":\"test\"}"));
    kuzzle.getOfflineQueue().add(o);
    o.setQuery(new JSONObject("{\"controller\":\"test2\",\"metadata\":{},\"requestId\":\"a476ae61-497e-4338-b4dd-751ac22c6b61\",\"action\":\"test2\",\"collection\":\"test2\"}"));
    kuzzle.getOfflineQueue().add(o);
    assertEquals(kuzzle.getOfflineQueue().size(), 2);
    kuzzle.flushQueue();
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
  }

  @Test
  public void testListCollections() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);

    kuzzle = new Kuzzle("http://localhost:7512", options);
    Kuzzle kuzzleSpy = spy(kuzzle);
    kuzzleSpy.listCollections();
    kuzzleSpy.listCollections(options);
    kuzzleSpy.listCollections(new ResponseListener() {

      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    kuzzleSpy.listCollections(options, new ResponseListener() {

      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(kuzzleSpy, times(4)).query(any(String.class), eq("read"), eq("listCollections"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
  }

  @Test
  public void testManualQueuing() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(10000);
    options.setReplayInterval(1);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(false);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        kuzzle.setAutoReconnect(false);
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzle.connect(null);
    kuzzle.startQueuing();
    JSONObject query = new JSONObject();
    query.put("requestId", "42");
    kuzzle.query("test", "test", "test", query, null, null);
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    kuzzle.flushQueue();
    kuzzle.stopQueing();
    kuzzle.query("test", "test", "test", query, null, null);
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
  }

  @Test
  public void testQueuable() throws Exception {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        kuzzle.setAutoReconnect(false);
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    KuzzleOptions options = new KuzzleOptions();
    options.setAutoQueue(true);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.connect(null);
    kuzzle.listCollections(options);
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    kuzzle.flushQueue();
    options.setQueuable(false);
    kuzzle.listCollections(options);
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
  }

  @Test
  public void testDeleteSubscription() throws KuzzleException, IOException, JSONException {
    kuzzle.setHeaders(new JSONObject());
    Kuzzle kuzzleSpy = spy(kuzzle);
    KuzzleDataCollection collection = mock(KuzzleDataCollection.class);
    when(collection.getKuzzle()).thenReturn(kuzzleSpy);
    when(collection.getHeaders()).thenReturn(new JSONObject());
    KuzzleRoom room = new KuzzleRoom(collection);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("roomId", "42");
        //Call callback with response
        ((ResponseListener) invocation.getArguments()[5]).onSuccess(result);
        return null;
      }
    }).when(kuzzleSpy).query(any(String.class), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    room.renew(new JSONObject(), null);
    assertEquals(kuzzleSpy.getSubscriptions().size(), 1);
    room.unsubscribe();
    assertEquals(kuzzleSpy.getSubscriptions().size(), 0);
  }

}
