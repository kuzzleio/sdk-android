package io.kuzzle.test.core.KuzzleRoom;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.test.testUtils.KuzzleRoomExtend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class constructorTest {
  private KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
  private JSONObject mockNotif = new JSONObject();
  private JSONObject  mockResponse = new JSONObject();
  private Kuzzle k;
  private KuzzleRoomExtend room;

  @Before
  public void setUp() throws JSONException {
    mockNotif.put("type", "type")
      .put("index", "index")
      .put("status", 200)
      .put("collection", "collection")
      .put("controller", "controller")
      .put("action", "action")
      .put("state", "ALL")
      .put("scope", "ALL")
      .put("metadata", new JSONObject())
      .put("result", new JSONObject())
      .put("requestId", "42");
    mockResponse.put("result", new JSONObject().put("channel", "channel").put("roomId", "42"));
    k = mock(Kuzzle.class);
    when(k.getHeaders()).thenReturn(new JSONObject());
    room = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
  }

  @Test
  public void setSubscribeToSelfThroughConstructor() throws JSONException {
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setSubscribeToSelf(false);
    KuzzleRoom room = new KuzzleRoom(new KuzzleDataCollection(k, "index", "test"), options);
    assertEquals(room.isSubscribeToSelf(), false);
    room.setSubscribeToSelf(true);
    assertEquals(room.isSubscribeToSelf(), true);
  }

  @Test(expected = RuntimeException.class)
  public void testConstructorException() {
    KuzzleDataCollection fake = spy(new KuzzleDataCollection(k, "index", "test"));
    doThrow(JSONException.class).when(fake).getHeaders();
    room = new KuzzleRoomExtend(fake);
  }

  @Test
  public void testSetHeaders() throws JSONException {
    room.makeHeadersNull();
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    room.setHeaders(headers, true);
    assertEquals(room.getHeaders().getString("foo"), "bar");
    headers.put("oof", "baz");
    room.setHeaders(headers);
    assertEquals(room.getHeaders().getString("foo"), "bar");
    assertEquals(room.getHeaders().getString("oof"), "baz");
  }

  @Test(expected = RuntimeException.class)
  public void testSetHeadersException() {
    JSONObject json = spy(new JSONObject());
    doThrow(JSONException.class).when(json).keys();
    room.setHeaders(json, false);
  }

  @Test
  public void testGetHeaders() throws JSONException {
    room.setHeaders(null);
    assertNotNull(room.getHeaders());
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    room.setHeaders(headers);
    assertEquals(room.getHeaders().getString("foo"), "bar");
  }

  @Test
  public void testFilters() throws JSONException {
    JSONObject filters = new JSONObject();
    filters.put("foo", "bar");

    room.renew(filters, listener, null);
    assertEquals(room.getFilters().getString("foo"), "bar");
    JSONObject filters2 = new JSONObject();
    filters2.put("foo", "rab");
    room.setFilters(filters2);
    assertEquals(room.getFilters().getString("foo"), "rab");
  }

  @Test
  public void setMetadataThroughConstructor() throws JSONException {
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setMetadata(meta);
    KuzzleRoom room = new KuzzleRoom(new KuzzleDataCollection(k, "index", "test"), options);
    assertEquals(room.getMetadata().get("foo"), "bar");
    JSONObject meta2 = new JSONObject();
    meta2.put("oof", "rab");
    room.setMetadata(meta2);
    assertEquals(room.getMetadata().get("oof"), "rab");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullCollection() {
    // Should throw an exception
    new KuzzleRoom(null);
  }

  @Test
  public void testCollection() {
    KuzzleDataCollection collection = new KuzzleDataCollection(k, "index", "test");
    KuzzleRoom room = new KuzzleRoom(collection);
    assertEquals(room.getCollection(), collection.getCollection());
  }
}
