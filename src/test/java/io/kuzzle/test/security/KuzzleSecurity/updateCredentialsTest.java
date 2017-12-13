package io.kuzzle.test.security.KuzzleSecurity;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.security.Security;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class updateCredentialsTest {
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
    };
  }

  @Test(expected = RuntimeException.class)
  public void testUpdateCredentialsException() throws JSONException {
    listener = spy(listener);
    doThrow(JSONException.class).when(listener).onSuccess(any(JSONObject.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(mock(JSONObject.class));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.updateCredentials("strategy", "kuid", credentials, null, listener);
  }

  @Test(expected = RuntimeException.class)
  public void testUpdateCredentialsQueryException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.updateCredentials("strategy", "kuid", credentials, null, listener);
  }

  @Test
  public void testUpdateCredentials() throws JSONException {
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

    kuzzleSecurity.updateCredentials("strategy", "kuid", credentials)
      .updateCredentials("strategy", "kuid", credentials, mock(Options.class))
      .updateCredentials("strategy", "kuid", credentials, mock(ResponseListener.class))
      .updateCredentials("strategy", "kuid", credentials, mock(Options.class), mock(ResponseListener.class));

    verify(kuzzle, times(4)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "updateCredentials");
  }
}
