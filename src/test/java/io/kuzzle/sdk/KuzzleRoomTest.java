package io.kuzzle.sdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleRoomTest {

  private KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
  private JSONObject mockNotif = new JSONObject();
  private JSONObject  mockResponse = new JSONObject();
  private Kuzzle k;
  private KuzzleRoom room;

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
    room = new KuzzleRoom(new KuzzleDataCollection(k, "index", "test"));
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
  
  @Test(expected = RuntimeException.class)
  public void testCountQueryException() throws JSONException {
    doThrow(JSONException.class).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(OnQueryDoneListener.class));
    room.count(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testCountException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[4]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(OnQueryDoneListener.class));
    doThrow(JSONException.class).when(listener).onSuccess(any(Integer.class));
    room.count(listener);
  }

  @Test
  public void testCount() throws JSONException {
    JSONObject o = mock(JSONObject.class);
    when(o.put(any(String.class), any(Object.class))).thenReturn(new JSONObject());
    
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener)invocation.getArguments()[2]).onSuccess(new JSONObject().put("result", new JSONObject().put("count", 42)));
        ((OnQueryDoneListener)invocation.getArguments()[2]).onError(new JSONObject());
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(OnQueryDoneListener.class));
    room.count(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(k, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "count");
  }

  @Test(expected = RuntimeException.class)
  public void testRenewException() throws JSONException {
    doThrow(JSONException.class).when(listener).onError(any(JSONObject.class));
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    renew.callAfterRenew(listener, new JSONObject().put("error", mock(JSONObject.class)));
  }

  @Test
  public void testRenewWithError() throws URISyntaxException, JSONException {
    // Mocking getSocket()
    when(k.getSocket()).thenReturn(IO.socket("http://localhost:7515"));
    
    // stub to call the callback from KuzzleRoom.renew method
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("error", "foo");
        //Call callback with error response
        ((KuzzleResponseListener) invocation.getArguments()[4]).onError(response);
        return null;
      }
    }).when(k).query(eq(QueryArgsHelper.makeQueryArgs("subscribe", "on")), any(JSONObject.class), any(OnQueryDoneListener.class));
    room.renew(mock(JSONObject.class), mock(KuzzleResponseListener.class));
  }

  private void stubRoom(Kuzzle k) throws JSONException {
    // stub to call the query()'s callback from KuzzleRoom.renew method
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("result", new JSONObject().put("channel", "channel").put("roomId", "42"));
        //Call callback with response
        ((KuzzleResponseListener) invocation.getArguments()[4]).onSuccess(result);
        return null;
      }
    }).when(k).query(eq(QueryArgsHelper.makeQueryArgs("subscribe", "on")), any(JSONObject.class), any(OnQueryDoneListener.class));
  }

  @Test
  public void testRenew() throws JSONException {
    Socket s = mock(Socket.class);
    when(k.getSocket()).thenReturn(s);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(mockNotif);
        return null;
      }
    }).when(s).on(any(String.class), any(Emitter.Listener.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("result", new JSONObject().put("channel", "channel").put("roomId", "42"));
        //Call callback with response
        ((OnQueryDoneListener) invocation.getArguments()[5]).onSuccess(result);
        ((OnQueryDoneListener) invocation.getArguments()[5]).onError(new JSONObject());
        return null;
      }
    }).when(k).query(eq(QueryArgsHelper.makeQueryArgs("subscribe", "on")), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    
    stubRoom(k);
    room.renew(new JSONObject(), listener);
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(k, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "subscribe");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "on");
  }

  @Test
  public void testFilters() throws JSONException {
    Socket s = mock(Socket.class);
    when(k.getSocket()).thenReturn(s);
    JSONObject filters = new JSONObject();
    filters.put("foo", "bar");
    
    room.renew(filters, null);
    assertEquals(room.getFilters().getString("foo"), "bar");
    JSONObject filters2 = new JSONObject();
    filters2.put("foo", "rab");
    room.setFilters(filters2);
    assertEquals(room.getFilters().getString("foo"), "rab");
  }

  @Test
  public void setMetadataThroughConstructor() throws JSONException {
    Socket s = mock(Socket.class);
    when(k.getSocket()).thenReturn(s);
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

  @Test
  public void setSubscribeToSelfThroughConstructor() throws JSONException {
    Socket s = mock(Socket.class);
    when(k.getSocket()).thenReturn(s);
    JSONObject meta = new JSONObject();
    meta.put("foo", "bar");
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setSubscribeToSelf(false);
    KuzzleRoom room = new KuzzleRoom(new KuzzleDataCollection(k, "index", "test"), options);
    assertEquals(room.isSubscribeToSelf(), false);
    room.setSubscribeToSelf(true);
    assertEquals(room.isSubscribeToSelf(), true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCallAfterRenewWithNoResponse() {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    // Should throw an exception
    renew.callAfterRenew(null, null);
  }

  @Test
  public void testCallAfterRenewWithError() throws JSONException {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    JSONObject errorResponse = new JSONObject();
    errorResponse.put("error", "error");
    KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
    renew.callAfterRenew(listener, errorResponse);
    verify(listener, atLeastOnce()).onError(any(JSONObject.class));
  }

  @Test
  public void testCallAfterRenew() throws JSONException {
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"));
    JSONObject mockResponse = new JSONObject().put("result", new JSONObject());
    mockResponse.put("requestId", "42");
    renew.callAfterRenew(mock(KuzzleResponseListener.class), mockNotif);
  }

  @Test
  public void testCallAfterRenewWithSubscribeToSelf() throws JSONException {
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setSubscribeToSelf(true);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "index", "test"), options);
    Map<String, Date> m = new HashMap();
    m.put("42", new Date());
    when(k.getRequestHistory()).thenReturn(m);
    KuzzleResponseListener listener = mock(KuzzleResponseListener.class);
    renew.callAfterRenew(listener, mockNotif);
    verify(listener, atLeastOnce()).onSuccess(any(JSONObject.class));
  }

  @Test
  public void testUnsubscribe() throws JSONException {
    Socket s = mock(Socket.class);
    // Mocking getSocket()
    when(k.getSocket()).thenReturn(s);
    
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Call callback with response
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mockResponse);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject());
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[2]).onSuccess(mockResponse);
        ((OnQueryDoneListener) invocation.getArguments()[2]).onError(new JSONObject());
        return null;
      }
    }).when(k).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(OnQueryDoneListener.class));

    room.renew(mockNotif, listener);
    assertEquals(room.getRoomId(), "42");
    room.unsubscribe(listener);
  }

  @Test
  public void testSetHeaders() throws JSONException {
    
    JSONObject headers = new JSONObject();
    headers.put("foo", "bar");
    room.setHeaders(headers, true);
    assertEquals(room.getHeaders().getString("foo"), "bar");
    headers.put("oof", "baz");
    room.setHeaders(headers);
    assertEquals(room.getHeaders().getString("foo"), "bar");
    assertEquals(room.getHeaders().getString("oof"), "baz");
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

  // Class for testing protected callAfterRenew and triggerEvents
  public class KuzzleRoomExtend extends KuzzleRoom {

    public KuzzleRoomExtend(KuzzleDataCollection kuzzleDataCollection) {
      super(kuzzleDataCollection);
    }

    public KuzzleRoomExtend(KuzzleDataCollection kuzzleDataCollection, KuzzleRoomOptions options) {
      super(kuzzleDataCollection, options);
    }

    public void callAfterRenew(KuzzleResponseListener cb, Object args) {
      super.callAfterRenew(cb, args);
    }

  }


}
