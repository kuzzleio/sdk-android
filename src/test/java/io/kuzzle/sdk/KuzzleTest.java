package io.kuzzle.sdk;

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

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.listeners.IEventListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

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
    kuzzle.setSocket(mock(Socket.class));
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
    assertEquals(kuzzle.dataCollectionFactory("test").fetchDocument("test", null).getCollection(), "test");
    assertEquals(kuzzle.dataCollectionFactory("test2").fetchDocument("test2", null).getCollection(), "test2");
  }

  @Test
  public void testLogout() throws URISyntaxException {
    kuzzle = new Kuzzle("http://localhost:7512");
    assertNotNull(kuzzle.getSocket());
    kuzzle.logout();
    assertNull(kuzzle.getSocket());
  }

  @Test
  public void testRemoveListener() throws URISyntaxException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
    kuzzle.setSocket(mock(Socket.class));
    assertNotNull(kuzzle.getSocket());
    Kuzzle spy = spy(kuzzle);
    IEventListener event = new IEventListener() {
      @Override
      public void trigger(String subscriptionId, JSONObject result) {
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
  public void testSetHeaders() throws URISyntaxException, JSONException {
    kuzzle = new Kuzzle("http://localhost:7512");
    JSONObject content = new JSONObject();
    content.put("foo", "bar");
    kuzzle.setHeaders(content);
    assertEquals(kuzzle.getHeaders().getString("foo"), "bar");
    content.put("foo", "baz");
    kuzzle.setHeaders(content, true);
    assertEquals(kuzzle.getHeaders().getString("foo"), "baz");
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
  public void testMetadataOptions() throws URISyntaxException, JSONException, IOException, KuzzleException {
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
  public void testMetadataInKuzzle() throws URISyntaxException, KuzzleException, IOException, JSONException {
    Socket socket = mock(Socket.class);

    JSONObject jsonObj = new JSONObject();
    jsonObj.put("requestId", "42");
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    KuzzleOptions options = new KuzzleOptions();
    options.setMetadata(meta);

    kuzzle = new Kuzzle("http://localhost:7512", options);
    kuzzle.setSocket(socket);
    kuzzle.query("collection", "controller", "action", jsonObj);
    verify(socket).emit(eq("kuzzle"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

  @Test
  public void testGetAllStatsSuccess() throws Exception {
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
  public void testGetAllStatsError() throws Exception {
    kuzzle = new Kuzzle("http://localhost:7512");
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
      public void onSuccess(JSONObject object) throws Exception {
      }

      @Override
      public void onError(JSONObject error) throws Exception {
        assertEquals(error.get("error"), "rorre");
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
  public void testGetStatistic() throws URISyntaxException, JSONException, IOException, KuzzleException {
    kuzzle = new Kuzzle("http://localhost:7512");
    kuzzle.setSocket(mock(Socket.class));
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
