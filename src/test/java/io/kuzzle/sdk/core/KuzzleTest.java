package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.listeners.IEventListener;
import io.socket.client.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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

  @Test(expected = NullPointerException.class)
  public void testIsValid() throws URISyntaxException {
    kuzzle = new Kuzzle("http://localhost:7512");
    kuzzle.logout();
    kuzzle.isValid();
  }

  @Test
  public void testAddListener() throws URISyntaxException {
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
  public void testDataCollectionFactory() throws URISyntaxException, IOException, JSONException {
    kuzzle = new Kuzzle("http://localhost:7512");
    assertEquals(kuzzle.dataCollectionFactory("test").fetch("test", null).getCollection(), "test");
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
  public void testRemoveListener() throws URISyntaxException {
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
  public void testMetadata() throws URISyntaxException, JSONException, IOException {
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
    verify(socket).emit(eq("controller"), eq(jsonObj));
    assertEquals(jsonObj.getJSONObject("metadata").getString("foo"), "bar");
  }

}
