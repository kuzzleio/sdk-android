package io.kuzzle.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.IKuzzleEventListener;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
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
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options);
    s = mock(Socket.class);
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
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options, mock(KuzzleResponseListener.class));
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
    assertEquals(kuzzle.getDefaultIndex(), "testIndex");
    assertNotNull(kuzzle);
    verify(kuzzle, never()).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = RuntimeException.class)
  public void testIsValid() throws Exception {
    kuzzle.disconnect();
    kuzzle.isValid();
  }

  @Test
  public void testAddListener() {
    assertEquals(kuzzle.getEventListeners(), new ArrayList<IKuzzleEventListener>());
    kuzzle.addListener(KuzzleEvent.connected, mock(IKuzzleEventListener.class));
    assertEquals(kuzzle.getEventListeners().get(0).getType(), KuzzleEvent.connected);
  }

  @Test
  public void testDataCollectionFactory() {
    assertEquals(kuzzle.dataCollectionFactory("test").fetchDocument("test", mock(KuzzleResponseListener.class)).getCollection(), "test");
    assertEquals(kuzzle.dataCollectionFactory("test2").fetchDocument("test2", mock(KuzzleResponseListener.class)).getCollection(), "test2");
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("now", mock(JSONObject.class))));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.now(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testNowQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.now(listener);
  }

  @Test
  public void testNow() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("now", 42424242)));
        ((OnQueryDoneListener)invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.now(listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "now");
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testRemoveAllListeners() {
    kuzzle.addListener(KuzzleEvent.connected, null);
    assertEquals(kuzzle.getEventListeners().get(0).getType(), KuzzleEvent.connected);
    kuzzle.removeAllListeners();
    kuzzle.getEventListeners().get(0);
  }

  @Test
  public void testRemoveAllListenersType() {
    kuzzle.addListener(KuzzleEvent.connected, null);
    kuzzle.addListener(KuzzleEvent.disconnected, null);
    assertEquals(kuzzle.getEventListeners().size(), 2);
    kuzzle.removeAllListeners(KuzzleEvent.connected);
    assertEquals(kuzzle.getEventListeners().size(), 1);
  }

  @Test
  public void testRemoveListener() {
    assertNotNull(kuzzle.getSocket());
    kuzzle = spy(kuzzle);
    String id = kuzzle.addListener(KuzzleEvent.disconnected, mock(IKuzzleEventListener.class));
    String id2 = kuzzle.addListener(KuzzleEvent.connected, mock(IKuzzleEventListener.class));
    assertEquals(kuzzle.getEventListeners().get(0).getType(), KuzzleEvent.disconnected);
    assertEquals(kuzzle.getEventListeners().get(1).getType(), KuzzleEvent.connected);
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

    kuzzle.query(QueryArgsHelper.makeQueryArgs("controller", "action"), jsonObj, options, null);
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
    kuzzle.query(QueryArgsHelper.makeQueryArgs("controller", "action"), jsonObj, options);
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", mock(JSONObject.class))));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetAllStatisticsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", response));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(new KuzzleResponseListener<JSONArray>() {
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
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getAllStats");
  }

  @Test
  public void testGetAllStatsError() throws JSONException {
    kuzzle = spy(kuzzle);

    final JSONObject responseError = new JSONObject();
    responseError.put("error", "rorre");
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(responseError);
        return null;
      }
    }).when(kuzzle).query(eq(QueryArgsHelper.makeQueryArgs("admin", "getAllStats")), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getAllStatistics(new KuzzleResponseListener<JSONArray>() {
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
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getAllStats");
  }

  @Test
  public void testGetLastStatistic() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject("{ result: {\n" +
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", mock(JSONObject.class)));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getLastStats");
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", mock(JSONObject.class)));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetLastStatisticsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", mock(JSONArray.class))));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(new Date().toString(), listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetStatisticsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics(new Date().toString(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetStatsIllegalListener() {
    kuzzle.getStatistics(new Date().toString(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetStatsIllegalTimestamp() {
    kuzzle.getStatistics((String) null, mock(KuzzleResponseListener.class));
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getStatistics("2015-11-15T13:36:45.558Z", mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "admin");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getStats");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetServerInfoIllegalListener() {
    kuzzle.getServerInfo(null);
  }

  @Test(expected = RuntimeException.class)
  public void testGetServerInfoException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", mock(JSONObject.class)));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getServerInfo(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetServerInfoQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getServerInfo(listener);
  }

  @Test
  public void testGetServerInfo() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("serverInfo", mock(JSONObject.class))));
        ((OnQueryDoneListener)invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getServerInfo(listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "serverInfo");
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
    kuzzle.query(QueryArgsHelper.makeQueryArgs("test", "test"), mock(JSONObject.class), mock(OnQueryDoneListener.class));
    kuzzle.query(QueryArgsHelper.makeQueryArgs("test2", "test2"), mock(JSONObject.class));
    kuzzle.query(QueryArgsHelper.makeQueryArgs("test3", "test3"), new JSONObject());
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
    kuzzle.query(QueryArgsHelper.makeQueryArgs("test", "test"), query, null, mock(OnQueryDoneListener.class));
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

    IKuzzleEventListener listener = mock(IKuzzleEventListener.class);
    kuzzle.addListener(KuzzleEvent.reconnected, listener);
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
    kuzzle = new Kuzzle("http://localhost:7512", "testIndex", options, mock(KuzzleResponseListener.class));
    kuzzle.setSocket(s);
    final Kuzzle kuzzleSpy = spy(kuzzle);
    kuzzleSpy.setAutoReconnect(true);
    kuzzleSpy.addSubscription("42", new KuzzleRoom(new KuzzleDataCollection(kuzzleSpy, "index", "test")));
    kuzzleSpy.addSubscription("43", new KuzzleRoom(new KuzzleDataCollection(kuzzleSpy, "index", "test2")));
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
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzleSpy, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "on");
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
    kuzzle.listCollections("index", null);
  }

  @Test(expected = RuntimeException.class)
  public void testListCollectionException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONArray.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("collections", mock(JSONArray.class))));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.listCollections(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testListCollectionQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.listCollections(listener);
  }

  @Test
  public void testListCollections() throws URISyntaxException, JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("collections", mock(JSONObject.class))));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.listCollections(listener);
    kuzzle.listCollections(mock(KuzzleOptions.class), listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "listCollections");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListIndexesWithIllegalListener() {
    kuzzle.listIndexes(null);
  }

  @Test(expected = RuntimeException.class)
  public void testListIndexesException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONArray.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", new JSONArray().put("foo"))));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.listIndexes(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testListIndexesQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.listIndexes(listener);
  }

  @Test
  public void testListIndexes() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", new JSONArray().put("foo"))));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.listIndexes(listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "read");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "listIndexes");
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
    kuzzle.query(QueryArgsHelper.makeQueryArgs("test", "test"), query, null, null);
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    kuzzle.flushQueue();
    kuzzle.stopQueing();
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
    kuzzle.query(QueryArgsHelper.makeQueryArgs("test", "test"), query, null, null);
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
    kuzzle.listCollections(mock(KuzzleResponseListener.class));
    assertEquals(kuzzle.getOfflineQueue().size(), 1);
    kuzzle.flushQueue();
    options.setQueuable(false);
    kuzzle.listCollections(options, mock(KuzzleResponseListener.class));
    assertEquals(kuzzle.getOfflineQueue().size(), 0);
  }

  @Test
  public void testDeleteSubscription() throws JSONException {
    kuzzle.setHeaders(new JSONObject());
    kuzzle = spy(kuzzle);
    KuzzleDataCollection collection = mock(KuzzleDataCollection.class);
    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.controller = "subscribe";
    args.action = "off";
    when(collection.makeQueryArgs(any(String.class), any(String.class))).thenReturn(args);
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
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", result));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    room.renew(new JSONObject(), mock(KuzzleResponseListener.class));
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

    kuzzle.query(QueryArgsHelper.makeQueryArgs("controller", "action"), jsonObj, options, null);
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

    kuzzle.query(QueryArgsHelper.makeQueryArgs("controller", "action"), jsonObj, options, null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getString("index"), "%index");
  }

  @Test
  public void testLogin() throws JSONException {
    kuzzle = spy(kuzzle);
    kuzzle.setSocket(s);
    KuzzleResponseListener listenerSpy = spy(listener);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("_type", "type"));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(eq(QueryArgsHelper.makeQueryArgs("auth", "login")), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.login("local", new JSONObject().put("username", "username").put("password", "password"));
    kuzzle.login("local", new JSONObject().put("username", "username").put("password", "password"), 42);
    kuzzle.login("local", new JSONObject().put("username", "username").put("password", "password"), 42, listenerSpy);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(3)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "auth");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "login");
  }

  @Test(expected = RuntimeException.class)
  public void testLogoutException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONArray.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("hits", new JSONArray().put("foo"))));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.logout(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testLogoutQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.logout(listener);
  }

  @Test
  public void testLogout() throws URISyntaxException, JSONException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", "index", options);
    kuzzle.setSocket(s);
    kuzzle = spy(kuzzle);
    kuzzle.setSocket(s);

    final JSONObject response = new JSONObject("{\"result\": {\"jwt\": \"jwtToken\"}}");

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.login("local", new JSONObject().put("username", "username").put("password", "password"));
    kuzzle.logout();
    kuzzle.logout(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(3)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "auth");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "logout");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetDefaultIndexIllegalIndex() {
    kuzzle.setDefaultIndex(null);
  }

  @Test
  public void testSetDefaultIndex() throws JSONException {
    kuzzle.setDefaultIndex("test-index");
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");
    kuzzle.query(QueryArgsHelper.makeQueryArgs("controller", "action"), jsonObj, mock(KuzzleOptions.class), null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getString("index"), "test-index");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckTokenWithIllegalListener() {
    kuzzle.checkToken("token", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckTokenWithIllegalToken() {
    kuzzle.checkToken(null, mock(KuzzleResponseListener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCheckTokenWithEmptyToken() {
    kuzzle.checkToken("", null);
  }

  @Test(expected = RuntimeException.class)
  public void testCheckTokenException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("valid", true).put("expiresAt", new Date().getTime())));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.checkToken("token", listener);
  }

  @Test(expected = RuntimeException.class)
  public void testCheckTokenQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.checkToken("token", listener);
  }

  @Test
  public void testCheckTokenValid() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("valid", true).put("expiresAt", new Date().getTime())));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.checkToken("token-42", mock(KuzzleResponseListener.class));
    kuzzle.checkToken("token-42", mock(KuzzleOptions.class), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "auth");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "checkToken");
  }

  @Test
  public void testCheckTokenInvalid() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject().put("valid", false).put("state", "error")));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.checkToken("token-42", mock(KuzzleResponseListener.class));
    kuzzle.checkToken("token-42", mock(KuzzleOptions.class), mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "auth");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "checkToken");
  }

  @Test
  public void testWhoAmIValid() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject()
          .put("result", new JSONObject()
            .put("_id", "test")
            .put("_source", new JSONObject()
              .put("profile", new JSONObject()
                .put("_id", "admin")
                .put("roles", "")))));

        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzle.whoAmI(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "auth");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getCurrentUser");
  }

  @Test(expected = RuntimeException.class)
  public void testWhoAmIInvalid() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.whoAmI(mock(KuzzleResponseListener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWhoAmINoListener() throws JSONException {
    kuzzle.whoAmI(null);
  }

  @Test(expected = RuntimeException.class)
  public void testWhoAmIInvalidResponse() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[3]).onSuccess(new JSONObject());
        ((OnQueryDoneListener)invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.whoAmI(mock(KuzzleResponseListener.class));
  }
}
