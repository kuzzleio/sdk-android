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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleTest {

  private Kuzzle kuzzle;
  private Socket s;

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
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(Socket.EVENT_CONNECT), any(Emitter.Listener.class));
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
      }

      @Override
      public void onError(JSONObject error) {
      }
    });
    kuzzle.setSocket(s);
    kuzzle.connect();
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
    assertEquals(options.getIndex(), "mainindex");
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
    kuzzle.addListener(EventType.CONNECTED, event);
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.CONNECTED);
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
    kuzzle.addListener(EventType.CONNECTED, null);
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.CONNECTED);
    kuzzle.removeAllListeners();
    kuzzle.getEventListeners().get(0);
  }

  @Test
  public void testRemoveAllListenersType() throws Exception {
    kuzzle.addListener(EventType.CONNECTED, null);
    kuzzle.addListener(EventType.DISCONNECTED, null);
    assertEquals(kuzzle.getEventListeners().size(), 2);
    kuzzle.removeAllListeners(EventType.CONNECTED);
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
    String id = spy.addListener(EventType.DISCONNECTED, event);
    String id2 = spy.addListener(EventType.CONNECTED, event);
    assertEquals(spy.getEventListeners().get(0).getType(), EventType.DISCONNECTED);
    assertEquals(spy.getEventListeners().get(1).getType(), EventType.CONNECTED);
    spy.removeListener(EventType.CONNECTED, id2);
    spy.removeListener(EventType.DISCONNECTED, id);
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

    final JSONObject response = new JSONObject("{\"statistics\":[{\"connections\":{},\"ongoingRequests\":{},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:19:25.153Z\"},{\"connections\":{},\"ongoingRequests\":{},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:19:35.155Z\"},{\"connections\":{},\"ongoingRequests\":{},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:19:45.155Z\"},{\"connections\":{},\"ongoingRequests\":{},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:19:55.156Z\"},{\"connections\":{},\"ongoingRequests\":{},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:20:05.156Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":4},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:20:15.156Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:20:25.158Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:20:35.158Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:20:45.159Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:20:55.160Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:21:05.161Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:21:15.161Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:21:25.162Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:21:35.162Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:21:45.163Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:21:55.163Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:22:05.164Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:22:15.164Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:22:25.165Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:22:35.165Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:22:45.167Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:22:55.167Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:23:05.168Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:23:15.168Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:23:25.168Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:23:35.170Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:23:45.170Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:23:55.170Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:24:05.171Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:24:15.173Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:24:25.174Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:24:35.175Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:24:45.177Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:24:55.177Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:25:05.177Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:25:15.177Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":1},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:25:25.179Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":4},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:25:35.180Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:25:45.181Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:25:55.182Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:26:05.182Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:26:15.185Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:26:25.185Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:26:35.185Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:26:45.185Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:26:55.185Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:27:05.185Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:27:15.185Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:27:25.185Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:27:35.185Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:27:45.187Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":8},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:27:55.187Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:28:05.190Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:28:15.191Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:28:25.192Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:28:35.192Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:28:45.193Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:28:55.194Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:29:05.194Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:29:15.194Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:29:25.194Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:29:35.194Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:29:45.194Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:29:55.195Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:30:05.196Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:30:15.198Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:30:25.198Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:30:35.198Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:30:45.198Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:30:55.198Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:31:05.200Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:31:15.200Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:31:25.203Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:31:35.203Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:31:45.205Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:31:55.205Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:32:05.205Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:32:15.206Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:32:25.206Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:32:35.207Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:32:45.207Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:32:55.209Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:33:05.211Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:33:15.213Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:33:25.213Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:33:35.214Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:33:45.214Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:33:55.215Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:34:05.215Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:34:15.216Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:34:25.216Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:34:35.217Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:34:45.218Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:34:55.219Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:35:05.221Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:35:15.221Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:35:25.222Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:35:35.223Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:35:45.223Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:35:55.225Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:36:05.227Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:36:15.227Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:36:25.227Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:36:35.228Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:36:45.231Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:36:55.231Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:37:05.231Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:37:15.231Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:37:25.230Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:37:35.231Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:37:45.231Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:37:55.233Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:38:05.233Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:38:15.233Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:38:25.235Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:38:35.235Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:38:45.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:38:55.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:39:05.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:39:15.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:39:25.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:39:35.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:39:45.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:39:55.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:40:05.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:40:15.236Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:40:25.238Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:40:35.240Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:40:45.241Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:40:55.241Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:41:05.241Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:41:15.241Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:41:25.243Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:41:35.243Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:41:45.245Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:41:55.245Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:42:05.246Z\"},{\"connections\":{\"websocket\":4},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:42:15.246Z\"},{\"connections\":{\"websocket\":5},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:42:25.247Z\"},{\"connections\":{\"websocket\":5},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:42:35.247Z\"},{\"connections\":{\"websocket\":5},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:42:45.247Z\"},{\"connections\":{\"websocket\":5},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:42:55.248Z\"},{\"connections\":{\"websocket\":5},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:43:05.248Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:43:15.248Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:43:25.248Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:43:35.250Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:43:45.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:43:55.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:44:05.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:44:15.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:44:25.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:44:35.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:44:45.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:44:55.252Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:45:05.252Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:45:15.252Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:45:25.252Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:45:35.254Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:45:45.254Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:45:55.254Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:46:05.254Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:46:15.256Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:46:25.256Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:46:35.256Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:46:45.256Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:46:55.256Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:47:05.258Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:47:15.258Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:47:25.258Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:47:35.258Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:47:45.258Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:47:55.258Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:48:05.260Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:48:15.260Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:48:25.260Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:48:35.260Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:48:45.260Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:48:55.260Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:49:05.261Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:49:15.261Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:49:25.261Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:49:35.261Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:49:45.261Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:49:55.261Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:50:05.262Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:50:15.262Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:50:25.262Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:50:35.262Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:50:45.262Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:50:55.264Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:51:05.266Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:51:15.266Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:51:25.267Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:51:35.268Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:51:45.268Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:51:55.268Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:52:05.268Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:52:15.268Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:52:25.268Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:52:35.270Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:52:45.270Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:52:55.272Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:53:05.272Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":8},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:53:15.273Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:53:25.273Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:53:35.274Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":7},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:53:45.274Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":7},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:53:55.276Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:54:05.276Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:54:15.277Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":8},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:54:25.277Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:54:35.279Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:54:45.280Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:54:55.281Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:55:05.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:55:15.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:55:25.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:55:35.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:55:45.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:55:55.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:56:05.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:56:15.282Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:56:25.284Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:56:35.285Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:56:45.287Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:56:55.288Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:57:05.288Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:57:15.288Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:57:25.288Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:57:35.288Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:57:45.288Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":9},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:57:55.288Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:58:05.291Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:58:15.291Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:58:25.291Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":4},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:58:35.291Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":5},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:58:45.292Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:58:55.292Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:59:05.293Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:59:15.294Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:59:25.294Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":9},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:59:35.295Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:59:45.295Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T09:59:55.297Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:00:05.298Z\"},{\"connections\":{},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:00:15.298Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":1},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:00:25.298Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{\"websocket\":8},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:00:35.298Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:00:45.298Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:00:55.298Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:01:05.299Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:01:15.299Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:01:25.302Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:01:35.303Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:01:45.303Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:01:55.305Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:02:05.305Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:02:15.307Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:02:25.306Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:02:35.307Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:02:45.309Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:02:55.309Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:03:05.309Z\"},{\"connections\":{\"websocket\":1},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:03:15.310Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:03:25.311Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:03:35.311Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:03:45.312Z\"},{\"connections\":{\"websocket\":3},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:03:55.313Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:04:05.313Z\"},{\"connections\":{\"websocket\":2},\"ongoingRequests\":{\"websocket\":0},\"completedRequests\":{},\"failedRequests\":{},\"timestamp\":\"2015-12-11T10:04:15.313Z\"}],\"requestId\":\"aad7549d-6bd9-43da-a07f-773d36e4a9cd\",\"controller\":\"admin\",\"action\":\"getAllStats\",\"metadata\":{},\"_source\":{},\"state\":\"done\"}");

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
              assertEquals(array.getJSONObject(i).get(key), response.getJSONArray("statistics").getJSONObject(i).get(key));
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
    spy.getStatistics(null);
    spy.getStatistics("2015-11-15T13:36:45.558Z", new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    verify(spy, times(2)).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(ResponseListener.class));
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
    kuzzle.connect();

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
    kuzzle.connect();
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
    kuzzle.connect();
    kuzzle.setOfflineQueue(o);

    IEventListener listener = mock(IEventListener.class);
    kuzzle.addListener(EventType.RECONNECTED, listener);
    kuzzle.connect();
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
    ResponseListener listener = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
    ResponseListener spyListener = spy(listener);
    kuzzle = new Kuzzle("http://localhost:7512", options, spyListener);
    kuzzle.setSocket(s);
    final Kuzzle kuzzleSpy = spy(kuzzle);
    kuzzleSpy.addSubscription("42", new KuzzleRoom(new KuzzleDataCollection(kuzzleSpy, "test")));
    kuzzleSpy.addSubscription("43", new KuzzleRoom(new KuzzleDataCollection(kuzzleSpy, "test2")));

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
    kuzzleSpy.connect();
    verify(kuzzleSpy, times(2)).query(any(String.class), eq("subscribe"), eq("on"), any(JSONObject.class), any(KuzzleOptions.class), any(ResponseListener.class));
    assertEquals(kuzzleSpy.getSubscriptions().size(), 2);
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
    kuzzleSpy.connect();
    verify(kuzzleSpy, times(1)).logout();
  }

  @Test
  public void testConnectNotValid() throws Exception {
    ResponseListener listener = new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
    ResponseListener spy = spy(listener);
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", spy);
    kuzzle.setSocket(s);
    Kuzzle kuzzleSpy = spy(kuzzle);
    when(kuzzleSpy.isValidSate()).thenReturn(false);
    kuzzleSpy.connect();
    verify(spy, times(1)).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testOnConnectError() throws Exception {
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
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options, listenerSpy);
    kuzzle.setSocket(s);
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
    kuzzleSpy.connect();
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
    kuzzle.connect();
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
    kuzzle.connect();
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
        result.put("channel", "channel");
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

  @Test
  public void testDefaultIndex() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");

    kuzzle.query("collection", "controller", "action", jsonObj, options, null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getString("index"), "mainindex");
  }

  @Test
  public void testMultiIndex() throws Exception {
    KuzzleOptions options = new KuzzleOptions();
    options.setIndex("%test");
    options.setQueuable(false);
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(s);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");

    kuzzle.query("collection", "controller", "action", jsonObj, options, null);
    verify(s).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getString("index"), "%test");
  }

}
