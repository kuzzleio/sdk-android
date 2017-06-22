package io.kuzzle.test.core.Kuzzle;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.test.testUtils.KuzzleExtend;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class getMyCredentialsTest {
  private KuzzleExtend kuzzle;
  private ResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    Options options = new Options();
    options.setConnect(Mode.MANUAL);

    kuzzle = new KuzzleExtend("localhost", options, null);
    kuzzle.setSocket(mock(Socket.class));

    listener = new ResponseListener<Object>() {
      @Override
      public void onSuccess(Object object) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
  }

  @Test(expected = RuntimeException.class)
  public void testGetMyCredentialsException() throws JSONException {
    listener = spy(listener);
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    kuzzle.getMyCredentials("strategy", null, listener);
  }

  @Test(expected = RuntimeException.class)
  public void testGetMyCredentialsQueryException() throws JSONException {
    kuzzle = spy(kuzzle);
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    kuzzle.getMyCredentials("strategy", null, listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListenerNull() {
    kuzzle.getMyCredentials(null, null);
  }

  @Test
  public void testGetMyCredentials() throws JSONException {
    kuzzle = spy(kuzzle);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject()));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));

    kuzzle.getMyCredentials("strategy", mock(ResponseListener.class));
    kuzzle.getMyCredentials("strategy", mock(Options.class), mock(ResponseListener.class));

    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);

    verify(kuzzle, times(2)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "auth");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getMyCredentials");
  }
}
