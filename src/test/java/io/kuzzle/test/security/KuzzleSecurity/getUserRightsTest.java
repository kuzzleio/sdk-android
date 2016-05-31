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

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class getUserRightsTest {

  private Kuzzle kuzzle;
  private KuzzleSecurity kuzzleSecurity;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() {
    kuzzle = mock(Kuzzle.class);
    kuzzleSecurity = new KuzzleSecurity(kuzzle);
    listener = mock(KuzzleResponseListener.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalUserId() {
    kuzzleSecurity.getUserRights(null, mock(KuzzleResponseListener.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalCallback() {
    kuzzleSecurity.getUserRights("id", null);
  }

  @Test(expected = RuntimeException.class)
  public void testQueryException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject());
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.getUserRights("id", listener);
  }

  @Test(expected = RuntimeException.class)
  public void testException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.getUserRights("id", listener);
  }

  @Test
  public void testGetUserRights() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject()
            .put("result", new JSONObject()
              .put("hits", new JSONObject()));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzleSecurity.getUserRights("id", mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(io.kuzzle.sdk.core.Kuzzle.QueryArgs.class);
    verify(kuzzle).query((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((io.kuzzle.sdk.core.Kuzzle.QueryArgs) argument.getValue()).action, "getUserRights");
  }

}
