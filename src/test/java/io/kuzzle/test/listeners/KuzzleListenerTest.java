package io.kuzzle.test.listeners;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.sdk.util.Event;
import io.kuzzle.sdk.util.KuzzleQueryObject;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KuzzleListenerTest {
  private Kuzzle kuzzle;
  private KuzzleExtend kuzzleExtend;
  private Socket s;
  private Event event;

  @Before
  public void setUp() throws URISyntaxException {
    Options options = new Options();
    options.setConnect(Mode.MANUAL);
    s = mock(Socket.class);
    KuzzleExtend extended = new KuzzleExtend("localhost", options, null);
    extended.setSocket(s);
    kuzzle = extended;
    kuzzleExtend = extended;
  }

  private void mockAnswer(final String event) {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        //Mock response
        //Call callback with response
        ((Emitter.Listener) invocation.getArguments()[1]).call(null, null);
        return s;
      }
    }).when(s).once(eq(event), any(Emitter.Listener.class));
  }

  @Test
  public void testCONNECTEDevent() throws URISyntaxException {
    mockAnswer(Socket.EVENT_CONNECT);
    event = mock(Event.class);
    kuzzle.addListener(KuzzleEvent.connected, event);
    kuzzle.connect();
    verify(event, times(1)).trigger();
  }

  @Test
  public void testERRORevent() throws URISyntaxException {
    mockAnswer(Socket.EVENT_CONNECT_ERROR);
    event = mock(Event.class);
    kuzzle.addListener(KuzzleEvent.error, event);
    kuzzle.connect();
    verify(event, times(1)).trigger(null, null);
  }

  @Test
  public void testDISCONNECTevent() throws URISyntaxException {
    mockAnswer(Socket.EVENT_DISCONNECT);
    event = mock(Event.class);
    kuzzle.addListener(KuzzleEvent.disconnected, event);
    kuzzle.connect();
    verify(event, times(1)).trigger();
  }

  @Test
  public void testRECONNECTevent() throws URISyntaxException {
    mockAnswer(Socket.EVENT_RECONNECT);
    event = mock(Event.class);
    kuzzle.addListener(KuzzleEvent.reconnected, event);
    kuzzle.connect();
    verify(event, times(1)).trigger();
  }

  @Test
  public void testOfflineQueuePush() throws JSONException {
    event = mock(Event.class);
    kuzzleExtend.addListener(KuzzleEvent.offlineQueuePush, event);

    Options opts = new Options().setQueuable(true);
    kuzzleExtend.setState(KuzzleStates.OFFLINE);
    kuzzleExtend.startQueuing();

    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.controller = "foo";
    args.action = "bar";
    kuzzleExtend.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(KuzzleQueryObject.class);
    verify(event).trigger(argument.capture());
    assertEquals(((KuzzleQueryObject) argument.getValue()).getQuery().getString("action"), "bar");
  }

  @Test
  public void testOfflineQueuePop() throws JSONException, URISyntaxException {
    event = mock(Event.class);
    kuzzleExtend.setAutoReplay(true);
    kuzzleExtend.addListener(KuzzleEvent.offlineQueuePop, event);
    mockAnswer(Socket.EVENT_RECONNECT);

    Options opts = new Options().setQueuable(true);
    kuzzleExtend.setState(KuzzleStates.OFFLINE);
    kuzzleExtend.startQueuing();

    Kuzzle.QueryArgs args = new Kuzzle.QueryArgs();
    args.controller = "foo";
    args.action = "bar";
    kuzzleExtend.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));

    kuzzleExtend.connect();
    ArgumentCaptor argument = ArgumentCaptor.forClass(KuzzleQueryObject.class);
    verify(event).trigger(argument.capture());
    assertEquals(((KuzzleQueryObject) argument.getValue()).getQuery().getString("action"), "bar");
  }
}
