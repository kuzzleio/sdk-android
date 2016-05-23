package io.kuzzle.test.security.KuzzleSecurity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.sdk.security.KuzzleSecurity;
import io.kuzzle.sdk.security.KuzzleUser;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class updateUserTest {
  private Kuzzle kuzzle;
  private KuzzleSecurity kuzzleSecurity;
  private KuzzleResponseListener listener;
  private JSONObject  content;

  @Before
  public void setUp() throws JSONException {
    kuzzle = mock(Kuzzle.class);
    kuzzleSecurity = new KuzzleSecurity(kuzzle);
    listener = mock(KuzzleResponseListener.class);
    content = new JSONObject()
        .put("foo", "bar");
  }

  @Test
  public void testUpdateUserNoListener() throws JSONException {
    kuzzleSecurity.updateUser("foo", content, new KuzzleOptions());
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "updateUser");
  }

  @Test
  public void testUpdateUserValidResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject(
          "{" +
            "\"result\": {" +
            "\"_id\": \"foobar\"" +
            "}" +
            "}");

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.updateUser("foobar", content, new KuzzleResponseListener<KuzzleUser>() {
      @Override
      public void onSuccess(KuzzleUser response) {
        assertEquals(response.getId(), "foobar");
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

    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle, times(1)).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "updateUser");
  }

  @Test(expected = RuntimeException.class)
  public void testUpdateUserBadResponse() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject();

        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));

    kuzzleSecurity.updateUser("foobar", content, new KuzzleOptions(), listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUpdateUserNoID() throws JSONException {
    kuzzleSecurity.updateUser(null, content);
  }
}
