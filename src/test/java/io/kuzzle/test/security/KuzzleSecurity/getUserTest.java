package io.kuzzle.test.security.KuzzleSecurity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.Options;
import io.kuzzle.sdk.listeners.ResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.security.KuzzleSecurity;
import io.kuzzle.sdk.security.KuzzleUser;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class getUserTest {
  private Kuzzle kuzzle;
  private KuzzleSecurity kuzzleSecurity;
  private ResponseListener listener;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    kuzzleSecurity = new KuzzleSecurity(kuzzle);
    listener = mock(ResponseListener.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetUserNoID() throws JSONException {
    kuzzleSecurity.fetchUser(null, new Options(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetUserNoListener() throws JSONException {
    kuzzleSecurity.fetchUser("foo", null);
  }

  @Test
  public void testgetUserValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"," +
            "\"_source\": {}" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.fetchUser("foobar", new ResponseListener<KuzzleUser>() {
      @Override
      public void onSuccess(KuzzleUser response) {
        assertEquals(response.id, "foobar");
      }

      @Override
      public void onError(JSONObject error) {
        try {
          assertEquals(error.getString("error"), "stub");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }
    });

    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "fetchUser");
  }

  @Test(expected = RuntimeException.class)
  public void testgetUserBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(Options.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.fetchUser("foobar", listener);
  }
}
