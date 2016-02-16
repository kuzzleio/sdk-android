package io.kuzzle.sdk;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.KuzzleEvent;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.toolbox.KuzzleTestToolbox;
import io.kuzzle.sdk.util.Event;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KuzzleListenerTest {

  private Kuzzle kuzzle;
  private Socket s;
  private Event event;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);
    kuzzle = new Kuzzle("http://localhost:7512", options);
    s = mock(Socket.class);
    KuzzleTestToolbox.setSocket(kuzzle, s);
  }

  private void  mockAnswer(final String event) {
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

}
