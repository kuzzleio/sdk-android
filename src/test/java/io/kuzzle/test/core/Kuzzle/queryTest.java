package io.kuzzle.test.core.Kuzzle;

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
import io.kuzzle.sdk.enums.Event;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.EventListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.States;
import io.kuzzle.sdk.util.QueueFilter;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class queryTest {
  private KuzzleExtend kuzzle;
  private Socket socket = mock(Socket.class);
  private Kuzzle.QueryArgs args;

  @Before
  public void setUp() throws URISyntaxException {
    Options options = new Options();
    options.setConnect(Mode.MANUAL);

    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setState(States.CONNECTED);
    kuzzle.setSocket(socket);

    args = new Kuzzle.QueryArgs();
    args.controller = "foo";
    args.action = "bar";
  }

  @Test
  public void checkAllSignaturesVariants() throws JSONException {
    JSONObject query = new JSONObject();
    OnQueryDoneListener queryListener = mock(OnQueryDoneListener.class);
    kuzzle = spy(kuzzle);
    kuzzle.query(args, query);
    kuzzle.query(args, query, new Options());
    kuzzle.query(args, query, queryListener);
    verify(kuzzle, times(3)).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIfDisconnected() throws JSONException {
    kuzzle.setState(States.DISCONNECTED);
    kuzzle.query(args, new JSONObject());
  }

  @Test
  public void shouldEmitTheRightRequest() throws JSONException {
    kuzzle.query(args, new JSONObject());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject request = (JSONObject) argument.getValue();
    assertEquals(request.getString("controller"), args.controller);
    assertEquals(request.getString("action"), args.action);
    assertEquals(request.getJSONObject("volatile").length(), 1);
    assertEquals(request.has("index"), false);
    assertEquals(request.has("collection"), false);
    assertEquals(request.has("headers"), false);
    assertNotNull(request.getString("requestId"));
  }

  @Test
  public void shouldAddIndexIfNeeded() throws JSONException {
    args.index = "index";
    kuzzle.query(args, new JSONObject());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject request = (JSONObject) argument.getValue();
    assertEquals(request.getString("controller"), args.controller);
    assertEquals(request.getString("action"), args.action);
    assertEquals(request.getString("index"), args.index);
    assertEquals(request.has("collection"), false);
    assertEquals(request.has("headers"), false);
    assertEquals(request.getJSONObject("volatile").length(), 1);
    assertNotNull(request.getString("requestId"));
  }

  @Test
  public void shouldAddCollectionIfNeeded() throws JSONException {
    args.collection = "collection";
    kuzzle.query(args, new JSONObject());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject request = (JSONObject) argument.getValue();
    assertEquals(request.getString("controller"), args.controller);
    assertEquals(request.getString("action"), args.action);
    assertEquals(request.getString("collection"), args.collection);
    assertEquals(request.has("index"), false);
    assertEquals(request.has("headers"), false);
    assertEquals(request.getJSONObject("volatile").length(), 1);
    assertNotNull(request.getString("requestId"));
  }

  @Test
  public void shouldAddJwtTokenIfNeeded() throws JSONException {
    kuzzle.setJwtToken("token");
    kuzzle.query(args, new JSONObject());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject request = (JSONObject) argument.getValue();
    assertEquals(request.getString("controller"), args.controller);
    assertEquals(request.getString("action"), args.action);
    assertEquals(request.getJSONObject("volatile").length(), 1);
    assertEquals(request.has("index"), false);
    assertEquals(request.has("collection"), false);
    assertNotNull(request.getString("requestId"));
    assertEquals(request.getString("jwt"), "token");
  }

  @Test
  public void shouldNotAddJwtTokenIfCheckingToken() throws JSONException {
    kuzzle.setJwtToken("token");
    args.controller = "auth";
    args.action = "checkToken";
    kuzzle.query(args, new JSONObject());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject request = (JSONObject) argument.getValue();
    assertEquals(request.getString("controller"), args.controller);
    assertEquals(request.getString("action"), args.action);
    assertEquals(request.getJSONObject("volatile").length(), 1);
    assertEquals(request.has("index"), false);
    assertEquals(request.has("collection"), false);
    assertEquals(request.has("headers"), false);
    assertNotNull(request.getString("requestId"));
  }

  @Test
  public void shouldAddVolatile() throws JSONException {
    JSONObject
      kuzzleVolatile = new JSONObject().put("foo", "foo").put("bar", "bar"),
      optionsVolatile = new JSONObject().put("qux", "qux").put("foo", "bar");
    Options opts = new Options().setVolatile(optionsVolatile);

    kuzzle.setVolatile(kuzzleVolatile);
    kuzzle.query(args, new JSONObject(), opts);

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject _volatile = ((JSONObject) argument.getValue()).getJSONObject("volatile");
    assertEquals(_volatile.length(), 4);
    assertEquals(_volatile.getString("sdkVersion"), kuzzle.getSdkVersion());
    assertEquals(_volatile.getString("foo"), "bar"); // options take precedence over kuzzle volatile data
    assertEquals(_volatile.getString("bar"), "bar");
    assertEquals(_volatile.getString("qux"), "qux");
  }

  @Test
  public void shouldSendSdkVersionInVolatile() throws JSONException  {
    kuzzle.query(args, new JSONObject(), new Options());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject _volatile = ((JSONObject) argument.getValue()).getJSONObject("volatile");
    assertEquals(_volatile.length(), 1);
    assertEquals(_volatile.getString("sdkVersion"), kuzzle.getSdkVersion());
  }

  @Test
  public void shouldAddRefresh() throws JSONException {
    String optionsRefresh = "foo";
    Options opts = new Options().setRefresh(optionsRefresh);

    kuzzle.query(args, new JSONObject(), opts);

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    String refresh = ((JSONObject) argument.getValue()).getString("refresh");
    assertEquals(refresh, optionsRefresh);
  }

  @Test
  public void shouldAddHeaders() throws JSONException {
    JSONObject headers = new JSONObject().put("foo", "bar");

    kuzzle.setHeaders(headers);

    kuzzle.query(args, new JSONObject());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    assertEquals(((JSONObject) argument.getValue()).getString("foo"), "bar");
  }

  @Test
  public void shouldEmitRequestIfConnected() throws JSONException {
    Options opts = new Options().setQueuable(false);
    kuzzle.setState(States.CONNECTED);
    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));
    verify(kuzzleSpy).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    assertEquals(kuzzleSpy.getOfflineQueue().size(), 0);
  }

  @Test
  public void shouldQueueRequestsIfNotConnected() throws JSONException {
    Options opts = new Options().setQueuable(true);
    kuzzle.setState(States.OFFLINE);
    kuzzle.startQueuing();

    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));
    verify(kuzzleSpy, never()).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    assertEquals(kuzzleSpy.getOfflineQueue().size(), 1);
  }

  @Test
  public void shouldFilterRequestBeforeQueuingIt() throws JSONException {
    Options opts = new Options().setQueuable(true);
    QueueFilter filter = spy(new QueueFilter() {
      @Override
      public boolean filter(JSONObject object) {
        return false;
      }
    });

    kuzzle.setQueueFilter(filter);
    kuzzle.setState(States.OFFLINE);
    kuzzle.startQueuing();

    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));
    verify(kuzzleSpy, never()).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    verify(filter).filter(any(JSONObject.class));
    assertEquals(kuzzleSpy.getOfflineQueue().size(), 0);
  }

  @Test
  public void shouldTriggerTokenExpiredEvent() throws JSONException {
    EventListener fake = spy(new EventListener() {
      @Override
      public void trigger(Object... args) {

      }
    });
    kuzzle.addListener(Event.tokenExpired, fake);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        final JSONObject response = new JSONObject()
            .put("error", new JSONObject()
                .put("message", "Token expired"))
            .put("action", "notlogout");
        ((Emitter.Listener) invocation.getArguments()[1]).call(response);
        return null;
      }
    }).when(socket).once(any(String.class), any(Emitter.Listener.class));
    kuzzle.emitRequest(new JSONObject().put("requestId", "foo"), mock(OnQueryDoneListener.class));
    verify(fake).trigger(any(Object.class));
  }

  @Test(expected = RuntimeException.class)
  public void testEmitRequestException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((Emitter.Listener) invocation.getArguments()[1]).call(new JSONObject());
        return null;
      }
    }).when(socket).once(any(String.class), any(Emitter.Listener.class));
    OnQueryDoneListener listener = spy(new OnQueryDoneListener() {
      @Override
      public void onSuccess(JSONObject response) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    });
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    kuzzle.emitRequest(new JSONObject().put("requestId", "foo"), listener);
  }

  @Test
  public void shouldDiscardQueuableRequestWhenNotConnected() throws JSONException {
    Options opts = new Options().setQueuable(false);
    kuzzle.setState(States.OFFLINE);
    OnQueryDoneListener listener = mock(OnQueryDoneListener.class);

    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, listener);
    verify(kuzzleSpy, never()).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    verify(listener).onError(any(JSONObject.class));
  }

  @Test
  public void shouldDiscardNonQueuedRequestWhenNotConnected() throws JSONException {
    Options opts = new Options().setQueuable(false);
    kuzzle.setState(States.OFFLINE);
    kuzzle.stopQueuing();
    OnQueryDoneListener listener = mock(OnQueryDoneListener.class);

    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, listener);
    verify(kuzzleSpy, never()).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    verify(listener).onError(any(JSONObject.class));
  }
}
