package io.kuzzle.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.IEventListener;
import io.kuzzle.sdk.listeners.KuzzResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleTest {

  private Kuzzle kuzzle;
  private Socket s;
  private KuzzResponseListener  listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    s = mock(Socket.class);
    kuzzle.setSocket(s);
    listener = new KuzzResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
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
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options, mock(KuzzResponseListener.class));
    kuzzle.setSocket(s);
    kuzzle.connect();
    verify(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadUriConnection() throws URISyntaxException {
    kuzzle = new Kuzzle(null, "testIndex");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadIndex() throws URISyntaxException {
    kuzzle = new Kuzzle("http://localhost:7512", null);
  }

  @Test
  public void testKuzzleConstructor() throws URISyntaxException, JSONException {
    kuzzle = spy(kuzzle);
    assertEquals(kuzzle.getIndex(), "testIndex");
    assertNotNull(kuzzle);
    verify(kuzzle, never()).query(any(String.class), eq("auth"), eq("login"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testIsValid() throws Exception {
    kuzzle.disconnect();
    kuzzle.isValid();
  }

  @Test
  public void testAddListener() {
    assertEquals(kuzzle.getEventListeners(), new ArrayList<IEventListener>());
    kuzzle.addListener(EventType.CONNECTED, mock(IEventListener.class));
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.CONNECTED);
  }

  @Test
  public void testDataCollectionFactory() {
    assertEquals(kuzzle.dataCollectionFactory("test").fetchDocument("test", mock(KuzzResponseListener.class)).getCollection(), "test");
    assertEquals(kuzzle.dataCollectionFactory("test2").fetchDocument("test2", mock(KuzzResponseListener.class)).getCollection(), "test2");
  }

  @Test
  public void testDisconnect() {
    assertNotNull(kuzzle.getSocket());
    kuzzle.disconnect();
    assertNull(kuzzle.getSocket());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNowIllegalListener() {
    kuzzle = spy(kuzzle);
    kuzzle.now(null);
  }

  @Test(expected = RuntimeException.class)
  public void testNowException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", new JSONObject().put("now", mock(JSONObject.class))));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("read"), eq("now"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.now(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testNowQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(String.class), eq("read"), eq("now"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.now(listener);
  }

  @Test
  public void testNow() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", new JSONObject().put("now", 42424242)));
        ((OnQueryDoneListener)invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("read"), eq("now"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.now(listener);
    verify(kuzzle).query(any(String.class), eq("read"), eq("now"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testRemoveAllListeners() {
    kuzzle.addListener(EventType.CONNECTED, null);
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.CONNECTED);
    kuzzle.removeAllListeners();
    kuzzle.getEventListeners().get(0);
  }

  @Test
  public void testRemoveAllListenersType() {
    kuzzle.addListener(EventType.CONNECTED, null);
    kuzzle.addListener(EventType.DISCONNECTED, null);
    assertEquals(kuzzle.getEventListeners().size(), 2);
    kuzzle.removeAllListeners(EventType.CONNECTED);
    assertEquals(kuzzle.getEventListeners().size(), 1);
  }

  @Test
  public void testRemoveListener() {
    assertNotNull(kuzzle.getSocket());
    kuzzle = spy(kuzzle);
    String id = kuzzle.addListener(EventType.DISCONNECTED, mock(IEventListener.class));
    String id2 = kuzzle.addListener(EventType.CONNECTED, mock(IEventListener.class));
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.DISCONNECTED);
    assertEquals(kuzzle.getEventListeners().get(1).getType(), EventType.CONNECTED);
    kuzzle.removeListener(id2);
    kuzzle.removeListener(id);
    assertEquals(kuzzle.getEventListeners().size(), 0);
  }

  @Test
  public void testSetHeaders() throws JSONException {
    JSONObject content = new JSONObject();
    content.put("foo", "bar");
    kuzzle.setHeaders(content);
    assertEquals(kuzzle.getHeaders().getString("foo"), "bar");
    content.put("foo", "baz");
    kuzzle.setHeaders(content, true);
    assertEquals(kuzzle.getHeaders().getString("foo"), "baz");
  }

  @Test
  public void testAddHeaders() throws JSONException {
    JSONObject query = new JSONObject();
    JSONObject headers = new JSONObject();
    headers.put("testPurpose", "test");
    kuzzle.addHeaders(query, headers);
    assertEquals(query.get("testPurpose"), "test");
  }

  @Test
  public void testMetadataOptions() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
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
  public void testMetadataInKuzzle() throws JSONException, URISyntaxException {
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    KuzzleOptions options = new KuzzleOptions();
    options.setMetadata(meta);
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.setSocket(s);
    kuzzle.query("collection", "controller", "action", jsonObj, options);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetAllStatisticsNoListener() {
    kuzzle.getAllStatistics(null);
  }

  @Test(expected = RuntimeException.class)
  public void testGetAllStatisticsException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", mock(JSONObject.class))));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetAllStatisticsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(listener);
  }

  @Test
  public void testGetAllStatsSuccess() throws JSONException {
    kuzzle = spy(kuzzle);

    final JSONObject response = new JSONObject("{\n" +
        "    \"total\": 25,\n" +
        "    \"hits\": [\n" +
        "      {\n" +
        "        \"completedRequests\": {\n" +
        "          \"websocket\": 148,\n" +
        "          \"rest\": 24,\n" +
        "          \"mq\": 78\n" +
        "        },\n" +
        "        \"failedRequests\": {\n" +
        "          \"websocket\": 3\n" +
        "        },\n" +
        "        \"ongoingRequests\": {\n" +
        "          \"mq\": 8,\n" +
        "          \"rest\": 2\n" +
        "        },\n" +
        "        \"connections\": {\n" +
        "          \"websocket\": 13\n" +
        "        },\n" +
        "        \"timestamp\": \"2016-01-13T13:46:19.917Z\"\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n");

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", response));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(new KuzzResponseListener<JSONArray>() {
      @Override
      public void onSuccess(JSONArray result) {
        try {
          for (int i = 0; i < result.length(); i++) {
            for (Iterator ite = result.getJSONObject(i).keys(); ite.hasNext(); ) {
              String key = (String) ite.next();
              assertEquals(result.getJSONObject(i).get(key), response.getJSONArray("hits").getJSONObject(i).get(key));
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
    verify(kuzzle, times(1)).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testGetAllStatsError() throws JSONException {
    kuzzle = spy(kuzzle);

    final JSONObject responseError = new JSONObject();
    responseError.put("error", "rorre");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(responseError);
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(new KuzzResponseListener<JSONArray>() {
      @Override
      public void onSuccess(JSONArray object) {
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
    verify(kuzzle, times(1)).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testGetLastStatistic() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject("{ result: {\n" +
            "    completedRequests: {\n" +
            "      websocket: 148,\n" +
            "      rest: 24,\n" +
            "      mq: 78\n" +
            "    },\n" +
            "    failedRequests: {\n" +
            "      websocket: 3\n" +
            "    },\n" +
            "    ongoingRequests: {\n" +
            "      mq: 8,\n" +
            "      rest: 2\n" +
            "    },\n" +
            "    connections: {\n" +
            "      websocket: 13\n" +
            "    },\n" +
            "    \"timestamp\": \"2016-01-13T13:46:19.917Z\"\n" +
            "  }" +
            "}"));
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("admin"), eq("getLastStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(mock(KuzzResponseListener.class));
    verify(kuzzle, times(1)).query(any(String.class), eq("admin"), eq("getLastStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetStatisticsWithoutListener() {
    kuzzle.getStatistics(null);
  }

  @Test(expected = RuntimeException.class)
  public void testGetLastStatisticsException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", mock(JSONObject.class)));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("admin"), eq("getLastStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetLastStatisticsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(String.class), eq("admin"), eq("getLastStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetStatisticsException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONArray.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", mock(JSONArray.class))));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(new Date().toString(), listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetStatisticsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(new Date().toString(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetStatsIllegalListener() {
    kuzzle.getStatistics(new Date().toString(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetStatsIllegalTimestamp() {
    kuzzle.getStatistics((String)null, mock(KuzzResponseListener.class));
  }

  @Test
  public void testGetStatistics() throws JSONException {
    kuzzle = spy(kuzzle);
    final JSONObject response = new JSONObject("{ result: {\n" +
        "    total: 25,\n" +
        "    hits: [\n" +
        "      {\n" +
        "        completedRequests: {\n" +
        "          websocket: 148,\n" +
        "          rest: 24,\n" +
        "          mq: 78\n" +
        "        },\n" +
        "        failedRequests: {\n" +
        "          websocket: 3\n" +
        "        },\n" +
        "        ongoingRequests: {\n" +
        "          mq: 8,\n" +
        "          rest: 2\n" +
        "        },\n" +
        "        connections: {\n" +
        "          websocket: 13\n" +
        "        },\n" +
        "        \"timestamp\": \"2016-01-13T13:46:19.917Z\"\n" +
        "      },\n" +
        "      ...\n" +
        "    ]\n" +
        "  }" +
        "}");

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(null);
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics("2015-11-15T13:36:45.558Z", mock(KuzzResponseListener.class));
    verify(kuzzle, times(1)).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testDequeue() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(10000);
    options.setAutoReplay(true);
    options.setReplayInterval(1);
    options.setConnect(Mode.MANUAL);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.setSocket(s);
    kuzzle.setAutoReconnect(true);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzle.connect();

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
    kuzzle.connect();
    kuzzle.setAutoReplay(false);
    kuzzle.replayQueue();
    verify(s, atLeastOnce()).emit(eq("kuzzle"), eq(query));
  }

  @Test
  public void testQueueMaxSize() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1000);
    options.setQueueMaxSize(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.setSocket(s);
    kuzzle.setAutoReconnect(true);
    kuzzle.setAutoQueue(true);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzle.connect();
    kuzzle.query("test", "test", "test", mock(JSONObject.class), mock(OnQueryDoneListener.class));
    kuzzle.query("test2", "test2", "test2", mock(JSONObject.class));
    kuzzle.query("test3", "test3", "test3", new JSONObject());
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    assertEquals(kuzzle.getOfflineQueue().peek().getQuery().getString("controller"), "test3");
    verify(s, never()).emit(any(String.class), any(Emitter.Listener.class));
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
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    JSONObject query = new JSONObject();
    query.put("requestId", "42");
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.setSocket(s);
    kuzzle.connect();
    kuzzle.query("test", "test", "test", query, null, mock(OnQueryDoneListener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq(Socket.EVENT_RECONNECT), any(Emitter.Listener.class));
    verify(s, atLeastOnce()).once(eq("42"), any(Emitter.Listener.class));
  }

  @Test
  public void testMaxTTLWithReconnectListener() throws JSONException, URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setReplayInterval(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setOfflineMode(Mode.AUTO);

    KuzzleQueryObject o = new KuzzleQueryObject();
    o.setTimestamp(new Date());
    o.setAction("test");
    JSONObject query = new JSONObject("{\"controller\":\"test3\",\"metadata\":{},\"requestId\":\"a476ae61-497e-4338-b4dd-751ac22c6b61\",\"action\":\"test3\",\"collection\":\"test3\"}");
    o.setQuery(query);

    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.setSocket(s);
    kuzzle.setAutoReconnect(true);

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
    kuzzle.setAutoReconnect(true);
    kuzzle.connect();
    kuzzle.setOfflineQueue(o);

    IEventListener listener = mock(IEventListener.class);
    kuzzle.addListener(EventType.RECONNECTED, listener);
    kuzzle.connect();
    verify(listener, times(1)).trigger();
  }

  @Test
  public void testRenewSubscriptionsAfterReconnection() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options, mock(KuzzResponseListener.class));
    kuzzle.setSocket(s);
    final Kuzzle kuzzleSpy = spy(kuzzle);
    kuzzleSpy.setAutoReconnect(true);
    kuzzleSpy.addSubscription("42", new KuzzleRoom(new KuzzleDataCollection(kuzzleSpy, "test")));
    kuzzleSpy.addSubscription("43", new KuzzleRoom(new KuzzleDataCollection(kuzzleSpy, "test2")));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
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
    kuzzleSpy.connect();
    verify(kuzzleSpy, times(2)).query(any(String.class), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(kuzzleSpy.getSubscriptions().size(), 2);
  }

  @Test
  public void testAutoReconnect() {
    kuzzle.setAutoReconnect(false);
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        kuzzle.setSocket(s);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzle.connect();
    verify(kuzzle, times(1)).disconnect();
  }

  @Test
  public void testConnectNotValid() throws URISyntaxException {
    listener = spy(listener);
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", listener);
    kuzzle.setSocket(s);
    kuzzle = spy(kuzzle);
    when(kuzzle.isValidState()).thenReturn(false);
    kuzzle.connect();
    verify(listener, atLeastOnce()).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testOnConnectError() throws URISyntaxException {
    listener = spy(listener);
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options, listener);
    kuzzle.setSocket(s);
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
  public void testFlushQueue() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);

    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
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

  @Test(expected = IllegalArgumentException.class)
  public void testListCollectionsWithIllegalListener() {
    kuzzle.listCollections(null);
  }

  @Test(expected = RuntimeException.class)
  public void testListCollectionException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONArray.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", new JSONObject().put("collections", mock(JSONArray.class))));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("read"), eq("listCollections"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.listCollections(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testListCollectionQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(String.class), eq("read"), eq("listCollections"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.listCollections(listener);
  }

  @Test
  public void testListCollections() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(1);
    options.setAutoReplay(true);
    options.setConnect(Mode.MANUAL);
    options.setAutoReconnect(true);
    options.setOfflineMode(Mode.AUTO);

    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", new JSONObject().put("collections", mock(JSONArray.class))));
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("read"), eq("listCollections"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.listCollections(mock(KuzzResponseListener.class));
    kuzzle.listCollections(options, mock(KuzzResponseListener.class));
    verify(kuzzle, times(2)).query(any(String.class), eq("read"), eq("listCollections"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testManualQueuing() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueueTTL(10000);
    options.setReplayInterval(1);
    options.setConnect(Mode.MANUAL);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.setSocket(s);
    kuzzle.setAutoReconnect(true);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    kuzzle.connect();
    kuzzle.startQueuing();
    JSONObject query = new JSONObject();
    query.put("requestId", "42");
    kuzzle.query("test", "test", "test", query, null, null);
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    kuzzle.flushQueue();
    kuzzle.stopQueing();
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
    kuzzle.query("test", "test", "test", query, null, null);
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
  }

  @Test
  public void testQueuable() throws URISyntaxException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        kuzzle.setAutoReconnect(false);
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    KuzzleOptions options = new KuzzleOptions();
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.connect();
    kuzzle.listCollections(mock(KuzzResponseListener.class));
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    kuzzle.flushQueue();
    options.setQueuable(false);
    kuzzle.listCollections(options, mock(KuzzResponseListener.class));
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
  }

  @Test
  public void testDeleteSubscription() throws JSONException {
    kuzzle.setHeaders(new JSONObject());
    kuzzle = spy(kuzzle);
    KuzzleDataCollection collection = mock(KuzzleDataCollection.class);
    when(collection.getKuzzle()).thenReturn(kuzzle);
    when(collection.getHeaders()).thenReturn(new JSONObject());
    KuzzleRoom room = new KuzzleRoom(collection);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("roomId", "42");
        result.put("channel", "channel");
        //Call callback with response
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", result));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("roomId", "42");
        result.put("channel", "channel");
        //Call callback with response
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("result", result));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("subscribe"), eq("off"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    room.renew(new JSONObject(), null);
    assertEquals(kuzzle.getSubscriptions().size(), 1);
    room.unsubscribe();
    assertEquals(kuzzle.getSubscriptions().size(), 0);
  }

  @Test
  public void testDefaultIndex() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    kuzzle.setSocket(s);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");

    kuzzle.query("collection", "controller", "action", jsonObj, options, null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getString("index"), "testIndex");
  }

  @Test
  public void testMultiIndex() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "%index", options);
    kuzzle.setSocket(s);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");

    kuzzle.query("collection", "controller", "action", jsonObj, options, null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getString("index"), "%index");
  }

  @Test
  public void testLoginWithConstructor() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setLoginStrategy("local");
    options.setLoginUsername("username");
    options.setLoginPassword("password");
    options.setLoginExpiresIn(30000);
    options.setConnect(Mode.MANUAL);
    options.setQueuable(false);
    kuzzle = new Kuzzle("http://localhost:7512", "index", options);
    kuzzle.setSocket(s);
    kuzzle = spy(kuzzle);
    kuzzle.setSocket(s);

    final JSONObject mockResponse = new JSONObject();
    mockResponse.put("requestId", "42");
    mockResponse.put("jwt", "jwtToken");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(mockResponse);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("auth"), eq("login"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call();
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    kuzzle.connect();
    verify(kuzzle, times(1)).query(any(String.class), eq("auth"), eq("login"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    // check if token
    kuzzle.query("collection", "controller", "action", mockResponse, options, mock(OnQueryDoneListener.class));
    verify(s).emit(eq("kuzzle"), eq(mockResponse));
  }

  @Test
  public void testLogin() throws JSONException {
    Kuzzle kuzzleSpy = spy(kuzzle);
    kuzzleSpy.setSocket(s);
    KuzzResponseListener listenerSpy = spy(listener);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(new JSONObject().put("_type", "type"));
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzleSpy).query(any(String.class), eq("auth"), eq("login"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzleSpy.login("local", "username", "password");
    kuzzleSpy.login("local", "username", "password", 42);
    kuzzleSpy.login("local", "username", "password", 42, new KuzzleOptions());
    kuzzleSpy.login("local", "username", "password", 42, new KuzzleOptions(), listenerSpy);
    kuzzleSpy.login("local", "username", "password", 42, listenerSpy);
    kuzzleSpy.login("local", "username", "password", new KuzzleOptions());
    kuzzleSpy.login("local", "username", "password", new KuzzleOptions(), listenerSpy);
    verify(kuzzleSpy, times(7)).query(any(String.class), eq("auth"), eq("login"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    verify(listenerSpy, atLeastOnce()).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testLogout() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setLoginStrategy("local");
    options.setLoginUsername("username");
    options.setLoginPassword("password");
    options.setLoginExpiresIn(30000);
    options.setConnect(Mode.MANUAL);
    options.setQueuable(false);
    kuzzle = new Kuzzle("http://localhost:7512", "index", options);
    kuzzle.setSocket(s);
    kuzzle = spy(kuzzle);
    kuzzle.setSocket(s);

    final JSONObject response = new JSONObject();
    response.put("jwt", "jwtToken");
    response.put("requestId", "42");
    response.put("_type", "collection");

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("auth"), eq("login"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.login("local", "username", "password");

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(String.class), eq("auth"), eq("logout"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.logout();
    kuzzle.logout(mock(KuzzResponseListener.class));
    kuzzle.logout(mock(KuzzleOptions.class));
    kuzzle.logout(mock(KuzzleOptions.class), mock(KuzzResponseListener.class));
    verify(kuzzle, times(1)).query(any(String.class), eq("auth"), eq("login"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    verify(kuzzle, times(4)).query(any(String.class), eq("auth"), eq("logout"), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    response.remove("jwt");
    kuzzle.query("collection", "controller", "action", response, options, mock(OnQueryDoneListener.class));
    verify(s).emit(eq("kuzzle"), eq(response));
  }

}
