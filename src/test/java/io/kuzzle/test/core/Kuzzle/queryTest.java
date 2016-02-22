package io.kuzzle.test.core.Kuzzle;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.helpers.KuzzleFilterHelper;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.state.KuzzleStates;
import io.kuzzle.sdk.util.KuzzleQueueFilter;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class queryTest {
  private KuzzleExtend kuzzle;
  private Socket socket = mock(Socket.class);
  private KuzzleResponseListener listener;
  private Kuzzle.QueryArgs args;

  @Before
  public void setUp() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setConnect(Mode.MANUAL);

    kuzzle = new KuzzleExtend("http://localhost:7512", options, null);
    kuzzle.setState(KuzzleStates.CONNECTED);
    kuzzle.setSocket(socket);

    listener = new KuzzleResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };

    args = new Kuzzle.QueryArgs();
    args.controller = "foo";
    args.action = "bar";
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIfDisconnected() throws JSONException {
    kuzzle.setState(KuzzleStates.DISCONNECTED);
    kuzzle.query(args, new JSONObject());
  }

  @Test
  public void shouldDoNothingIfOfflineAndNotQueuable() throws JSONException {
    KuzzleOptions opts = new KuzzleOptions().setQueuable(false);
    kuzzle.setState(KuzzleStates.OFFLINE);
    kuzzle = spy(kuzzle);
    doThrow(RuntimeException.class)
      .when(kuzzle)
      .emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    assertThat(kuzzle.query(args, new JSONObject(), opts), instanceOf(KuzzleExtend.class));
  }

  @Test
  public void shouldEmitTheRightRequest() throws JSONException {
    kuzzle.query(args, new JSONObject());

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject request = (JSONObject) argument.getValue();
    assertEquals(request.getString("controller"), args.controller);
    assertEquals(request.getString("action"), args.action);
    assertEquals(request.getJSONObject("metadata").length(), 0);
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
    assertEquals(request.getJSONObject("metadata").length(), 0);
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
    assertEquals(request.getJSONObject("metadata").length(), 0);
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
    assertEquals(request.getJSONObject("metadata").length(), 0);
    assertEquals(request.has("index"), false);
    assertEquals(request.has("collection"), false);
    assertNotNull(request.getString("requestId"));

    String token = request.getJSONObject("headers").getString("authorization");
    assertEquals(token, "Bearer token");
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
    assertEquals(request.getJSONObject("metadata").length(), 0);
    assertEquals(request.has("index"), false);
    assertEquals(request.has("collection"), false);
    assertEquals(request.has("headers"), false);
    assertNotNull(request.getString("requestId"));
  }

  @Test
  public void shouldAddMetadata() throws JSONException {
    JSONObject
      kuzzleMetadata = new JSONObject().put("foo", "foo").put("bar", "bar"),
      optionsMetadata = new JSONObject().put("qux", "qux").put("foo", "bar");
    KuzzleOptions opts = new KuzzleOptions().setMetadata(optionsMetadata);

    kuzzle.setMetadata(kuzzleMetadata);
    kuzzle.query(args, new JSONObject(), opts);

    ArgumentCaptor argument = ArgumentCaptor.forClass(JSONObject.class);
    verify(socket).emit(any(String.class), (JSONObject) argument.capture());

    JSONObject metadata = ((JSONObject) argument.getValue()).getJSONObject("metadata");
    assertEquals(metadata.length(), 3);
    assertEquals(metadata.getString("foo"), "bar"); // options take precedence over kuzzle metadata
    assertEquals(metadata.getString("bar"), "bar");
    assertEquals(metadata.getString("qux"), "qux");
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
    KuzzleOptions opts = new KuzzleOptions().setQueuable(false);
    kuzzle.setState(KuzzleStates.CONNECTED);
    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));
    verify(kuzzleSpy).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    assertEquals(kuzzleSpy.getOfflineQueue().size(), 0);
  }

  @Test
  public void shouldQueueRequestsIfNotConnected() throws JSONException {
    KuzzleOptions opts = new KuzzleOptions().setQueuable(true);
    kuzzle.setState(KuzzleStates.OFFLINE);
    kuzzle.startQueuing();

    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));
    verify(kuzzleSpy, never()).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    assertEquals(kuzzleSpy.getOfflineQueue().size(), 1);
  }

  @Test
  public void shouldFilterRequestBeforeQueuingIt() throws JSONException {
    KuzzleOptions opts = new KuzzleOptions().setQueuable(true);
    KuzzleQueueFilter filter = spy(new KuzzleQueueFilter() {
      @Override
      public boolean filter(JSONObject object) {
        return false;
      }
    });

    kuzzle.setQueueFilter(filter);
    kuzzle.setState(KuzzleStates.OFFLINE);
    kuzzle.startQueuing();

    KuzzleExtend kuzzleSpy = spy(kuzzle);
    kuzzleSpy.query(args, new JSONObject(), opts, mock(OnQueryDoneListener.class));
    verify(kuzzleSpy, never()).emitRequest(any(JSONObject.class), any(OnQueryDoneListener.class));
    verify(filter).filter(any(JSONObject.class));
    assertEquals(kuzzleSpy.getOfflineQueue().size(), 0);
  }
}
