package io.kuzzle.test.core.Kuzzle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;

import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.listeners.KuzzleResponseListener;
import io.kuzzle.sdk.listeners.OnQueryDoneListener;
import io.kuzzle.test.testUtils.KuzzleExtend;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class getMyRightsTest {

  private Kuzzle kuzzle;
  private KuzzleResponseListener listener;

  @Before
  public void setUp() throws URISyntaxException {
    kuzzle = spy(new KuzzleExtend("http://localhost:7512", mock(KuzzleOptions.class), mock(KuzzleResponseListener.class)));
    listener = mock(KuzzleResponseListener.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalCallback() {
    kuzzle.getMyRights(null);
  }

  @Test(expected = RuntimeException.class)
  public void testQueryException() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(new JSONObject());
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getMyRights(listener);
  }

  @Test(expected = RuntimeException.class)
  public void testException() throws JSONException {
    doThrow(JSONException.class).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getMyRights(listener);
  }

  @Test
  public void testGetMyRights() throws JSONException {
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        JSONObject response = new JSONObject()
            .put("result", new JSONObject()
              .put("hits", new JSONArray()));
        ((OnQueryDoneListener) invocation.getArguments()[3]).onSuccess(response);
        ((OnQueryDoneListener) invocation.getArguments()[3]).onError(new JSONObject().put("error", "stub"));
        return null;
      }
    }).when(kuzzle).query(any(Kuzzle.QueryArgs.class), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    kuzzle.getMyRights(mock(KuzzleResponseListener.class));
    ArgumentCaptor argument = ArgumentCaptor.forClass(Kuzzle.QueryArgs.class);
    verify(kuzzle).query((Kuzzle.QueryArgs) argument.capture(), any(JSONObject.class), any(KuzzleOptions.class), any(OnQueryDoneListener.class));
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).controller, "security");
    assertEquals(((Kuzzle.QueryArgs) argument.getValue()).action, "getMyRights");
  }

}
