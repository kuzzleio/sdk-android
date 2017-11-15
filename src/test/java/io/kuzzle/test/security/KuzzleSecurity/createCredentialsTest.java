package io.kuzzle.test.security.KuzzleSecurity;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.security.Security;
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

public class createCredentialsTest {
  private Kuzzle kuzzle;
  private Security kuzzleSecurity;
  private ResponseListener listener;
  private JSONObject credentials = mock(JSONObject.class);

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    kuzzleSecurity = new Security(kuzzle);
    listener = new ResponseListener() {
      @Override
      public void onSuccess(Object response) {

      }

      @Override
      public void onError(JSONObject error) {

      }
    };
  }

  @Test(expected = RuntimeException.class)
  public void testCreateCredentialsException() throws JSONException {
    listener = spy(listener);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.createCredentials("strategy", "kuid", credentials, null, listener);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateCredentialsQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.createCredentials("strategy", "kuid", credentials, null, listener);
  }

  @Test
  public void testCreateCredentials() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        if (invocation.getArguments()[3] != null) {
          ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject().put("result", new JSONObject()));
          ((OnQueryDoneListener) invocation.getArguments()[3]).onError(mock(JSONObject.class));
        }
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));

    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);

    kuzzleSecurity.createCredentials("strategy", "kuid", credentials)
      .createCredentials("strategy", "kuid", credentials, mock(Options.class))
      .createCredentials("strategy", "kuid", credentials, mock(ResponseListener.class))
      .createCredentials("strategy", "kuid", credentials, mock(Options.class), mock(ResponseListener.class));

    verify(kuzzle, times(4)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).getController(), "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).getAction(), "createCredentials");
  }
}
