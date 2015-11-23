package io.kuzzle.sdk.test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.IEventListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KuzzleTest {

  private Kuzzle kuzzle;

  @Test(expected = IllegalArgumentException.class)
  public void testBadUriConnection() throws URISyntaxException {
    kuzzle = new Kuzzle(null);
  }

  @Test
  public void testKuzzleConnection() throws URISyntaxException, IOException, JSONException {
    kuzzle = new Kuzzle("http://localhost:7512");
    assertNotNull(kuzzle);
  }

  @Test(expected = KuzzleException.class)
  public void testIsValid() throws URISyntaxException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
    kuzzle.logout();
    kuzzle.isValid();
  }

  @Test
  public void testAddListener() throws URISyntaxException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
    assertEquals(kuzzle.getEventListeners(), new ArrayList<IEventListener>());
    IEventListener event = new IEventListener() {
      @Override
      public void trigger(String subscriptionId, JSONObject result) {
      }
    };
    List<IEventListener> listenerList = new ArrayList<>();
    listenerList.add(event);
    kuzzle.addListener(EventType.SUBSCRIBED, event);
    assertEquals(kuzzle.getEventListeners().get(0).getType(), EventType.SUBSCRIBED);
  }

  @Test
  public void testDataCollectionFactory() throws URISyntaxException, IOException, JSONException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
    assertEquals(kuzzle.dataCollectionFactory("test").fetch("test", null).getCollection(), "faketest");
    assertEquals(kuzzle.dataCollectionFactory("test2").fetch("test2", null).getCollection(), "test2");
  }

  @Test
  public void testLogout() throws URISyntaxException {
    kuzzle = new Kuzzle("http://localhost:7512");
    assertNotNull(kuzzle.getSocket());
    kuzzle.logout();
    assertNull(kuzzle.getSocket());
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testRemoveListener() throws URISyntaxException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
    IEventListener event = new IEventListener() {
      @Override
      public void trigger(String subscriptionId, JSONObject result) {
      }
    };
    String id = kuzzle.addListener(EventType.UNSUBSCRIBED, event);
    String id2 = kuzzle.addListener(EventType.SUBSCRIBED, event);
    kuzzle.removeListener(EventType.SUBSCRIBED, id2);
    kuzzle.removeListener(EventType.UNSUBSCRIBED, id);
    kuzzle.getEventListeners().get(0);
  }

  @Test
  public void testAddHeaders() throws JSONException, URISyntaxException {
    kuzzle = new Kuzzle("http://localhost:7512");
    JSONObject query = new JSONObject();
    JSONObject headers = new JSONObject();
    headers.put("testPurpose", "test");
    kuzzle.addHeaders(query, headers);
    assertEquals(query.get("testPurpose"), "test");
  }

  @Test
  public void testMetadata() throws URISyntaxException, JSONException, IOException, KuzzleException {
    Socket socket = mock(Socket.class);
    KuzzleOptions options = new KuzzleOptions();
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    options.setMetadata(meta);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");

    kuzzle = new Kuzzle("http://localhost:7512");
    kuzzle.setSocket(socket);
    kuzzle.query("collection", "controller", "action", jsonObj, options, null);
    verify(socket).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

  @Test
  public void testGetAllStats() throws Exception {
    kuzzle = new Kuzzle("http://localhost:7512");
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
      public void onSuccess(JSONObject object) throws Exception {
        JSONArray array = object.getJSONArray("statistics");
        for (int i = 0; i < array.length(); i++) {
          for (Iterator ite = array.getJSONObject(i).keys(); ite.hasNext(); ) {
            String key = (String) ite.next();
            assertEquals(array.getJSONObject(i).get(key), responseCallback.getJSONArray("statistics").getJSONObject(i).get(key));
          }
        }
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    verify(spy, times(1)).query(any(String.class), eq("admin"), eq("getAllStats"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testGetLastStatistic() throws URISyntaxException, JSONException, IOException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
    Kuzzle spy = spy(kuzzle);
    final JSONObject response = new JSONObject();
    JSONObject stats = new JSONObject();
    JSONObject frame = new JSONObject();

    //{"statistics":{"2015-11-19T16:24:46.401Z":{"connections":{},"ongoingRequests":{"rest":0},"completedRequests":{},"failedRequests":{}}},"requestId":"26e20fba-caa6-459a-84a6-e115e3105ce1","controller":"admin","action":"getStats","metadata":{},"_source":{"since":"Thu Nov 19 17:24:47 GMT+01:00 2015"}}
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
      public void onSuccess(JSONObject object) throws Exception {
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    verify(spy, times(1)).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testGetNoStatistic() throws URISyntaxException, JSONException, IOException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
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
    spy.getStatistics("2015-11-15T13:36:45.558Z", new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) throws Exception {

      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    });
    verify(spy, times(1)).query(any(String.class), eq("admin"), eq("getStats"), any(JSONObject.class), any(ResponseListener.class));
  }

}
