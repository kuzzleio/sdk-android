package io.kuzzle.sdk.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.enums.EventType;
import io.kuzzle.sdk.exceptions.KuzzleException;
import io.kuzzle.sdk.util.Event;
import io.socket.client.IO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KuzzleRoomTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor() throws IOException, JSONException, KuzzleException {
    Kuzzle k = mock(Kuzzle.class);
    // Should throw an exception
    new KuzzleRoom(null);
  }

  @Test
  public void testCount() throws IOException, JSONException, KuzzleException {
    Kuzzle k = mock(Kuzzle.class);
    KuzzleRoom room = new KuzzleRoom(new KuzzleDataCollection(k, "test"));

    room.count(null);
    verify(k, times(1)).query(eq("test"), eq("subscribe"), eq("count"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testRenewWithError() throws KuzzleException, IOException, JSONException, URISyntaxException {
    Kuzzle k = mock(Kuzzle.class);
    // Mocking getSocket()
    when(k.getSocket()).thenReturn(IO.socket("http://localhost:7515"));
    KuzzleRoom room = new KuzzleRoom(new KuzzleDataCollection(k, "test"));
    // stub to call the callback from KuzzleRoom.renew method
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();
        response.put("error", "foo");
        //Call callback with error response
        ((ResponseListener) invocation.getArguments()[4]).onError(response);
        return null;
      }
    }).when(k).query(eq("test"), eq("subscribe"), eq("on"), any(JSONObject.class), any(ResponseListener.class));
    room.renew(new JSONObject(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {

      }

      @Override
      public void onError(JSONObject arg) throws Exception {

      }
    });
  }

  private void stubRoom(Kuzzle k) throws IOException, JSONException, KuzzleException {
    // stub to call the query()'s callback from KuzzleRoom.renew method
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        JSONObject result = new JSONObject();
        result.put("roomId", "42");
        result.put("roomName", "room");
        //Call callback with response
        ((ResponseListener) invocation.getArguments()[4]).onSuccess(result);
        return null;
      }
    }).when(k).query(eq("test"), eq("subscribe"), eq("on"), any(JSONObject.class), any(ResponseListener.class));
  }

  @Test
  public void testRenew() throws IOException, JSONException, KuzzleException, URISyntaxException {
    Kuzzle k = mock(Kuzzle.class);
    // Mocking getSocket()
    when(k.getSocket()).thenReturn(IO.socket("http://localhost:7515"));
    KuzzleRoom room = new KuzzleRoom(new KuzzleDataCollection(k, "test"));

    stubRoom(k);

    room.renew(new JSONObject(), null);
  }

  @Test(expected = NullPointerException.class)
  public void testCallAfterRenewWithNoResponse() throws Exception {
    Kuzzle k = mock(Kuzzle.class);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test"));
    // Should throw an exception
    renew.callAfterRenew(null, null);
  }

  @Test(expected = KuzzleException.class)
  public void testCallAfterRenewWithError() throws Exception {
    Kuzzle k = mock(Kuzzle.class);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test"));
    JSONObject errorResponse = new JSONObject();
    errorResponse.put("error", "error");
    errorResponse.put("result", new JSONObject());
    // Should throw an exception
    renew.callAfterRenew(null, errorResponse);
  }

  @Test
  public void testCallAfterRenew() throws Exception {
    Kuzzle k = mock(Kuzzle.class);
    KuzzleRoomExtend renew = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test"));
    renew.setListeningToConnections(true);

    final JSONObject result = new JSONObject();
    JSONObject action = new JSONObject();
    action.put("action", "on");
    result.put("result", action);
    renew.callAfterRenew(null, result);
    action.put("action", "off");
    renew.callAfterRenew(null, result);
    action.put("action", "unknown");
    renew.callAfterRenew(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        assertEquals(args.get("action"), "unknown");
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    }, result);
  }

  @Test(expected = NullPointerException.class)
  public void testTriggerEventsNullResponse() throws Exception {
    Kuzzle k = mock(Kuzzle.class);
    KuzzleRoomExtend trigger = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test"));
    trigger.triggerEvents(true, EventType.SUBSCRIBED, null, null, null);
  }

  @Test(expected = KuzzleException.class)
  public void testTriggerEventsError() throws Exception {
    Kuzzle k = mock(Kuzzle.class);
    KuzzleRoomExtend trigger = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test"));

    JSONObject response = new JSONObject();
    response.put("error", "error");
    trigger.triggerEvents(true, EventType.SUBSCRIBED, response, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        assertEquals(((JSONObject) args).get("error").toString(), "error");
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    }, null);
  }

  @Test
  public void testTriggerSUBSCRIBEDEvents() throws Exception {
    Kuzzle k = mock(Kuzzle.class);

    // Mocking getEventListeners
    List<Event> listeners = new ArrayList<>();
    Event mockEvent = mock(Event.class);
    when(mockEvent.getType()).thenReturn(EventType.SUBSCRIBED);
    listeners.add(mockEvent);
    when(k.getEventListeners()).thenReturn(listeners);

    KuzzleRoomExtend trigger = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test"));

    JSONObject result = new JSONObject();
    JSONObject responseObject = new JSONObject();
    responseObject.put("result", result);
    trigger.triggerEvents(true, EventType.SUBSCRIBED, new JSONObject(), new ResponseListener() {
      @Override
      public void onSuccess(JSONObject args) throws Exception {
        // Test callback
        assertNotNull(((JSONObject) args).get("count").toString());
      }

      @Override
      public void onError(JSONObject error) throws Exception {

      }
    }, responseObject);
    verify(mockEvent, times(1)).trigger(any(String.class), any(JSONObject.class));
  }

  @Test
  public void testTriggerUNSUBSCRIBEDEvents() throws Exception {
    Kuzzle k = mock(Kuzzle.class);

    // Mocking getEventListeners
    List<Event> listeners = new ArrayList<>();
    Event mockEvent = mock(Event.class);
    when(mockEvent.getType()).thenReturn(EventType.UNSUBSCRIBED);
    listeners.add(mockEvent);
    when(k.getEventListeners()).thenReturn(listeners);

    KuzzleRoomExtend trigger = new KuzzleRoomExtend(new KuzzleDataCollection(k, "test"));

    JSONObject result = new JSONObject();
    JSONObject responseObject = new JSONObject();
    responseObject.put("result", result);
    trigger.triggerEvents(true, EventType.UNSUBSCRIBED, responseObject, null, responseObject);
    verify(mockEvent, times(1)).trigger(any(String.class), any(JSONObject.class));
  }

  @Test
  public void testUnsubscribe() throws JSONException, IOException, KuzzleException, URISyntaxException {
    Kuzzle k = mock(Kuzzle.class);
    // Mocking getSocket()
    when(k.getSocket()).thenReturn(IO.socket("http://localhost:7515"));
    KuzzleRoom room = new KuzzleRoom(new KuzzleDataCollection(k, "test"));

    stubRoom(k);

    room.renew(new JSONObject(), null);

    room.unsubscribe();
  }

  // Class for testing protected callAfterRenew and triggerEvents
  public class KuzzleRoomExtend extends KuzzleRoom {

    public KuzzleRoomExtend(KuzzleDataCollection kuzzleDataCollection) throws KuzzleException {
      super(kuzzleDataCollection);
    }

    public void callAfterRenew(ResponseListener cb, Object... args) throws Exception {
      super.callAfterRenew(cb, args);
    }

    public void triggerEvents(boolean listening, EventType globalEvent, JSONObject args2, ResponseListener cb, Object... args) throws Exception {
      super.triggerEvents(listening, globalEvent, args2, cb, args);
    }
  }


}
